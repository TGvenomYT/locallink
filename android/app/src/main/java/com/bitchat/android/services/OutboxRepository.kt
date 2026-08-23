package com.bitchat.android.services

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Durable backing store for [MessageRouter]'s pre-handshake outbox.
 *
 * The outbox previously lived only in a [java.util.concurrent.ConcurrentHashMap], so a queued
 * private message (e.g. one waiting for a Noise handshake with a peer who isn't in range yet)
 * was silently lost forever if the process died before the handshake completed - a routine event
 * on Android, not an edge case. This mirrors every write/removal into an encrypted SQLite table so
 * [MessageRouter] can restore its queue on the next launch and let the existing outbox scheduler
 * keep retrying.
 */
internal class OutboxRepository private constructor(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Executors
        .newSingleThreadExecutor { runnable -> Thread(runnable, "outbox-store").apply { isDaemon = true } }
        .asCoroutineDispatcher(),
    private val storageCipher: ConversationStorageCipher = AndroidConversationStorageCipher(
        keyAlias = "bitchat_outbox_storage_v1"
    )
) {
    companion object {
        private const val TAG = "OutboxRepository"

        @Volatile
        private var instance: OutboxRepository? = null

        fun getInstance(
            context: Context,
            storageCipher: ConversationStorageCipher = AndroidConversationStorageCipher(
                keyAlias = "bitchat_outbox_storage_v1"
            )
        ): OutboxRepository =
            instance ?: synchronized(this) {
                instance ?: OutboxRepository(
                    context.applicationContext,
                    storageCipher = storageCipher
                ).also { instance = it }
            }

        internal fun resetForTesting() {
            instance = null
        }
    }

    data class PersistedEntry(
        val conversationID: String,
        val messageID: String,
        val nickname: String,
        val content: String,
        val enqueuedAtMs: Long
    )

    private val database = OutboxDatabase(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    /**
     * One-time, synchronous load used while [MessageRouter] is constructing, so restored
     * messages are back in the in-memory queue before anything else can run.
     */
    fun loadAllBlocking(): List<PersistedEntry> = try {
        database.loadAll(storageCipher)
    } catch (error: Exception) {
        Log.e(TAG, "Unable to restore persisted outbox: ${error.message}")
        emptyList()
    }

    /** Suspends until all persistence work queued before this call has finished. */
    suspend fun awaitPendingWrites() {
        withContext(dispatcher) { Unit }
    }

    fun insert(entry: PersistedEntry) {
        scope.launch {
            try {
                database.insert(entry, storageCipher)
            } catch (error: Exception) {
                Log.e(TAG, "Unable to persist outbox message: ${error.message}")
            }
        }
    }

    fun delete(messageID: String) {
        scope.launch {
            try {
                database.delete(messageID)
            } catch (error: Exception) {
                Log.e(TAG, "Unable to delete persisted outbox message: ${error.message}")
            }
        }
    }

    /** Used by panic-mode wipe; also destroys the dedicated Keystore key for cryptographic erasure. */
    fun clearAll() {
        scope.launch {
            try {
                storageCipher.destroyKey()
                database.clearAll()
            } catch (error: Exception) {
                Log.e(TAG, "Unable to clear persisted outbox: ${error.message}")
            }
        }
    }
}

private class OutboxDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "outbox.db"
        private const val DATABASE_VERSION = 1
        private val COLUMNS = arrayOf(
            "message_id", "conversation_id", "nickname", "content_ciphertext", "enqueued_at"
        )
    }

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE outbox_messages (
                message_id TEXT PRIMARY KEY NOT NULL,
                conversation_id TEXT NOT NULL,
                nickname TEXT NOT NULL,
                content_ciphertext BLOB NOT NULL,
                enqueued_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_outbox_conversation ON outbox_messages(conversation_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No prior schema versions exist yet.
    }

    fun insert(entry: OutboxRepository.PersistedEntry, cipher: ConversationStorageCipher) {
        writableDatabase.insertWithOnConflict(
            "outbox_messages",
            null,
            ContentValues().apply {
                put("message_id", entry.messageID)
                put("conversation_id", entry.conversationID)
                put("nickname", entry.nickname)
                put(
                    "content_ciphertext",
                    cipher.encrypt(
                        entry.content.toByteArray(Charsets.UTF_8),
                        outboxContentAad(entry.messageID)
                    )
                )
                put("enqueued_at", entry.enqueuedAtMs)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun delete(messageID: String) {
        writableDatabase.delete("outbox_messages", "message_id = ?", arrayOf(messageID))
    }

    fun clearAll() {
        writableDatabase.delete("outbox_messages", null, null)
    }

    fun loadAll(cipher: ConversationStorageCipher): List<OutboxRepository.PersistedEntry> {
        val results = mutableListOf<OutboxRepository.PersistedEntry>()
        readableDatabase.query(
            "outbox_messages", COLUMNS, null, null, null, null, "enqueued_at ASC"
        ).use { cursor ->
            val messageIdIdx = cursor.getColumnIndexOrThrow("message_id")
            val conversationIdIdx = cursor.getColumnIndexOrThrow("conversation_id")
            val nicknameIdx = cursor.getColumnIndexOrThrow("nickname")
            val contentIdx = cursor.getColumnIndexOrThrow("content_ciphertext")
            val enqueuedAtIdx = cursor.getColumnIndexOrThrow("enqueued_at")
            while (cursor.moveToNext()) {
                val messageID = cursor.getString(messageIdIdx)
                val content = try {
                    cipher.decrypt(cursor.getBlob(contentIdx), outboxContentAad(messageID))
                        .toString(Charsets.UTF_8)
                } catch (error: Exception) {
                    Log.e("OutboxRepository", "Dropping unreadable persisted outbox row: ${error.message}")
                    null
                } ?: continue
                results.add(
                    OutboxRepository.PersistedEntry(
                        conversationID = cursor.getString(conversationIdIdx),
                        messageID = messageID,
                        nickname = cursor.getString(nicknameIdx),
                        content = content,
                        enqueuedAtMs = cursor.getLong(enqueuedAtIdx)
                    )
                )
            }
        }
        return results
    }
}

private fun outboxContentAad(messageID: String): ByteArray =
    "outbox:$messageID:content".toByteArray(Charsets.UTF_8)

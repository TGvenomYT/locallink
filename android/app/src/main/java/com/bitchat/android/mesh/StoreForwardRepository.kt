package com.bitchat.android.mesh

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.bitchat.android.protocol.BitchatPacket
import com.bitchat.android.services.AndroidConversationStorageCipher
import com.bitchat.android.services.ConversationStorageCipher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Durable backing store for [StoreForwardManager]'s offline-peer message cache.
 *
 * Private text messages are handled upstream by MessageRouter's outbox (see
 * OutboxRepository), but StoreForwardManager still caches packets that don't go through
 * that path - fragments of large messages, file/voice-note transfers, and sync requests
 * addressed to a specific peer who is temporarily out of range. Those were previously held
 * only in a ConcurrentHashMap, so a voice note or file cached for an offline peer was
 * silently lost forever if the process died before that peer reconnected - the same class
 * of bug fixed for private text messages, just for a narrower set of packet types.
 *
 * Both BluetoothMeshService and MeshCore (WiFi Aware) construct their own
 * StoreForwardManager, so this repository is a shared singleton: a message cached while
 * only one transport had a peer in range becomes visible to the other transport too, at
 * least across a restart, instead of being invisible to whichever instance didn't cache it.
 */
internal class StoreForwardRepository private constructor(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Executors
        .newSingleThreadExecutor { runnable -> Thread(runnable, "store-forward-store").apply { isDaemon = true } }
        .asCoroutineDispatcher(),
    private val storageCipher: ConversationStorageCipher =
        AndroidConversationStorageCipher(keyAlias = "bitchat_store_forward_storage_v1")
) {
    companion object {
        private const val TAG = "StoreForwardRepository"

        @Volatile
        private var instance: StoreForwardRepository? = null

        fun getInstance(
            context: Context,
            storageCipher: ConversationStorageCipher =
                AndroidConversationStorageCipher(keyAlias = "bitchat_store_forward_storage_v1")
        ): StoreForwardRepository =
            instance ?: synchronized(this) {
                instance ?: StoreForwardRepository(
                    context.applicationContext,
                    storageCipher = storageCipher
                ).also { instance = it }
            }

        internal fun resetForTesting() {
            instance = null
        }
    }

    data class PersistedEntry(
        val messageID: String,
        val recipientPeerID: String,
        val packetBytes: ByteArray,
        val timestampMs: Long,
        val isForFavorite: Boolean
    )

    private val database = StoreForwardDatabase(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    /** One-time, synchronous load used while a StoreForwardManager is constructing. */
    fun loadAllBlocking(): List<PersistedEntry> = try {
        database.loadAll(storageCipher)
    } catch (error: Exception) {
        Log.e(TAG, "Unable to restore persisted store-forward cache: ${error.message}")
        emptyList()
    }

    fun insert(entry: PersistedEntry) {
        scope.launch {
            try {
                database.insert(entry, storageCipher)
            } catch (error: Exception) {
                Log.e(TAG, "Unable to persist store-forward message: ${error.message}")
            }
        }
    }

    fun delete(messageID: String) {
        scope.launch {
            try {
                database.delete(messageID)
            } catch (error: Exception) {
                Log.e(TAG, "Unable to delete persisted store-forward message: ${error.message}")
            }
        }
    }

    fun deleteAll(messageIDs: Collection<String>) {
        if (messageIDs.isEmpty()) return
        scope.launch {
            try {
                database.deleteAll(messageIDs)
            } catch (error: Exception) {
                Log.e(TAG, "Unable to delete persisted store-forward messages: ${error.message}")
            }
        }
    }

    fun clearAll() {
        scope.launch {
            try {
                storageCipher.destroyKey()
                database.clearAll()
            } catch (error: Exception) {
                Log.e(TAG, "Unable to clear persisted store-forward cache: ${error.message}")
            }
        }
    }

    /** Suspends until all persistence work queued before this call has finished. */
    suspend fun awaitPendingWrites() {
        withContext(dispatcher) { Unit }
    }
}

private class StoreForwardDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "store_forward.db"
        private const val DATABASE_VERSION = 1
        private val COLUMNS = arrayOf(
            "message_id", "recipient_peer_id", "packet_ciphertext", "timestamp_ms", "is_for_favorite"
        )
    }

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE cached_messages (
                message_id TEXT PRIMARY KEY NOT NULL,
                recipient_peer_id TEXT NOT NULL,
                packet_ciphertext BLOB NOT NULL,
                timestamp_ms INTEGER NOT NULL,
                is_for_favorite INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_cached_messages_recipient ON cached_messages(recipient_peer_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No prior schema versions exist yet.
    }

    fun insert(entry: StoreForwardRepository.PersistedEntry, cipher: ConversationStorageCipher) {
        writableDatabase.insertWithOnConflict(
            "cached_messages",
            null,
            ContentValues().apply {
                put("message_id", entry.messageID)
                put("recipient_peer_id", entry.recipientPeerID)
                put(
                    "packet_ciphertext",
                    cipher.encrypt(entry.packetBytes, cachedMessageAad(entry.messageID))
                )
                put("timestamp_ms", entry.timestampMs)
                put("is_for_favorite", if (entry.isForFavorite) 1 else 0)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun delete(messageID: String) {
        writableDatabase.delete("cached_messages", "message_id = ?", arrayOf(messageID))
    }

    fun deleteAll(messageIDs: Collection<String>) {
        messageIDs.chunked(400).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            writableDatabase.delete(
                "cached_messages",
                "message_id IN ($placeholders)",
                chunk.toTypedArray()
            )
        }
    }

    fun clearAll() {
        writableDatabase.delete("cached_messages", null, null)
    }

    fun loadAll(cipher: ConversationStorageCipher): List<StoreForwardRepository.PersistedEntry> {
        val results = mutableListOf<StoreForwardRepository.PersistedEntry>()
        readableDatabase.query(
            "cached_messages", COLUMNS, null, null, null, null, "timestamp_ms ASC"
        ).use { cursor ->
            val messageIdIdx = cursor.getColumnIndexOrThrow("message_id")
            val recipientIdx = cursor.getColumnIndexOrThrow("recipient_peer_id")
            val packetIdx = cursor.getColumnIndexOrThrow("packet_ciphertext")
            val timestampIdx = cursor.getColumnIndexOrThrow("timestamp_ms")
            val favoriteIdx = cursor.getColumnIndexOrThrow("is_for_favorite")
            while (cursor.moveToNext()) {
                val messageID = cursor.getString(messageIdIdx)
                val packetBytes = try {
                    cipher.decrypt(cursor.getBlob(packetIdx), cachedMessageAad(messageID))
                } catch (error: Exception) {
                    Log.e(
                        "StoreForwardRepository",
                        "Dropping unreadable persisted store-forward row: ${error.message}"
                    )
                    null
                } ?: continue
                if (BitchatPacket.fromBinaryData(packetBytes) == null) {
                    // Wire format changed underneath a persisted row; skip rather than crash.
                    continue
                }
                results.add(
                    StoreForwardRepository.PersistedEntry(
                        messageID = messageID,
                        recipientPeerID = cursor.getString(recipientIdx),
                        packetBytes = packetBytes,
                        timestampMs = cursor.getLong(timestampIdx),
                        isForFavorite = cursor.getInt(favoriteIdx) != 0
                    )
                )
            }
        }
        return results
    }
}

private fun cachedMessageAad(messageID: String): ByteArray =
    "store-forward:$messageID:packet".toByteArray(Charsets.UTF_8)

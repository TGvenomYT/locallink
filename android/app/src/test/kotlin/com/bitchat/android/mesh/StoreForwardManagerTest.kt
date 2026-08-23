package com.bitchat.android.mesh

import android.os.Build
import com.bitchat.android.protocol.BitchatPacket
import com.bitchat.android.protocol.MessageType
import com.bitchat.android.services.InMemoryConversationStorageCipher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class StoreForwardManagerTest {

    private val recipientPeerID = "peer1234" // 8 ASCII bytes, matches the 8-byte recipientID wire field
    private val senderPeerID = "sender01"

    // Reused across simulated "restarts": a real device's Keystore-backed key survives a
    // process restart, so the in-memory test double must model that, not use a fresh key.
    private lateinit var cipher: InMemoryConversationStorageCipher

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        StoreForwardRepository.resetForTesting()
        cipher = InMemoryConversationStorageCipher()
        StoreForwardRepository.getInstance(context, cipher)
    }

    @After
    fun tearDown() {
        StoreForwardRepository.resetForTesting()
    }

    private fun filePacket(messageID: String = "msg-1"): BitchatPacket = BitchatPacket(
        version = 1u,
        type = MessageType.FILE_TRANSFER.value,
        senderID = senderPeerID.toByteArray(Charsets.UTF_8),
        recipientID = recipientPeerID.toByteArray(Charsets.UTF_8),
        timestamp = System.currentTimeMillis().toULong(),
        payload = "voice-note-bytes".toByteArray(Charsets.UTF_8),
        signature = null,
        ttl = 5u
    )

    private fun newManager(context: android.content.Context): StoreForwardManager {
        val manager = StoreForwardManager(context)
        manager.delegate = object : StoreForwardManagerDelegate {
            override fun isFavorite(peerID: String) = false
            override fun isPeerOnline(peerID: String) = true
            override fun sendPacket(packet: BitchatPacket) {}
        }
        return manager
    }

    @Test
    fun `cached file transfer survives simulated process restart and is delivered on reconnect`() {
        val context = RuntimeEnvironment.getApplication()
        val manager = newManager(context)
        manager.cacheMessage(filePacket("msg-restart"), "msg-restart")
        runBlocking { StoreForwardRepository.getInstance(context).awaitPendingWrites() }

        // Simulate process death: drop the repository singleton but keep the same on-disk
        // files and the same (Keystore-equivalent) key, exactly like a real restart.
        StoreForwardRepository.resetForTesting()
        StoreForwardRepository.getInstance(context, cipher)

        val sent = CopyOnWriteArrayList<BitchatPacket>()
        val latch = CountDownLatch(1)
        val restored = StoreForwardManager(context)
        restored.delegate = object : StoreForwardManagerDelegate {
            override fun isFavorite(peerID: String) = false
            override fun isPeerOnline(peerID: String) = true
            override fun sendPacket(packet: BitchatPacket) {
                sent.add(packet)
                latch.countDown()
            }
        }

        restored.sendCachedMessages(recipientPeerID)
        assertTrue("cached message should be delivered after restart", latch.await(2, TimeUnit.SECONDS))
        assertEquals(1, sent.size)
        assertEquals("voice-note-bytes", String(sent[0].payload, Charsets.UTF_8))
    }

    @Test
    fun `panic wipe clears the persisted cache so a later restart does not resurrect it`() {
        val context = RuntimeEnvironment.getApplication()
        val manager = newManager(context)
        manager.cacheMessage(filePacket("msg-wipe"), "msg-wipe")
        runBlocking { StoreForwardRepository.getInstance(context).awaitPendingWrites() }

        manager.clearAllCache()
        runBlocking { StoreForwardRepository.getInstance(context).awaitPendingWrites() }

        StoreForwardRepository.resetForTesting()
        StoreForwardRepository.getInstance(context, cipher)

        val restored = StoreForwardRepository.getInstance(context).loadAllBlocking()
        assertTrue("wiped cache should not resurrect any persisted message", restored.isEmpty())
    }
}

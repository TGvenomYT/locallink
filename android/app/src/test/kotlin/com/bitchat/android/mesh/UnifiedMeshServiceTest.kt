package com.bitchat.android.mesh

import android.os.Build
import com.bitchat.android.ui.debug.DebugSettingsManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers UnifiedMeshService's transport-selection logic for private messages under BLE-only
 * conditions (no Wi-Fi Aware peer connected) - the realistic state for a fresh pilot rollout
 * where Wi-Fi Aware permission may not be granted on every device yet. The Wi-Fi Aware branch
 * itself goes through the WifiAwareController singleton and isn't covered here; that gap is
 * real and worth closing separately rather than papering over with an unverified test double.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class UnifiedMeshServiceTest {

    private val peerID = "aaaabbbbccccdddd"
    private lateinit var bluetooth: BluetoothMeshService
    private lateinit var service: UnifiedMeshService

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        bluetooth = mock()
        whenever(bluetooth.myPeerID).thenReturn("1111222233334444")
        service = UnifiedMeshService(context, bluetooth)
    }

    @After
    fun tearDown() {
        // isBleEnabled() reads this singleton first; other test classes assume the default.
        DebugSettingsManager.getInstance().setBleEnabled(true)
    }

    private fun peerInfo(isConnected: Boolean) = PeerInfo(
        id = peerID,
        nickname = "peer",
        isConnected = isConnected,
        isDirectConnection = true,
        noisePublicKey = ByteArray(32) { 0x0B },
        signingPublicKey = ByteArray(32) { 0x0A },
        isVerifiedNickname = false,
        lastSeen = System.currentTimeMillis()
    )

    @Test
    fun `private message routes over BLE when a Noise session is already established`() {
        whenever(bluetooth.getPeerInfo(peerID)).thenReturn(peerInfo(isConnected = true))
        whenever(bluetooth.hasEstablishedSession(peerID)).thenReturn(true)

        service.sendPrivateMessage("hello", peerID, "peer", "msg-1")

        verify(bluetooth, times(1)).sendPrivateMessage("hello", peerID, "peer", "msg-1")
    }

    @Test
    fun `private message still routes over BLE when connected but session not yet established`() {
        // No Wi-Fi Aware peer in this environment either, so BLE is the only real path -
        // this exercises the isBleConnected() fallback branch, not just the isBleReady() one.
        whenever(bluetooth.getPeerInfo(peerID)).thenReturn(peerInfo(isConnected = true))
        whenever(bluetooth.hasEstablishedSession(peerID)).thenReturn(false)

        service.sendPrivateMessage("hello", peerID, "peer", "msg-2")

        verify(bluetooth, times(1)).sendPrivateMessage("hello", peerID, "peer", "msg-2")
    }

    @Test
    fun `disconnected BLE peer still routes to BLE when BLE is the only enabled transport`() {
        // isBleConnected() is false here, but the fallback branch is
        // isBleConnected(...) || (isBleEnabled() && !isWifiConnected(...)) - with BLE enabled
        // (the default) and no Wi-Fi Aware peer, that OR is still true. BLE owns handling an
        // unreachable peer internally (e.g. store-and-forward); UnifiedMeshService isn't meant
        // to silently drop the message just because it isn't connected at this instant.
        whenever(bluetooth.getPeerInfo(peerID)).thenReturn(peerInfo(isConnected = false))
        whenever(bluetooth.hasEstablishedSession(peerID)).thenReturn(false)

        service.sendPrivateMessage("hello", peerID, "peer", "msg-3")

        verify(bluetooth, times(1)).sendPrivateMessage("hello", peerID, "peer", "msg-3")
    }

    @Test
    fun `private message is not sent over BLE when BLE transport is disabled`() {
        DebugSettingsManager.getInstance().setBleEnabled(false)
        whenever(bluetooth.getPeerInfo(peerID)).thenReturn(peerInfo(isConnected = false))
        whenever(bluetooth.hasEstablishedSession(peerID)).thenReturn(false)

        service.sendPrivateMessage("hello", peerID, "peer", "msg-4")

        // With BLE disabled and no Wi-Fi Aware service present in this test environment,
        // this message has nowhere to go - verifying it does NOT fall through to BLE anyway.
        verify(bluetooth, never()).sendPrivateMessage(any(), eq(peerID), any(), any())
    }

    @Test
    fun `peer nicknames include BLE peers when no Wi-Fi Aware transport is present`() {
        whenever(bluetooth.getPeerNicknames()).thenReturn(mapOf(peerID to "peer"))

        val nicknames = service.getPeerNicknames()

        assertEquals(mapOf(peerID to "peer"), nicknames)
    }
}

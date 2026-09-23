package it.scvnsc.whoknows.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNetworkTrackerTest {

    private val wifi = "wifi"
    private val mobile = "mobile"

    @Test
    fun `starts offline without a network and online with one`() {
        assertTrue(DefaultNetworkTracker<String>().isOffline)
        assertFalse(DefaultNetworkTracker(wifi).isOffline)
    }

    @Test
    fun `available network makes the app online`() {
        val tracker = DefaultNetworkTracker<String>()
        tracker.onAvailable(wifi)
        assertFalse(tracker.isOffline)
        assertEquals(wifi, tracker.currentNetwork)
    }

    @Test
    fun `losing the default network makes the app offline`() {
        val tracker = DefaultNetworkTracker(wifi)
        tracker.onLost(wifi)
        assertTrue(tracker.isOffline)
        assertNull(tracker.currentNetwork)
    }

    @Test
    fun `switching network with the new one announced first stays online`() {
        // Wi-Fi -> dati mobili: il sistema annuncia la nuova rete di default, poi segnala la perdita della vecchia
        val tracker = DefaultNetworkTracker(wifi)
        tracker.onAvailable(mobile)
        tracker.onLost(wifi)
        assertFalse(tracker.isOffline)
        assertEquals(mobile, tracker.currentNetwork)
    }

    @Test
    fun `switching network with the old one lost first is offline only in between`() {
        val tracker = DefaultNetworkTracker(wifi)
        tracker.onLost(wifi)
        assertTrue(tracker.isOffline)
        tracker.onAvailable(mobile)
        assertFalse(tracker.isOffline)
    }

    @Test
    fun `losing a network that is not the default is ignored`() {
        // Caso del bug originale: i dati mobili spenti in background mentre si e' sul Wi-Fi
        val tracker = DefaultNetworkTracker(wifi)
        tracker.onLost(mobile)
        assertFalse(tracker.isOffline)
        assertEquals(wifi, tracker.currentNetwork)
    }

    @Test
    fun `airplane mode then reconnection`() {
        val tracker = DefaultNetworkTracker(wifi)
        tracker.onLost(wifi)
        assertTrue(tracker.isOffline)
        tracker.onAvailable(wifi)
        assertFalse(tracker.isOffline)
    }

    @Test
    fun `reset replaces the current network`() {
        val tracker = DefaultNetworkTracker(wifi)
        tracker.reset(null)
        assertTrue(tracker.isOffline)
        tracker.reset(mobile)
        assertEquals(mobile, tracker.currentNetwork)
    }
}

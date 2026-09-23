package it.scvnsc.whoknows.utils

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryUntilSuccessTest {

    //Simula la rete e l'API: ogni elemento di [answers] e' l'esito di un tentativo (null = fallito)
    private class FakeSource(vararg answers: String?) {
        private val queue = ArrayDeque(answers.toList())
        var calls = 0
        var failureIsConnectivity = true
        suspend fun fetch(): String? {
            calls++
            return if (queue.isEmpty()) null else queue.removeFirst()
        }
    }

    @Test
    fun `returns immediately when the first request succeeds`() = runBlocking {
        val source = FakeSource("Q1")
        val waiting = mutableListOf<Boolean>()

        val result = retryUntilSuccess(
            fetch = { source.fetch() },
            shouldStop = { false },
            isOffline = { false },
            lastFailureWasConnectivity = { source.failureIsConnectivity },
            onWaitingForConnection = { waiting += it },
            pollDelayMillis = 1
        )

        assertEquals("Q1", result)
        assertEquals(1, source.calls)
        assertEquals(listOf(false), waiting)
    }

    @Test
    fun `connectivity failures show the waiting state until a request succeeds`() = runBlocking {
        val source = FakeSource(null, null, "Q2")
        val waiting = mutableListOf<Boolean>()

        val result = retryUntilSuccess(
            fetch = { source.fetch() },
            shouldStop = { false },
            isOffline = { false },
            lastFailureWasConnectivity = { true },
            onWaitingForConnection = { waiting += it },
            pollDelayMillis = 1
        )

        assertEquals("Q2", result)
        assertEquals(3, source.calls)
        assertEquals(listOf(true, true, false), waiting)
    }

    @Test
    fun `waits while the system is offline and retries when it comes back`() = runBlocking {
        val source = FakeSource(null, "Q3")
        var offlinePolls = 3

        val result = retryUntilSuccess(
            fetch = { source.fetch() },
            shouldStop = { false },
            isOffline = { offlinePolls-- > 0 },
            lastFailureWasConnectivity = { true },
            onWaitingForConnection = { },
            pollDelayMillis = 1
        )

        assertEquals("Q3", result)
        // nessuna richiesta mentre si e' offline: solo il tentativo iniziale e quello dopo il ritorno della rete
        assertEquals(2, source.calls)
        assertTrue(offlinePolls < 0)
    }

    @Test
    fun `quitting the game while offline stops without further requests`() = runBlocking {
        val source = FakeSource(null)
        var stopped = false
        var polls = 0
        val waiting = mutableListOf<Boolean>()

        val result = retryUntilSuccess(
            fetch = { source.fetch() },
            shouldStop = { stopped },
            isOffline = { polls++; if (polls == 5) stopped = true; true },
            lastFailureWasConnectivity = { true },
            onWaitingForConnection = { waiting += it },
            pollDelayMillis = 1
        )

        assertNull(result)
        assertEquals(1, source.calls)
        // alla fine lo stato di attesa viene sempre azzerato
        assertEquals(false, waiting.last())
    }

    @Test
    fun `non connectivity failures retry without showing the network error`() = runBlocking {
        val source = FakeSource(null, "Q4")
        val waiting = mutableListOf<Boolean>()

        val result = retryUntilSuccess(
            fetch = { source.fetch() },
            shouldStop = { false },
            isOffline = { false },
            lastFailureWasConnectivity = { false },
            onWaitingForConnection = { waiting += it },
            pollDelayMillis = 1
        )

        assertEquals("Q4", result)
        assertFalse(waiting.any { it })
    }

    @Test
    fun `game over already set means a single attempt`() = runBlocking {
        val source = FakeSource(null)

        val result = retryUntilSuccess(
            fetch = { source.fetch() },
            shouldStop = { true },
            isOffline = { false },
            lastFailureWasConnectivity = { true },
            onWaitingForConnection = { },
            pollDelayMillis = 1
        )

        assertNull(result)
        assertEquals(1, source.calls)
    }
}

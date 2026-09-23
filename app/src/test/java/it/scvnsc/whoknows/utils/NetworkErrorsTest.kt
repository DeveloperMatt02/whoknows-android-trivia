package it.scvnsc.whoknows.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorsTest {

    @Test
    fun `connection problems are connectivity errors`() {
        assertTrue(UnknownHostException("opentdb.com").isConnectivityError())
        assertTrue(SocketTimeoutException("timeout").isConnectivityError())
        assertTrue(ConnectException("refused").isConnectivityError())
        assertTrue(IOException("stream closed").isConnectivityError())
    }

    @Test
    fun `api and logic errors are not connectivity errors`() {
        assertFalse(IllegalStateException("OpenTDB returned response_code 5").isConnectivityError())
        assertFalse(IllegalArgumentException("bad").isConnectivityError())
    }
}

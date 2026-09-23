package it.scvnsc.whoknows.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `list survives a round trip`() {
        val answers = listOf("Jupiter", "Saturn", "Neptune")
        assertEquals(answers, converters.fromString(converters.fromList(answers)))
    }

    @Test
    fun `answers containing commas are not split`() {
        val answers = listOf("1,000", "10,000", "100")
        assertEquals(answers, converters.fromString(converters.fromList(answers)))
    }

    @Test
    fun `answers containing quotes and brackets survive a round trip`() {
        val answers = listOf("\"The Matrix\"", "[REDACTED]", "It's")
        assertEquals(answers, converters.fromString(converters.fromList(answers)))
    }

    @Test
    fun `empty list survives a round trip`() {
        assertEquals(emptyList<String>(), converters.fromString(converters.fromList(emptyList())))
    }

    @Test
    fun `legacy comma separated values are still readable`() {
        assertEquals(listOf("False"), converters.fromString("False"))
        assertEquals(listOf("Rome", "Paris", "Berlin"), converters.fromString("Rome,Paris,Berlin"))
    }
}

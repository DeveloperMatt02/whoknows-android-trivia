package it.scvnsc.whoknows.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.random.Random

class GameRulesTest {

    @Test
    fun `points scale with difficulty`() {
        assertEquals(1, GameRules.pointsFor("easy"))
        assertEquals(2, GameRules.pointsFor("medium"))
        assertEquals(3, GameRules.pointsFor("hard"))
    }

    @Test
    fun `points are case insensitive and default to one`() {
        assertEquals(3, GameRules.pointsFor("Hard"))
        assertEquals(1, GameRules.pointsFor("unknown"))
        assertEquals(1, GameRules.pointsFor(null))
    }

    @Test
    fun `shuffled answers contain every answer exactly once`() {
        val incorrect = listOf("Rome", "Paris", "Berlin")
        val answers = GameRules.shuffledAnswers("Madrid", incorrect, Random(42))

        assertEquals(4, answers.size)
        assertEquals((incorrect + "Madrid").sorted(), answers.sorted())
    }

    @Test
    fun `shuffling does not always put the correct answer in the same position`() {
        val positions = (0 until 50).map { seed ->
            GameRules.shuffledAnswers("A", listOf("B", "C", "D"), Random(seed)).indexOf("A")
        }.toSet()

        assertTrue("Correct answer always at index $positions", positions.size > 1)
    }

    @Test
    fun `true or false questions keep both options`() {
        val answers = GameRules.shuffledAnswers("True", listOf("False"), Random(1))
        assertEquals(setOf("True", "False"), answers.toSet())
    }

    @Test
    fun `mixed maps to an empty api parameter`() {
        assertEquals("", GameRules.toApiParameter(GameRules.MIXED))
        assertEquals("Science: Computers", GameRules.toApiParameter("Science: Computers"))
        assertEquals("hard", GameRules.toApiParameter("hard"))
    }

    @Test
    fun `elapsed time is formatted as minutes and seconds`() {
        assertEquals("00:00", GameRules.formatElapsedTime(0, Locale.US))
        assertEquals("00:59", GameRules.formatElapsedTime(59, Locale.US))
        assertEquals("01:05", GameRules.formatElapsedTime(65, Locale.US))
        assertEquals("61:01", GameRules.formatElapsedTime(3661, Locale.US))
    }
}

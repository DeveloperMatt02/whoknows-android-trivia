package it.scvnsc.whoknows.utils

import java.util.Locale
import kotlin.random.Random

//Regole di gioco pure (senza dipendenze Android), separate dal ViewModel per poterle testare con unit test JVM
object GameRules {

    const val STARTING_LIVES = 3

    //Valore usato dall'interfaccia per "qualsiasi categoria/difficolta'"
    const val MIXED = "Mixed"

    //Punti assegnati per una risposta corretta in base alla difficolta' della domanda
    fun pointsFor(difficulty: String?): Int = when (difficulty?.lowercase()) {
        DifficultyType.Easy.name.lowercase() -> 1
        DifficultyType.Medium.name.lowercase() -> 2
        DifficultyType.Hard.name.lowercase() -> 3
        else -> 1
    }

    //Mescola le possibili risposte (altrimenti la risposta corretta sarebbe sempre nella stessa posizione)
    fun shuffledAnswers(
        correctAnswer: String,
        incorrectAnswers: List<String>,
        random: Random = Random.Default
    ): List<String> = (incorrectAnswers + correctAnswer).shuffled(random)

    //L'API vuole un parametro vuoto per "qualsiasi": converte "Mixed" in ""
    fun toApiParameter(value: String): String = if (value == MIXED) "" else value

    //Formatta i secondi trascorsi come mm:ss
    fun formatElapsedTime(totalSeconds: Int, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

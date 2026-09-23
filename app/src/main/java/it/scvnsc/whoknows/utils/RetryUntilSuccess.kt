package it.scvnsc.whoknows.utils

import kotlinx.coroutines.delay

/**
 * Ripete [fetch] finche' non restituisce un valore oppure [shouldStop] diventa vero (es. partita abbandonata).
 *
 * - Dopo ogni tentativo fallito comunica tramite [onWaitingForConnection] se il fallimento dipende
 *   dalla connessione, cosi' la UI puo' mostrare la schermata "No internet connection".
 * - Finche' il sistema risulta offline aspetta con delay(), che sospende la coroutine senza bloccare il thread.
 * - L'eventuale attesa tra due richieste consecutive (rate limit dell'API) e' responsabilita' di [fetch].
 *
 * Separata dal ViewModel (e senza dipendenze Android) per poterla testare con unit test JVM.
 */
suspend fun <T : Any> retryUntilSuccess(
    fetch: suspend () -> T?,
    shouldStop: () -> Boolean,
    isOffline: () -> Boolean,
    lastFailureWasConnectivity: () -> Boolean,
    onWaitingForConnection: (Boolean) -> Unit,
    pollDelayMillis: Long = 1000L
): T? {
    var result = fetch()
    while (result == null && !shouldStop()) {
        onWaitingForConnection(lastFailureWasConnectivity())
        while (isOffline() && !shouldStop()) {
            delay(pollDelayMillis)
        }
        if (shouldStop()) break
        result = fetch()
    }
    onWaitingForConnection(false)
    return result
}

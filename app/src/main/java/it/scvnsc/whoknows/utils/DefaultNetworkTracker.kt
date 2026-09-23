package it.scvnsc.whoknows.utils

/**
 * Tiene traccia della rete di default del sistema a partire dagli eventi del NetworkCallback
 * (onAvailable / onLost). L'app e' offline solo quando non esiste una rete di default.
 *
 * Gli eventi possono arrivare in ordine diverso quando il sistema cambia rete
 * (es. Wi-Fi -> dati mobili): la perdita di una rete che non e' piu' quella di default viene ignorata.
 * La logica e' separata dal Service (e generica sul tipo di rete) per poterla testare con unit test JVM.
 */
class DefaultNetworkTracker<N : Any>(initialNetwork: N? = null) {

    var currentNetwork: N? = initialNetwork
        private set

    val isOffline: Boolean
        @Synchronized get() = currentNetwork == null

    @Synchronized
    fun onAvailable(network: N) {
        currentNetwork = network
    }

    @Synchronized
    fun onLost(network: N) {
        if (network == currentNetwork) {
            currentNetwork = null
        }
    }

    @Synchronized
    fun reset(network: N?) {
        currentNetwork = network
    }
}

package it.scvnsc.whoknows.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import it.scvnsc.whoknows.utils.DefaultNetworkTracker

class NetworkMonitorService : Service() {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private val defaultNetworkTracker = DefaultNetworkTracker<Network>()

    companion object {
        private val _isOffline = MutableLiveData<Boolean>()
        val isOffline: LiveData<Boolean> = _isOffline

        fun startMonitoring(context: Context) {
            //controllo immediatamente lo stato della connessione all'avvio dell'applicazione
            _isOffline.value = !hasInternetConnection(context)
            Log.d("NetworkMonitorService", "startMonitoring: isOffline: ${_isOffline.value}")

            val intent = Intent(context, NetworkMonitorService::class.java)
            context.startService(intent)
        }

        //Controlla la rete attiva, cioe' quella che il sistema sta usando in questo momento
        //(Wi-Fi, dati mobili, Ethernet o VPN), e non una rete qualsiasi tra quelle disponibili
        private fun hasInternetConnection(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        fun stopMonitoring(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                defaultNetworkTracker.onAvailable(network)
                _isOffline.postValue(defaultNetworkTracker.isOffline)
            }

            override fun onLost(network: Network) {
                //Non interrogo activeNetwork: dentro onLost puo' ancora restituire la rete appena persa.
                //Il tracker considera offline solo la perdita della rete di default corrente
                defaultNetworkTracker.onLost(network)
                _isOffline.postValue(defaultNetworkTracker.isOffline)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            // Il callback non era registrato, possiamo ignorare questa eccezione
        }

        //Parto dalla rete attiva in questo momento; alla registrazione onAvailable viene comunque
        //chiamato subito per la rete di default corrente
        defaultNetworkTracker.reset(connectivityManager.activeNetwork)

        //Callback sulla sola rete di default: con registerNetworkCallback(request) arrivavano gli eventi
        //di tutte le reti con accesso a Internet, e la perdita di una rete secondaria (per esempio i dati
        //mobili spenti in background mentre si e' connessi al Wi-Fi) segnava l'app come offline
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

}

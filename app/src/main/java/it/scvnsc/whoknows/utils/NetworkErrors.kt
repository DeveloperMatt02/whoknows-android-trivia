package it.scvnsc.whoknows.utils

import java.io.IOException

//Vero se l'errore dipende dalla connessione (DNS non risolto, timeout, connessione rifiutata o interrotta)
//e non, per esempio, da una risposta di errore dell'API
fun Exception.isConnectivityError(): Boolean = this is IOException

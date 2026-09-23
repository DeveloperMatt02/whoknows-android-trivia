package it.scvnsc.whoknows.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

class Converters {

    //Le liste (es. risposte errate) vengono salvate come array JSON: a differenza del vecchio
    //formato separato da virgole, funziona anche con risposte che contengono virgole (es. "1,000")
    @TypeConverter
    fun fromString(value: String): List<String> {
        if (value.startsWith("[")) {
            try {
                return gson.fromJson(value, listType)
            } catch (e: JsonParseException) {
                //non era JSON valido: provo con il vecchio formato
            }
        }
        //Compatibilita' con i dati salvati dalle versioni precedenti (valori separati da virgola)
        return value.split(",")
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return gson.toJson(list)
    }

    private companion object {
        val gson = Gson()
        val listType = object : TypeToken<List<String>>() {}.type
    }
}

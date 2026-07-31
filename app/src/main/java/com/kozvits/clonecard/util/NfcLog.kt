package com.kozvits.clonecard.util

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Журнал NFC-событий для диагностики прямо на экране (без adb).
 * Каждая запись дублируется в logcat под тегом CloneCard.
 */
object NfcLog {
    val entries = mutableStateListOf<String>()

    fun log(msg: String) {
        android.util.Log.d("CloneCard", msg)
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        entries.add(0, "$time  $msg")
        while (entries.size > 12) entries.removeAt(entries.size - 1)
    }

    fun clear() {
        entries.clear()
        log("Журнал очищен")
    }
}

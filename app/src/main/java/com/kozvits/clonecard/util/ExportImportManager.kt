package com.kozvits.clonecard.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kozvits.clonecard.data.db.DumpEntity
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Экспорт/импорт дампов в JSON-файлы.
 */
object ExportImportManager {

    private val gson = Gson()

    /**
     * Экспортирует дамп в JSON-формат для сохранения в файл.
     */
    fun dumpToJson(dump: DumpEntity): String {
        val data = mapOf(
            "version" to 1,
            "app" to "CloneCard",
            "uid" to dump.uid,
            "uidBytes" to dump.uidBytes.joinToString(","),
            "blocks" to dump.blocks.joinToString(","),
            "label" to dump.label,
            "timestamp" to dump.timestamp,
            "isMagicCard" to dump.isMagicCard
        )
        return gson.toJson(data)
    }

    /**
     * Импортирует дамп из JSON-строки.
     */
    fun jsonToDump(json: String): DumpEntity? {
        return try {
            val map = gson.fromJson<Map<String, Any>>(json, object : TypeToken<Map<String, Any>>() {}.type)
            val uid = map["uid"] as? String ?: return null
            val uidStr = map["uidBytes"] as? String ?: ""
            val blocksStr = map["blocks"] as? String ?: ""
            val label = map["label"] as? String ?: ""
            val timestamp = (map["timestamp"] as? Double)?.toLong() ?: System.currentTimeMillis()
            val isMagic = map["isMagicCard"] as? Boolean ?: false

            DumpEntity(
                uid = uid,
                uidBytes = uidStr.split(",").mapNotNull { it.trim().toIntOrNull() },
                blocks = blocksStr.split(",").mapNotNull { it.trim().toIntOrNull() },
                label = label,
                timestamp = timestamp,
                isMagicCard = isMagic
            )
        } catch (e: Exception) { null }
    }

    /**
     * Читает JSON-файл из Uri (через SAF).
     */
    fun readDumpFromUri(context: Context, uri: Uri): DumpEntity? {
        return try {
            val reader = BufferedReader(InputStreamReader(context.contentResolver.openInputStream(uri)))
            val json = reader.readText()
            reader.close()
            jsonToDump(json)
        } catch (e: Exception) { null }
    }

    /**
     * Эскпортирует все дампы в один JSON-массив.
     */
    fun allDumpsToJson(dumps: List<DumpEntity>): String {
        return gson.toJson(dumps.map { jsonToDump(dumpToJson(it)) })
    }
}

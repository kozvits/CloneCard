package com.kozvits.clonecard.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Представляет полный дамп MIFARE Classic 1K карты (64 блока по 16 байт = 1024 байт).
 */
data class MfcDump(
    val uid: String,                 // HEX UID: "A2 33 0B 2A"
    val uidBytes: List<Int>,         // UID как список байт (0-255)
    val blocks: List<Int>,           // 1024 байта = 64 блока * 16 байт
    val timestamp: Long = System.currentTimeMillis(),
    val label: String = "",
    val fileName: String = "",
    val isMagicCard: Boolean = false
) {
    val sectorCount: Int get() = 16   // MIFARE Classic 1K

    /** Получить блок (список из 16 байт) */
    fun getBlock(index: Int): List<Int> {
        val start = index * 16
        return if (start + 16 <= blocks.size) blocks.subList(start, start + 16)
        else emptyList()
    }

    /** Получить сектор (список из 4 блоков) */
    fun getSector(index: Int): List<List<Int>>? {
        val start = index * 4 * 16
        return if (start + 64 <= blocks.size) {
            (0 until 4).map { b ->
                val bs = start + b * 16
                blocks.subList(bs, bs + 16)
            }
        } else null
    }

    /** Проверить — трейлер ли это блок */
    fun isTrailerBlock(index: Int): Boolean = (index + 1) % 4 == 0

    /** Номер сектора по индексу блока */
    fun sectorOf(index: Int): Int = index / 4

    /** Преобразовать в hex-строку для отображения */
    fun toFormattedString(): String = buildString {
        for (i in 0 until 64) {
            val block = getBlock(i)
            val hex = block.joinToString(" ") { "%02X".format(it) }
            val ascii = block.joinToString("") {
                if (it in 0x20..0x7E) it.toChar().toString() else "."
            }
            append("Block %02X:  %s  | %s\n".format(i, hex, ascii))
        }
    }

    fun toJson(): String = Gson().toJson(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MfcDump) return false
        return uid == other.uid && blocks == other.blocks
    }

    override fun hashCode(): Int = 31 * uid.hashCode() + blocks.hashCode()

    companion object {
        fun fromJson(json: String): MfcDump? = try {
            Gson().fromJson(json, MfcDump::class.java)
        } catch (e: Exception) { null }

        /** Парсит дамп из текстового формата (строки вида: "00: A2 33 0B 2A ...") */
        fun fromHexDump(text: String): MfcDump? {
            val allBlocks = mutableListOf<Int>()
            val lines = text.lines().filter {
                it.isNotBlank() && !it.startsWith("#") && !it.startsWith("//")
            }
            for (line in lines) {
                // Ищем первую колонку hex-байт после ":" или в начале
                val hexPart = line.split("|")[0]  // убираем ASCII справа
                val hexBytes = hexPart
                    .replace(Regex("Block\\s*\\d+:"), "")
                    .replace(Regex("\\b[0-9A-Fa-f]{2}\\b")) {
                        val v = it.value.toIntOrNull(16)
                        if (v != null && v <= 255) it.value else ""
                    }
                    .split(Regex("\\s+"))
                    .mapNotNull { it.trim().toIntOrNull(16) }
                    .filter { it in 0..255 }
                allBlocks.addAll(hexBytes)
            }
            if (allBlocks.size < 16) return null
            val nfcBlocks = allBlocks.take(1024)
            val uidBytes = nfcBlocks.take(4)
            val uid = uidBytes.joinToString(" ") { "%02X".format(it) }
            return MfcDump(uid = uid, uidBytes = uidBytes, blocks = nfcBlocks)
        }
    }
}

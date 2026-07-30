package com.kozvits.clonecard.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import java.io.IOException

/**
 * Состояние при операциях с NFC.
 */
sealed class NfcState {
    data object Idle : NfcState()
    data object WaitingForCard : NfcState()
    data class Scanning(val progress: Float = 0f, val message: String = "") : NfcState()
    data class Success(val message: String) : NfcState()
    data class Error(val message: String) : NfcState()
}

/**
 * Результат чтения карты.
 */
data class ReadResult(
    val uid: List<Int>,
    val atqa: ByteArray? = null,
    val sak: ByteArray? = null,
    val blocks: List<Int>,         // все 1024 байта
    val authKeys: List<String> = emptyList()
) {
    val uidHex: String get() = uid.joinToString(" ") { "%02X".format(it) }
}

/**
 * Инкапсулирует работу с MIFARE Classic через Android NFC.
 */
class MfCardHandler {

    private var mf: MifareClassic? = null

    fun connect(tag: Tag): Boolean = try {
        mf = MifareClassic.get(tag)
        mf?.connect() == true
    } catch (e: IOException) {
        false
    }

    fun disconnect() {
        try { mf?.close() } catch (_: Exception) {}
        mf = null
    }

    val isConnected: Boolean get() = mf?.isConnected == true

    val type: String get() {
        val m = mf ?: return "Unknown"
        return when (m.type) {
            MifareClassic.TYPE_CLASSIC -> "MIFARE Classic"
            MifareClassic.TYPE_PLUS -> "MIFARE Plus"
            MifareClassic.TYPE_PRO -> "MIFARE Pro"
            else -> "Unknown"
        }
    }

    val size: Int get() = mf?.size ?: 0

    val sectorCount: Int get() = mf?.sectorCount ?: 16

    /** Прочитать UID карты. */
    fun readUid(tag: Tag): List<Int> {
        val nfcA = NfcA.get(tag) ?: return emptyList()
        return try {
            if (!nfcA.isConnected) nfcA.connect()
            nfcA.uid.map { it.toInt() and 0xFF }
        } catch (e: Exception) { emptyList() }
        finally { try { nfcA.close() } catch (_: Exception) {} }
    }

    /**
     * Прочитать карту целиком.
     * Использует ключи по умолчанию: [FF..FF, 00..00, A0A1A2A3A4A5, D3F7D3F7D3F7].
     */
    fun readCard(tag: Tag, onProgress: (Float, String) -> Unit = { _, _ -> }): ReadResult? {
        val mf = try { MifareClassic.get(tag)?.also { it.connect() } } catch (_: Exception) { null }
            ?: return null

        val uid = mf.uid.map { it.toInt() and 0xFF }
        val totalSectors = mf.sectorCount
        val allBlocks = mutableListOf<Int>()
        val defaultKeys = listOf(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            byteArrayOf(0, 0, 0, 0, 0, 0),
            byteArrayOf(0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5),
            byteArrayOf(0xD3, 0xF7.toByte(), 0xD3, 0xF7.toByte(), 0xD3, 0xF7.toByte()),
        )
        val authenticated = mutableSetOf<Int>()

        sectorLoop@ for (sector in 0 until totalSectors) {
            onProgress(sector.toFloat() / totalSectors, "Сектор $sector / $totalSectors")

            for (key in defaultKeys) {
                try {
                    mf.authenticateSectorUsingKeyB(sector, key)
                    authenticated.add(sector)
                    break
                } catch (_: Exception) {}
            }

            if (sector !in authenticated) {
                // Пробуем KeyA
                for (key in defaultKeys) {
                    try {
                        mf.authenticateSectorUsingKeyA(sector, key)
                        authenticated.add(sector)
                        break
                    } catch (_: Exception) {}
                }
            }

            // Читаем блоки сектора
            val blockIndex = sector * 4
            for (b in 0 until 4) {
                val blockNum = blockIndex + b
                try {
                    val raw = mf.readBlock(blockNum)
                    val bytes = raw.map { it.toInt() and 0xFF }
                    allBlocks.addAll(bytes)
                } catch (e: Exception) {
                    // Блок не читается — заполняем нулями
                    allBlocks.addAll((0 until 16).map { 0 })
                }
            }
        }

        try { mf.close() } catch (_: Exception) {}
        return if (allBlocks.isNotEmpty()) ReadResult(
            uid = uid,
            blocks = allBlocks,
            authKeys = authenticated.map { "Sector $it" }
        ) else null
    }

    /**
     * Запись одного блока на карту.
     * blockNum: 0-63, data: 16 байт.
     */
    fun writeBlock(tag: Tag, sector: Int, blockNum: Int, data: ByteArray, key: ByteArray = defaultKey): Boolean {
        val mf = try { MifareClassic.get(tag)?.also { it.connect() } } catch (_: Exception) { null }
            ?: return false
        return try {
            mf.authenticateSectorUsingKeyB(sector, key)
            mf.writeBlock(blockNum, data)
            true
        } catch (e: Exception) { false }
        finally { try { mf.close() } catch (_: Exception) {} }
    }

    /**
     * Записать дамп на карту. Каждый блок требует отдельной аутентификации.
     * unsafe = true — разрешить запись блока 0 (UID).
     */
    fun writeCard(
        tag: Tag,
        dump: com.kozvits.clonecard.data.model.MfcDump,
        unsafe: Boolean = false,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Boolean {
        val mf = try { MifareClassic.get(tag)?.also { it.connect() } } catch (_: Exception) { null }
            ?: return false

        val key = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val zeroKey = byteArrayOf(0, 0, 0, 0, 0, 0)
        var success = true

        try {
            for (sector in 0 until 16) {
                onProgress(sector.toFloat() / 16, "Сектор $sector / 16")

                // Аутентификация
                var authed = false
                for (k in listOf(key, zeroKey)) {
                    try {
                        mf.authenticateSectorUsingKeyB(sector, k)
                        authed = true
                        break
                    } catch (_: Exception) {}
                    try {
                        mf.authenticateSectorUsingKeyA(sector, k)
                        authed = true
                        break
                    } catch (_: Exception) {}
                }
                if (!authed) continue

                for (b in 0 until 4) {
                    val blockNum = sector * 4 + b
                    if (blockNum == 0 && !unsafe) continue  // блок UID — только с unsafe

                    val dumpBlock = dump.getBlock(blockNum)
                    if (dumpBlock.size != 16) continue

                    val data = dumpBlock.map { it.toByte() }.toByteArray()
                    try {
                        mf.writeBlock(blockNum, data)
                    } catch (e: Exception) {
                        if (blockNum != 0) success = false
                    }
                }
            }
        } finally {
            try { mf.close() } catch (_: Exception) {}
        }
        return success
    }

    companion object {
        val defaultKey = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
    }
}

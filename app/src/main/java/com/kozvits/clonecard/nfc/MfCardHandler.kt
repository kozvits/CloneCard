package com.kozvits.clonecard.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import java.io.IOException

/**
 * Инкапсулирует работу с MIFARE Classic через Android NFC.
 */
class MfCardHandler {

    private var mf: MifareClassic? = null

    fun connect(tag: Tag): Boolean = try {
        mf = MifareClassic.get(tag)
        mf?.connect()
        mf?.isConnected == true
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

    /** Извлечь UID из тега (список Int для удобства) */
    fun readUid(tag: Tag): List<Int> =
        tag.id.map { it.toInt() and 0xFF }

    /**
     * Прочитать карту целиком (64 блока × 16 байт = 1024 байта).
     * Пробует ключи: FF..FF, 00..00, A0A1A2A3A4A5, D3F7D3F7D3F7.
     */
    fun readCard(
        tag: Tag,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): ReadResult? {
        val mf = try {
            MifareClassic.get(tag)?.also { it.connect() }
        } catch (_: Exception) { null } ?: return null

        val uid = tag.id.map { it.toInt() and 0xFF }
        val totalSectors = mf.sectorCount
        val allBlocks = mutableListOf<Int>()
        val defaultKeys = listOf(
            byteArrayOf(-1, -1, -1, -1, -1, -1),                                // FF FF FF FF FF FF
            byteArrayOf(0, 0, 0, 0, 0, 0),
            byteArrayOf((-96).toByte(), (-95).toByte(), (-94).toByte(), (-93).toByte(), (-92).toByte(), (-91).toByte()),    // A0 A1 A2 A3 A4 A5
            byteArrayOf((-45).toByte(), (-9).toByte(), (-45).toByte(), (-9).toByte(), (-45).toByte(), (-9).toByte()),       // D3 F7 D3 F7 D3 F7
        )
        val authenticated = mutableSetOf<Int>()

        sectorLoop@ for (sector in 0 until totalSectors) {
            onProgress(sector.toFloat() / totalSectors, "Сектор $sector / $totalSectors")

            for (key in defaultKeys) {
                try {
                    mf.authenticateSectorWithKeyB(sector, key)
                    authenticated.add(sector)
                    break
                } catch (_: Exception) {}
            }

            if (sector !in authenticated) {
                for (key in defaultKeys) {
                    try {
                        mf.authenticateSectorWithKeyA(sector, key)
                        authenticated.add(sector)
                        break
                    } catch (_: Exception) {}
                }
            }

            val blockIndex = sector * 4
            for (b in 0 until 4) {
                val blockNum = blockIndex + b
                try {
                    val raw = mf.readBlock(blockNum)
                    val bytes = raw.map { it.toInt() and 0xFF }
                    allBlocks.addAll(bytes)
                } catch (_: Exception) {
                    allBlocks.addAll((0 until 16).map { 0 })
                }
            }
        }

        try { mf.close() } catch (_: Exception) {}

        return if (allBlocks.isNotEmpty())
            ReadResult(uid = uid, blocks = allBlocks)
        else null
    }

    /**
     * Записать один блок на карту.
     */
    fun writeBlock(
        tag: Tag,
        sector: Int,
        blockNum: Int,
        data: ByteArray,
        key: ByteArray = defaultKey
    ): Boolean {
        val mf = try { MifareClassic.get(tag)?.also { it.connect() } } catch (_: Exception) { null }
            ?: return false
        return try {
            mf.authenticateSectorWithKeyB(sector, key)
            mf.writeBlock(blockNum, data)
            true
        } catch (_: Exception) { false }
        finally { try { mf.close() } catch (_: Exception) {} }
    }

    /**
     * Записать полный дамп на карту.
     * @param unsafe true = разрешить запись блока 0 (UID).
     */
    fun writeCard(
        tag: Tag,
        blocks: List<Int>,
        unsafe: Boolean = false,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Boolean {
        val mf = try { MifareClassic.get(tag)?.also { it.connect() } } catch (_: Exception) { null }
            ?: return false

        val key = byteArrayOf(-1, -1, -1, -1, -1, -1)   // FF FF FF FF FF FF
        val zeroKey = byteArrayOf(0, 0, 0, 0, 0, 0)
        var success = true

        try {
            for (sector in 0 until 16) {
                onProgress(sector.toFloat() / 16f, "Сектор $sector / 16")

                var authed = false
                for (k in listOf(key, zeroKey)) {
                    try {
                        mf.authenticateSectorWithKeyB(sector, k)
                        authed = true
                        break
                    } catch (_: Exception) {}
                    try {
                        mf.authenticateSectorWithKeyA(sector, k)
                        authed = true
                        break
                    } catch (_: Exception) {}
                }
                if (!authed) continue

                for (b in 0 until 4) {
                    val blockNum = sector * 4 + b
                    if (blockNum == 0 && !unsafe) continue

                    val start = blockNum * 16
                    if (start + 16 > blocks.size) continue
                    val blockData = blocks.subList(start, start + 16)
                        .map { it.toByte() }.toByteArray()
                    try {
                        mf.writeBlock(blockNum, blockData)
                    } catch (_: Exception) {
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
        val defaultKey = byteArrayOf(-1, -1, -1, -1, -1, -1)
    }
}

/** Результат чтения карты. */
data class ReadResult(
    val uid: List<Int>,
    val blocks: List<Int>     // 1024 байта (64 блока × 16)
) {
    val uidHex: String get() = uid.joinToString(" ") { "%02X".format(it) }
}

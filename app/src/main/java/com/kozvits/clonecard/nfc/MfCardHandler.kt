package com.kozvits.clonecard.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
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
            byteArrayOf((-80).toByte(), (-79).toByte(), (-78).toByte(), (-77).toByte(), (-76).toByte(), (-75).toByte()),   // B0 B1 B2 B3 B4 B5
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
     * @param unsafe true = попытаться записать блок 0 (UID).
     */
    fun writeCard(
        tag: Tag,
        blocks: List<Int>,
        unsafe: Boolean = false,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): WriteResult {
        val mf = try { MifareClassic.get(tag)?.also { it.connect() } } catch (_: Exception) { null }
            ?: return WriteResult(dataOk = false, uidChanged = false, uidError = "Нет подключения к карте")

        val key = byteArrayOf(-1, -1, -1, -1, -1, -1)   // FF FF FF FF FF FF
        val zeroKey = byteArrayOf(0, 0, 0, 0, 0, 0)
        var dataOk = true
        var uidChanged = false
        var uidError: String? = null

        try {
            // --- UID (блок 0) ---
            if (unsafe && blocks.size >= 16) {
                val targetUid = blocks.subList(0, 4)
                // Длина UUID (5-й байт) должна быть 88 для 4-байтного UID
                val block0 = blocks.subList(0, 16).map { it.toByte() }.toByteArray()
                val uidWritten = writeUidBlock0(tag, block0)
                if (uidWritten) {
                    uidChanged = true
                    val cur = readUid(tag).joinToString(" ") { "%02X".format(it) }
                    val tgt = targetUid.joinToString(" ") { "%02X".format(it) }
                    if (cur != tgt) {
                        uidChanged = false
                        uidError = "Карта НЕ magic (UID не изменился). UID перезаписывается только на CUID/Gen-картах."
                    }
                } else {
                    uidError = "Не удалось записать блок 0 (возможно, карта не magic)."
                }
                onProgress(0.05f, "UID: ${if (uidChanged) "OK" else "не изменён"}")
            }

            // --- Данные (секторы 0..15, блоки 1..63) ---
            for (sector in 0 until 16) {
                onProgress(0.05f + sector.toFloat() / 16f * 0.95f, "Сектор $sector / 16")

                var authed = false
                for (k in listOf(key, zeroKey)) {
                    try {
                        mf.authenticateSectorWithKeyB(sector, k); authed = true; break
                    } catch (_: Exception) {}
                    try {
                        mf.authenticateSectorWithKeyA(sector, k); authed = true; break
                    } catch (_: Exception) {}
                }
                if (!authed) continue

                for (b in 0 until 4) {
                    val blockNum = sector * 4 + b
                    if (blockNum == 0) continue   // UID обработан выше

                    val start = blockNum * 16
                    if (start + 16 > blocks.size) continue
                    val blockData = blocks.subList(start, start + 16)
                        .map { it.toByte() }.toByteArray()
                    try {
                        mf.writeBlock(blockNum, blockData)
                    } catch (_: Exception) {
                        dataOk = false
                    }
                }
            }
        } finally {
            try { mf.close() } catch (_: Exception) {}
        }
        return WriteResult(dataOk = dataOk, uidChanged = uidChanged, uidError = uidError)
    }

    /**
     * Записать блок 0 (UID) через backdoor-команды magic-карт.
     * Пробует Gen2 (direct write) и Gen1a (cnippet-команда 0x43 + 0xA0 с паролем).
     * Возвращает true, если команда отправлена без ошибки транспорта.
     */
    fun writeUidBlock0(tag: Tag, block0: ByteArray): Boolean {
        if (block0.size < 16) return false

        // 1) Gen2 / CUID — прямая запись блока 0 через MifareClassic.writeBlock
        try {
            val mf = MifareClassic.get(tag) ?: return false
            if (!mf.isConnected) mf.connect()
            // Аутентификация сектора 0 ключом A = FF FF FF FF FF FF (или 00..00)
            var authed = false
            for (k in arrayOf(
                byteArrayOf(-1, -1, -1, -1, -1, -1),
                byteArrayOf(0, 0, 0, 0, 0, 0)
            )) {
                try { mf.authenticateSectorWithKeyA(0, k); authed = true; break }
                catch (_: Exception) {}
                try { mf.authenticateSectorWithKeyB(0, k); authed = true; break }
                catch (_: Exception) {}
            }
            if (authed) {
                mf.writeBlock(0, block0)
                return true
            }
        } catch (_: Exception) {}

        // 2) Gen1a (Magic, старые) — backdoor через cnippet-команду
        try {
            val nfcA = NfcA.get(tag) ?: return false
            if (!nfcA.isConnected) nfcA.connect()
            val wrapper = byteArrayOf(
                0xAD.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00
            )
            // WUPA (0x52) + cnippet unlock (0x40 0x43 0xA0 0x00) + 16 байт блока 0
            val unlock = byteArrayOf(0x40, 0x43, 0xA0.toByte(), 0x00)
            nfcA.transceive(wrapper)            // разблокировка backdoor
            nfcA.transceive(unlock + block0)    // запись блока 0
            try { nfcA.close() } catch (_: Exception) {}
            return true
        } catch (_: Exception) {}

        return false
    }

    companion object {
        val defaultKey = byteArrayOf(-1, -1, -1, -1, -1, -1)
    }
}

/** Результат записи карты. */
data class WriteResult(
    val dataOk: Boolean,
    val uidChanged: Boolean,
    val uidError: String?
) {
    val success: Boolean get() = dataOk && (uidChanged || uidError == null)
}

/** Результат чтения карты. */
data class ReadResult(
    val uid: List<Int>,
    val blocks: List<Int>     // 1024 байта (64 блока × 16)
) {
    val uidHex: String get() = uid.joinToString(" ") { "%02X".format(it) }
}

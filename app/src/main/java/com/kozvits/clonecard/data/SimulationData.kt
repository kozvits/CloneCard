package com.kozvits.clonecard.data

import com.kozvits.clonecard.data.db.DumpEntity

/**
 * Набор предустановленных дампов для имитации работы без физической карты.
 */
object SimulationData {

    /** Домофонный ключ Vizit (оригинал) — UID: A2 33 0B 2A */
    val vizitDump: DumpEntity by lazy {
        val blocks = generateDumpBlocks("A2 33 0B 2A")
        DumpEntity(
            uid = "A2 33 0B 2A",
            uidBytes = listOf(0xA2, 0x33, 0x0B, 0x2A),
            blocks = blocks,
            label = "Ключ домофона Vizit (симуляция)",
            isMagicCard = false,
            isSimulation = true
        )
    }

    /** Magic Card Gen2 (заводская) — UID: 83 DB B1 6F */
    val magicDump: DumpEntity by lazy {
        val blocks = generateDumpBlocks("83 DB B1 6F", magic = true)
        DumpEntity(
            uid = "83 DB B1 6F",
            uidBytes = listOf(0x83, 0xDB, 0xB1, 0x6F),
            blocks = blocks,
            label = "Magic Card Gen2 (симуляция)",
            isMagicCard = true,
            isSimulation = true
        )
    }

    /** Пустая чистая карта */
    val cleanDump: DumpEntity by lazy {
        val blocks = generateDumpBlocks("FD 02 B2 6F", blank = true)
        DumpEntity(
            uid = "FD 02 B2 6F",
            uidBytes = listOf(0xFD, 0x02, 0xB2, 0x6F),
            blocks = blocks,
            label = "Чистая карта (симуляция)",
            isMagicCard = false,
            isSimulation = true
        )
    }

    val defaultDumps: List<DumpEntity> get() = listOf(vizitDump, magicDump, cleanDump)

    /** Создать 1024 байт (64 блока × 16) с характерными данными */
    private fun generateDumpBlocks(uidHex: String, magic: Boolean = false, blank: Boolean = false): List<Int> {
        val uidParts = uidHex.split(" ").mapNotNull { it.trim().toIntOrNull(16) }
        val uid = if (uidParts.size >= 4) uidParts.take(4) else listOf(0, 0, 0, 0)

        val blocks = mutableListOf<Int>()

        for (sector in 0 until 16) {
            for (block in 0 until 4) {
                val blockIndex = sector * 4 + block

                if (sector == 0 && block == 0) {
                    // Блок 0: UID + SAK + ATQA
                    if (!blank) {
                        blocks.addAll(uid)
                        blocks.addAll(listOf(0xB0, 0x08, 0x04, 0x00))
                        if (magic) {
                            blocks.addAll(listOf(0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69)) // bcdefghi
                        } else {
                            blocks.addAll(listOf(0x62, 0x63, 0x25, 0x49, 0xC0, 0x7E, 0x68, 0x69))
                        }
                    } else {
                        blocks.addAll(uid)
                        blocks.addAll((0 until 12).map { 0 })
                    }
                } else if (block == 3) {
                    // Трейлер сектора: KeyA + AccessBits + KeyB
                    if (!blank) {
                        blocks.addAll(listOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  // KeyA
                            0xFF, 0x07, 0x80, 0x69,                       // Access bits
                            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF))         // KeyB (FF)
                    } else {
                        blocks.addAll((0 until 16).map { 0 })
                    }
                } else {
                    // Блок данных: заполняем нулями (UID-only ключи)
                    if (!blank && sector == 0 && block == 1) {
                        // Имитация данных в блоке 1
                        blocks.addAll(listOf(0x11, 0x22, 0x33, 0x44) + (0 until 12).map { 0 })
                    } else {
                        blocks.addAll((0 until 16).map { 0 })
                    }
                }
            }
        }
        return blocks
    }

    /** Создать чистый дамп (все нули + UID) для очистки карты */
    fun createBlankDump(uid: String = "00 00 00 00"): DumpEntity {
        val uidParts = uid.split(" ").mapNotNull { it.trim().toIntOrNull(16) }
        val realUid = if (uidParts.size >= 4) uidParts.take(4) else listOf(0, 0, 0, 0)

        val blocks = mutableListOf<Int>()
        for (sector in 0 until 16) {
            for (block in 0 until 4) {
                val blockIndex = sector * 4 + block
                if (sector == 0 && block == 0) {
                    blocks.addAll(realUid)
                    blocks.addAll((0 until 12).map { 0 })
                } else if (block == 3) {
                    // Трейлер с заводскими FF-ключами
                    blocks.addAll(listOf(
                        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
                        0xFF, 0x07, 0x80, 0x69,
                        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
                    ))
                } else {
                    blocks.addAll((0 until 16).map { 0 })
                }
            }
        }
        return DumpEntity(
            uid = realUid.joinToString(" ") { "%02X".format(it) },
            uidBytes = realUid,
            blocks = blocks,
            label = "Чистая карта",
            isSimulation = true
        )
    }
}

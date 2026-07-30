package com.kozvits.clonecard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kozvits.clonecard.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumpDetailScreen(
    dumpId: Long,
    onBack: () -> Unit,
    onWrite: (Long) -> Unit,
    onCompare: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    // Пример данных
    val uid = "A2 33 0B 2A"
    val label = "Ключ домофона (оригинал)"
    val isMagic = false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { onDelete(dumpId) }) {
                        Icon(Icons.Filled.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCompare(dumpId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CompareArrows, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сравнить")
                    }
                    Button(
                        onClick = { onWrite(dumpId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Записать на карту")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Информация о дампе
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Verified,
                            null,
                            tint = NfcSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("UID: $uid", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Тип: MIFARE Classic 1K")
                    Text("Размер: 1024 байт (64 блока)")
                    Text("Секторов: 16")
                    Text("Дата: 20.07.2026 15:30")
                    if (isMagic) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("⚠ Magic Card", color = Warning, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hex-дамп
            Text(
                "Дамп (hex)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    // Пример данных дампа (первые 64 блока)
                    val sampleBlocks = listOf(
                        "00: A2 33 0B 2A B0 08 04 00 62 63 25 49 C0 7E 68 69  | .3.*....bc%I.~hi",
                        "01: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  | ................",
                        "02: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  | ................",
                        "03: 00 00 00 00 00 00 FF 07 80 69 FF FF FF FF FF FF  | .........i......",
                    )
                    val allBlocks = (0 until 16).flatMap { sector ->
                        val uidBlock = if (sector == 0) listOf(
                            "00: A2 33 0B 2A B0 08 04 00 62 63 25 49 C0 7E 68 69  | .3.*....bc%I.~hi"
                        ) else listOf(
                            "%02X: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  | ................".format(sector * 4),
                            "%02X: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  | ................".format(sector * 4 + 1),
                            "%02X: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  | ................".format(sector * 4 + 2),
                            "%02X: 00 00 00 00 00 00 FF 07 80 69 FF FF FF FF FF FF  | .........i......".format(sector * 4 + 3),
                        )
                        uidBlock
                    }

                    itemsIndexed(allBlocks) { index, block ->
                        val isTrailer = (index + 1) % 4 == 0
                        Text(
                            text = block,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (isTrailer) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

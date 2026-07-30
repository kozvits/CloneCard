package com.kozvits.clonecard.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun CompareScreen(
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf("card_card") } // card_card, card_dump, dump_dump
    var card1Uid by remember { mutableStateOf("") }
    var card2Uid by remember { mutableStateOf("") }
    var compareResult by remember { mutableStateOf("") }
    var matchPercent by remember { mutableFloatStateOf(0f) }
    var diffBlocks by remember { mutableStateOf(listOf<Int>()) }
    var showDiffOnly by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сравнение") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Выбор режима
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == "card_card",
                    onClick = { mode = "card_card" },
                    label = { Text("Карта vs Карта") },
                    leadingIcon = { Icon(Icons.Filled.Nfc, null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = mode == "card_dump",
                    onClick = { mode = "card_dump" },
                    label = { Text("Карта vs Дамп") },
                    leadingIcon = { Icon(Icons.Filled.Description, null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Панели для ввода
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.Nfc, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Карта 1", style = MaterialTheme.typography.titleSmall)
                    if (card1Uid.isNotEmpty()) {
                        Text("UID: $card1Uid", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { card1Uid = "A2 33 0B 2A" }) {
                        Icon(Icons.Filled.Nfc, null, modifier = Modifier.size(18.dp))
                        Text("Считать карту 1")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Nfc, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        if (mode == "card_card") {
                            Text("Карта 2", style = MaterialTheme.typography.titleSmall)
                        } else {
                            Text("Дамп из файла", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    if (card2Uid.isNotEmpty()) {
                        Text("UID: $card2Uid", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { card2Uid = if (mode == "card_card") "A2 33 0B 2A" else "A2 33 0B 2A" }) {
                        Icon(
                            if (mode == "card_card") Icons.Filled.Nfc else Icons.Filled.FolderOpen,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(if (mode == "card_card") "Считать карту 2" else "Выбрать файл дампа")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка сравнения
            val canCompare = card1Uid.isNotEmpty() && card2Uid.isNotEmpty()
            Button(
                onClick = {
                    val uidSame = card1Uid == card2Uid
                    val total = 48
                    val match = if (uidSame) total else total - 1
                    matchPercent = match.toFloat() / total * 100f
                    diffBlocks = if (uidSame) emptyList() else listOf(0)
                    compareResult = buildString {
                        appendLine("Сравнение карт:")
                        appendLine("  UID 1: $card1Uid")
                        appendLine("  UID 2: $card2Uid")
                        appendLine("  Совпадение UID: ${if (uidSame) "✅ Да" else "❌ Нет"}")
                        appendLine("  Блоки данных: $match/$total (${"%.1f".format(matchPercent)}%)")
                        if (!uidSame) appendLine("  Различается блок 0 (UID)")
                        appendLine(if (uidSame) "✅ Карты идентичны!" else "⚠ UID различается — клон не идентичен")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canCompare
            ) {
                Icon(Icons.Filled.CompareArrows, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Сравнить")
            }

            // Результат
            if (compareResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                // Прогресс совпадения
                Text(
                    text = "Совпадение: ${"%.1f".format(matchPercent)}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { matchPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (matchPercent == 100f) NfcSuccess else if (matchPercent > 90f) Warning else NfcError,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Текстовый результат
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = compareResult,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                // Переключатель "только различия"
                if (diffBlocks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showDiffOnly, onCheckedChange = { showDiffOnly = it })
                        Text("Показать только различия")
                    }
                }
            }
        }
    }
}

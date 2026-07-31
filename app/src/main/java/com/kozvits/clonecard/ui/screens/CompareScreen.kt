package com.kozvits.clonecard.ui.screens

import androidx.compose.foundation.layout.*
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
import com.kozvits.clonecard.MainActivity
import com.kozvits.clonecard.data.SimulationData
import com.kozvits.clonecard.data.db.DumpEntity
import com.kozvits.clonecard.data.repository.DumpRepository
import com.kozvits.clonecard.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onBack: () -> Unit,
    repository: DumpRepository,
    activity: MainActivity,
    nfcState: MutableState<MainActivity.NfcStatus>,
    pendingAction: MutableState<MainActivity.PendingNfcAction?>,
    scope: CoroutineScope
) {
    var leftDump by remember { mutableStateOf<DumpEntity?>(null) }
    var rightDump by remember { mutableStateOf<DumpEntity?>(null) }
    var compareResult by remember { mutableStateOf("") }
    var matchPercent by remember { mutableFloatStateOf(0f) }
    var showDumpPicker by remember { mutableStateOf(false) }
    // Режим «считать обе карты»: 1-я → A, 2-я → B
    var bothMode by remember { mutableStateOf(false) }
    var bothModeState by remember { mutableStateOf("") }

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
            // Панель 1
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Дамп A", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (leftDump != null) {
                        Text("UID: ${leftDump!!.uid}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        Text(leftDump!!.label.ifEmpty { "Без имени" }, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                leftDump = repository.allDumps.first().firstOrNull()
                            }
                        }) {
                            Icon(Icons.Filled.Folder, null, modifier = Modifier.size(18.dp))
                            Text("Из дампов")
                        }
                        OutlinedButton(onClick = {
                            leftDump = SimulationData.vizitDump
                        }) {
                            Icon(Icons.Filled.Science, null, modifier = Modifier.size(18.dp))
                            Text("Имитация")
                        }
                        OutlinedButton(onClick = {
                            pendingAction.value = MainActivity.PendingNfcAction.ReadCard { data ->
                                leftDump = DumpEntity(uid = data.uid, uidBytes = data.uidBytes, blocks = data.blocks, label = "Карта A")
                            }
                        }) {
                            Icon(Icons.Filled.Nfc, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Панель 2
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Дамп B", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (rightDump != null) {
                        Text("UID: ${rightDump!!.uid}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        Text(rightDump!!.label.ifEmpty { "Без имени" }, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                rightDump = repository.allDumps.first()
                                    .drop(1).firstOrNull()
                                    ?: repository.allDumps.first().firstOrNull()
                            }
                        }) {
                            Icon(Icons.Filled.Folder, null, modifier = Modifier.size(18.dp))
                            Text("Из дампов")
                        }
                        OutlinedButton(onClick = { rightDump = SimulationData.magicDump }) {
                            Icon(Icons.Filled.Science, null, modifier = Modifier.size(18.dp))
                            Text("Имитация")
                        }
                        OutlinedButton(onClick = {
                            pendingAction.value = MainActivity.PendingNfcAction.ReadCard { data ->
                                rightDump = DumpEntity(uid = data.uid, uidBytes = data.uidBytes, blocks = data.blocks, label = "Карта B")
                            }
                        }) {
                            Icon(Icons.Filled.Nfc, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // === СЧИТАТЬ ОБЕ КАРТЫ ПО NFC ===
            Button(
                onClick = {
                    leftDump = null
                    rightDump = null
                    compareResult = ""
                    bothMode = true
                    bothModeState = "Поднесите карту A к NFC..."
                    pendingAction.value = MainActivity.PendingNfcAction.ReadCard { data ->
                        leftDump = DumpEntity(
                            uid = data.uid, uidBytes = data.uidBytes,
                            blocks = data.blocks, label = "Карта A"
                        )
                        bothModeState = "Карта A: ${data.uid}. Поднесите карту B..."
                        // Пере-взвод для второй карты
                        pendingAction.value = MainActivity.PendingNfcAction.ReadCard { data2 ->
                            rightDump = DumpEntity(
                                uid = data2.uid, uidBytes = data2.uidBytes,
                                blocks = data2.blocks, label = "Карта B"
                            )
                            bothMode = false
                            bothModeState = "Обе карты прочитаны. Нажмите «Сравнить»."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Filled.Nfc, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Считать обе карты по NFC")
            }

            // Статус режима «обе карты»
            if (bothMode || bothModeState.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (nfcState.value.error != null) NfcError.copy(alpha = 0.12f)
                        else NfcScanning.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (nfcState.value.scanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Nfc, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            buildString {
                                append(bothModeState)
                                nfcState.value.error?.let { append("  ($it)") }
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопка сравнения
            Button(
                onClick = {
                    val left = leftDump ?: return@Button
                    val right = rightDump ?: return@Button

                    val minLen = minOf(left.blocks.size, right.blocks.size)
                    val match = (0 until minLen).count { left.blocks[it] == right.blocks[it] }
                    matchPercent = if (minLen > 0) match.toFloat() / minLen * 100f else 0f

                    val uidMatch = left.uid == right.uid
                    val diffBlocks = mutableListOf<Int>()
                    for (i in 0 until minOf(64, minLen / 16)) {
                        val leftBlock = left.blocks.drop(i * 16).take(16)
                        val rightBlock = right.blocks.drop(i * 16).take(16)
                        if (leftBlock != rightBlock) diffBlocks.add(i)
                    }

                    compareResult = buildString {
                        appendLine("Сравнение дампов:")
                        appendLine()
                        appendLine("UID A: ${left.uid}")
                        appendLine("UID B: ${right.uid}")
                        appendLine("Совпадение UID: ${if (uidMatch) "✅ Да" else "❌ Нет"}")
                        appendLine()
                        appendLine("Совпадение блоков данных: $match/$minLen байт (${"%.1f".format(matchPercent)}%)")
                        appendLine("Различающиеся блоки: ${diffBlocks.joinToString(", ")}")
                        appendLine()
                        appendLine(if (match == minLen && uidMatch) "✅ Дампы идентичны!"
                        else "⚠ Дампы различаются")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = leftDump != null && rightDump != null
            ) {
                Icon(Icons.Filled.CompareArrows, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Сравнить")
            }

            // Результат
            if (compareResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Совпадение: ${"%.1f".format(matchPercent)}%", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { matchPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (matchPercent >= 100f) NfcSuccess else if (matchPercent > 90f) Warning else NfcError,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())
                    ) {
                        Text(compareResult, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

package com.kozvits.clonecard.ui.screens

import androidx.compose.foundation.background
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
import com.kozvits.clonecard.MainActivity
import com.kozvits.clonecard.data.SimulationData
import com.kozvits.clonecard.data.db.DumpEntity
import com.kozvits.clonecard.data.repository.DumpRepository
import com.kozvits.clonecard.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadScreen(
    onBack: () -> Unit,
    repository: DumpRepository,
    activity: MainActivity,
    nfcState: MutableState<MainActivity.NfcStatus>,
    pendingAction: MutableState<MainActivity.PendingNfcAction?>,
    scope: CoroutineScope
) {
    var readDump by remember { mutableStateOf<MainActivity.ReadResultData?>(null) }
    var saved by remember { mutableStateOf(false) }

    // Авто-взвод чтения: открыл экран → поднёс карту → карта читается.
    // Повторный взвод после закрытия дампа (readDump = null).
    LaunchedEffect(readDump) {
        if (readDump == null) {
            pendingAction.value = MainActivity.PendingNfcAction.ReadCard { data ->
                readDump = data
                scope.launch {
                    val entity = DumpEntity(
                        uid = data.uid,
                        uidBytes = data.uidBytes,
                        blocks = data.blocks,
                        label = "Карта ${data.uid}"
                    )
                    repository.saveDump(entity)
                    saved = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Считать карту") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Статус
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        nfcState.value.scanning -> NfcScanning.copy(alpha = 0.15f)
                        nfcState.value.error != null -> NfcError.copy(alpha = 0.15f)
                        readDump != null -> NfcSuccess.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when {
                            nfcState.value.scanning -> Icons.Filled.Sync
                            nfcState.value.error != null -> Icons.Filled.Error
                            readDump != null -> Icons.Filled.CheckCircle
                            else -> Icons.Filled.Nfc
                        },
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = when {
                            nfcState.value.scanning -> NfcScanning
                            nfcState.value.error != null -> NfcError
                            readDump != null -> NfcSuccess
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val rd = readDump
                    Text(
                        text = when {
                            nfcState.value.scanning -> nfcState.value.message
                            nfcState.value.error != null -> "Ошибка: ${nfcState.value.error}"
                            rd != null -> "Карта прочитана! UID: ${rd.uid}"
                            else -> "Поднесите карту к NFC-считывателю"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (nfcState.value.scanning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка чтения
            Button(
                onClick = {
                    saved = false
                    pendingAction.value = MainActivity.PendingNfcAction.ReadCard { data ->
                        readDump = data
                        // Сохраняем в БД
                        scope.launch {
                            val entity = DumpEntity(
                                uid = data.uid,
                                uidBytes = data.uidBytes,
                                blocks = data.blocks,
                                label = "Карта ${data.uid}"
                            )
                            repository.saveDump(entity)
                            saved = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !nfcState.value.scanning
            ) {
                Icon(Icons.Filled.Nfc, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Поднести карту к NFC")
            }

            // Имитация
            if (!nfcState.value.scanning && readDump == null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val sim = SimulationData.vizitDump
                        readDump = MainActivity.ReadResultData(
                            uid = sim.uid,
                            uidBytes = sim.uidBytes,
                            blocks = sim.blocks
                        )
                        scope.launch {
                            repository.saveDump(sim)
                            saved = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Science, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Имитация: Ключ Vizit")
                }
            }

            // Результат
            readDump?.let { dump ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Дамп карты", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        val lines = dump.blocks.chunked(16).mapIndexed { i, block ->
                            val hex = block.joinToString(" ") { "%02X".format(it) }
                            val ascii = block.joinToString("") {
                                if (it in 0x20..0x7E) it.toChar().toString() else "."
                            }
                            "Block %02X:  $hex  | $ascii".format(i)
                        }
                        itemsIndexed(lines) { index, line ->
                            val isTrailer = (index + 1) % 4 == 0
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (isTrailer) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (saved) "✅ Дамп сохранён в БД" else "Сохранение...",
                    style = MaterialTheme.typography.bodySmall,
                    color = NfcSuccess
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { readDump = null }) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

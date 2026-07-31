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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kozvits.clonecard.MainActivity
import com.kozvits.clonecard.data.SimulationData
import com.kozvits.clonecard.data.repository.DumpRepository
import com.kozvits.clonecard.ui.theme.*
import com.kozvits.clonecard.util.NfcLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    repository: DumpRepository,
    scope: CoroutineScope,
    activity: MainActivity? = null,
    pendingAction: MutableState<MainActivity.PendingNfcAction?>? = null,
    nfcState: MutableState<MainActivity.NfcStatus>? = null
) {
    var writeUnsafe by remember { mutableStateOf(true) }
    var showEraseConfirm by remember { mutableStateOf(false) }
    var showResetSimConfirm by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }

    if (showEraseConfirm) {
        AlertDialog(
            onDismissRequest = { showEraseConfirm = false },
            title = { Text("Очистка карты") },
            text = { Text("Поднесите карту к NFC. Все блоки (кроме UID) будут заполнены нулями, ключи сброшены на FF.") },
            confirmButton = {
                Button(onClick = {
                    showEraseConfirm = false
                    statusMsg = "Поднесите карту к NFC для очистки..."
                    pendingAction?.value = MainActivity.PendingNfcAction.EraseCard { ok ->
                        statusMsg = if (ok) "Карта очищена успешно" else "Ошибка очистки карты"
                    }
                }) { Text("Поднести карту") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEraseConfirm = false }) { Text("Отмена") }
            }
        )
    }

    if (showResetSimConfirm) {
        AlertDialog(
            onDismissRequest = { showResetSimConfirm = false },
            title = { Text("Сброс симуляции?") },
            text = { Text("Удалить все текущие симуляции и создать новые.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        repository.deleteSimulationDumps()
                        repository.saveDumps(SimulationData.defaultDumps)
                        statusMsg = "Симуляции сброшены"
                    }
                    showResetSimConfirm = false
                }) { Text("Сбросить") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetSimConfirm = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
                .verticalScroll(rememberScrollState())
        ) {
            if (statusMsg.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NfcSuccess.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = NfcSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(statusMsg)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // === ОЧИСТКА КАРТЫ ===
            Text(
                "Очистка карты",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = NfcError.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Функция очистки карты",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Записывает на карту чистый дамп: все блоки данных нулевые, ключи FF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showEraseConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NfcError),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.DeleteForever, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ОЧИСТИТЬ КАРТУ")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === РЕЖИМЫ ЗАПИСИ ===
            Text(
                "Режимы записи",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Запись UID (--unsafe)", style = MaterialTheme.typography.bodyLarge)
                            Text("Разрешить запись блока 0 на Magic Card",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(checked = writeUnsafe, onCheckedChange = { writeUnsafe = it })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Safe mode", style = MaterialTheme.typography.bodyLarge)
                            Text("Не трогать трейлеры секторов",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(checked = true, onCheckedChange = {})
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === СИМУЛЯЦИЯ ===
            Text(
                "Симуляция",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Дампы для имитации работы без физической карты.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showResetSimConfirm = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                            Text("Сбросить симуляции")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === КЛЮЧИ ===
            Text(
                "Ключи аутентификации",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf(
                        "FF FF FF FF FF FF (заводской)",
                        "00 00 00 00 00 00",
                        "A0 A1 A2 A3 A4 A5",
                        "D3 F7 D3 F7 D3 F7",
                        "B0 B1 B2 B3 B4 B5",
                    ).forEach { key ->
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === О ПРОГРАММЕ ===
            Text(
                "О программе",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CloneCard v1.0", style = MaterialTheme.typography.bodyLarge)
                    Text("Работа с MIFARE Classic картами: чтение, запись, клонирование",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("Room DB | NFC Foreground Dispatch | Jetpack Compose",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("github.com/kozvits/CloneCard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === ДИАГНОСТИКА NFC ===
            Text(
                "Диагностика NFC",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Журнал NFC-событий: поднесите карту и посмотрите, что происходит.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { NfcLog.clear() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Очистить журнал")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (NfcLog.entries.isEmpty()) {
                        Text(
                            "Пусто. Откройте экран чтения, нажмите кнопку чтения и поднесите карту.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    } else {
                        NfcLog.entries.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

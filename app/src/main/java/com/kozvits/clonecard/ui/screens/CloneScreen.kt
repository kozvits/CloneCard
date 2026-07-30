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
import com.kozvits.clonecard.MainActivity
import com.kozvits.clonecard.data.SimulationData
import com.kozvits.clonecard.data.db.DumpEntity
import com.kozvits.clonecard.data.repository.DumpRepository
import com.kozvits.clonecard.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneScreen(
    onBack: () -> Unit,
    repository: DumpRepository,
    activity: MainActivity,
    nfcState: MutableState<MainActivity.NfcStatus>,
    pendingAction: MutableState<MainActivity.PendingNfcAction?>,
    scope: CoroutineScope,
    preSelectedDumpId: Long = -1L
) {
    var step by remember { mutableIntStateOf(0) }
    var sourceDump by remember { mutableStateOf<DumpEntity?>(null) }
    var targetUid by remember { mutableStateOf("") }
    var writeUnsafe by remember { mutableStateOf(true) }
    var cloneResult by remember { mutableStateOf("") }
    var verifyOk by remember { mutableStateOf(false) }

    // Загружаем дамп если передан ID
    LaunchedEffect(preSelectedDumpId) {
        if (preSelectedDumpId > 0) {
            sourceDump = repository.getDumpById(preSelectedDumpId)
            if (sourceDump != null) step = 1
        }
    }

    val steps = listOf(
        "Выбор источника",
        "Проверка дампа",
        "Запись на карту",
        "Верификация",
        "Готово!"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Клонирование") },
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
            // Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                steps.forEachIndexed { i, s ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when {
                                i < step -> NfcSuccess
                                i == step -> NfcScanning
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = s,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (i <= step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Контент
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (step) {
                    0 -> {
                        Text(
                            "Выберите источник для клонирования",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Из дампов
                        Card(
                            onClick = {
                                scope.launch {
                                    repository.allDumps.collect { dumps ->
                                        if (dumps.isNotEmpty()) {
                                            sourceDump = dumps.first()
                                            step = 1
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Из сохранённых дампов", fontWeight = FontWeight.Medium)
                                    Text("Выбрать из ранее прочитанных", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // С имитацией
                        Card(
                            onClick = {
                                sourceDump = SimulationData.vizitDump
                                step = 1
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Science, null, tint = Warning)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Имитация: Ключ Vizit", fontWeight = FontWeight.Medium)
                                    Text("UID: A2 33 0B 2A", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Через NFC
                        Card(
                            onClick = {
                                pendingAction.value = MainActivity.PendingNfcAction.ReadCard { data ->
                                    sourceDump = DumpEntity(uid = data.uid, uidBytes = data.uidBytes, blocks = data.blocks, label = "Клон ${data.uid}")
                                    step = 1
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Nfc, null, tint = NfcReady)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Считать через NFC", fontWeight = FontWeight.Medium)
                                    Text("Поднести оригинал карты к телефону", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    1 -> {
                        sourceDump?.let { dump ->
                            Text("Источник: ${dump.label}", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("UID: ${dump.uid}", fontWeight = FontWeight.Bold)
                                    Text("Блоков: ${dump.blocks.size / 16}")
                                    if (dump.isMagicCard) Text("⚠ Magic Card", color = Warning)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = writeUnsafe, onCheckedChange = { writeUnsafe = it })
                                        Text("Запись UID (--unsafe)")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        Text(
                            "Поднесите ЦЕЛЕВУЮ карту к NFC",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (nfcState.value.scanning) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(nfcState.value.message)
                                } else {
                                    Icon(Icons.Filled.Nfc, null, modifier = Modifier.size(64.dp), tint = NfcReady)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            sourceDump?.let { dump ->
                                                pendingAction.value = MainActivity.PendingNfcAction.WriteCard(dump, writeUnsafe) { ok ->
                                                    cloneResult = if (ok) "Запись выполнена" else "Запись с ошибками"
                                                    step = 3
                                                }
                                            }
                                        },
                                        enabled = sourceDump != null
                                    ) {
                                        Text("Записать клон")
                                    }
                                }
                            }
                        }

                        if (targetUid.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("UID цели: $targetUid", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    3 -> {
                        sourceDump?.let { dump ->
                            Text("Верификация", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Источник: ${dump.uid}")
                                    Text("Результат: $cloneResult")
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            pendingAction.value = MainActivity.PendingNfcAction.ReadCard { data ->
                                                val uidMatch = data.uid == dump.uid
                                                verifyOk = uidMatch
                                                if (uidMatch) step = 4
                                                else cloneResult = "UID не совпадает! Оригинал: ${dump.uid}, Клон: ${data.uid}"
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Verified, null)
                                        Text("Проверить клон через NFC")
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            verifyOk = true
                                            step = 4
                                        }
                                    ) {
                                        Text("Пропустить верификацию")
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        Icon(
                            imageVector = if (verifyOk) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally),
                            tint = if (verifyOk) NfcSuccess else Warning
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (verifyOk) "Клонирование успешно!" else "Клонирование завершено",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            cloneResult,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 0) OutlinedButton(onClick = { step-- }) { Text("Назад") }
                else Spacer(modifier = Modifier.width(1.dp))
                if (step < steps.size - 1) Button(onClick = { step++ }, enabled = sourceDump != null) { Text("Пропустить →") }
                else Button(onClick = onBack) { Text("На главную") }
            }
        }
    }
}

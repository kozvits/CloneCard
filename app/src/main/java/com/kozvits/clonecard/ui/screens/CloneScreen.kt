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
import com.kozvits.clonecard.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneScreen(
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var sourceUid by remember { mutableStateOf("") }
    var sourceDump by remember { mutableStateOf("") }
    var targetUid by remember { mutableStateOf("") }
    var cloneResult by remember { mutableStateOf("") }
    var verifyResult by remember { mutableStateOf("") }
    var writeUnsafe by remember { mutableStateOf(true) }

    val steps = listOf(
        "Приложите ОРИГИНАЛ карты для чтения",
        "Проверка дампа оригинала",
        "Приложите ЦЕЛЕВУЮ карту для записи",
        "Запись клона...",
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
                        if (i < steps.size - 1) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = (i + 1).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Текущий шаг
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (step) {
                            0, 2 -> Icons.Filled.Nfc
                            1 -> Icons.Filled.Description
                            3 -> Icons.Filled.Edit
                            4 -> Icons.Filled.Verified
                            else -> Icons.Filled.CheckCircle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Шаг ${step + 1} из ${steps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = steps[step],
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Контент шага
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (step) {
                    0 -> StepCard("Оригинал", sourceUid, sourceDump) {
                        Text("Поднесите оригинальную карту к NFC-считывателю телефона.")
                        Spacer(modifier = Modifier.height(8.dp))
                        sourceUid = "A2 33 0B 2A"
                        sourceDump = "Данные прочитаны (1024 байт)"
                    }
                    1 -> StepCard("Дамп оригинала", sourceUid, sourceDump) {
                        Text("UID: A2 33 0B 2A")
                        Text("Тип: MIFARE Classic 1K, UID-only")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = writeUnsafe, onCheckedChange = { writeUnsafe = it })
                            Text("Запись UID (--unsafe)")
                        }
                    }
                    2 -> StepCard("Целевая карта", targetUid, "") {
                        Text("Поднесите чистую карту к NFC-считывателю.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Текущий UID: $targetUid")
                        if (targetUid.isEmpty()) {
                            Text("UID будет отображён после чтения карты",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    3 -> {
                        StepCard("Запись...", targetUid, "") {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Запись дампа на целевую карту...")
                        }
                        cloneResult = "Записано: 63/64 блока (UID не записан)"
                    }
                    4 -> StepCard("Верификация", sourceUid, "") {
                        Text("UID оригинала: A2 33 0B 2A")
                        Text("UID клона:    $targetUid")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Совпадение данных: 48/48 блоков")
                        if (sourceUid == targetUid) {
                            Text("✅ Клон идентичен оригиналу (включая UID)",
                                color = NfcSuccess, fontWeight = FontWeight.Bold)
                        } else {
                            Text("⚠ UID различается. Если домофон UID-only — клон не сработает.",
                                color = Warning)
                        }
                    }
                    5 -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally),
                            tint = NfcSuccess
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Клонирование завершено!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            cloneResult,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            verifyResult,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки навигации
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 0) {
                    OutlinedButton(onClick = { step-- }) {
                        Icon(Icons.Filled.ArrowBack, null)
                        Text("Назад")
                    }
                } else { Spacer(modifier = Modifier.width(1.dp)) }

                if (step < steps.size - 1) {
                    Button(onClick = { step++ }) {
                        Text("Далее")
                        Icon(Icons.Filled.ArrowForward, null)
                    }
                } else {
                    Button(onClick = onBack) {
                        Icon(Icons.Filled.Home, null)
                        Text("На главную")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    title: String,
    uid: String,
    dumpInfo: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (uid.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "UID: $uid",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (dumpInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dumpInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

package com.kozvits.clonecard.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
fun ReadScreen(
    onBack: () -> Unit,
    onCardRead: (List<Int>) -> Unit
) {
    var state by remember { mutableStateOf("idle") } // idle, waiting, reading, done, error
    var uid by remember { mutableStateOf("") }
    var dumpText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var sectorInfo by remember { mutableStateOf("") }

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
            // Индикатор NFC
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (state) {
                        "idle" -> MaterialTheme.colorScheme.surfaceVariant
                        "waiting" -> NfcScanning
                        "reading" -> NfcScanning
                        "done" -> NfcSuccess
                        "error" -> NfcError
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (state) {
                            "idle" -> Icons.Filled.Nfc
                            "waiting" -> Icons.Filled.Contactless
                            "reading" -> Icons.Filled.Sync
                            "done" -> Icons.Filled.CheckCircle
                            "error" -> Icons.Filled.Error
                            else -> Icons.Filled.Nfc
                        },
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = when (state) {
                            "done" -> NfcSuccess
                            "error" -> NfcError
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (state) {
                            "idle" -> "Нажмите «Начать чтение» и поднесите карту"
                            "waiting" -> "Поднесите карту к NFC..."
                            "reading" -> "Чтение карты... ${(progress * 100).toInt()}%"
                            "done" -> "Карта прочитана"
                            "error" -> "Ошибка: $errorMsg"
                            else -> state
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (state == "reading") {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (uid.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "UID: $uid | $sectorInfo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка чтения
            if (state == "idle" || state == "error") {
                Button(
                    onClick = {
                        state = "waiting"
                        uid = ""
                        dumpText = ""
                        errorMsg = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = state != "reading"
                ) {
                    Icon(Icons.Filled.Nfc, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (state == "error") "Повторить" else "Начать чтение",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Результат
            if (dumpText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Дамп карты",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Дамп в hex-виде
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        val lines = dumpText.lines().filter { it.isNotBlank() }
                        itemsIndexed(lines) { index, line ->
                            val isTrailer = (index + 1) % 4 == 0
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (isTrailer)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCardRead(emptyList()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сохранить дамп")
                    }
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

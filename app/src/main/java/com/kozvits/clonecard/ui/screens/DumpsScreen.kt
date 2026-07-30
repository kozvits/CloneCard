package com.kozvits.clonecard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kozvits.clonecard.ui.theme.*

data class DumpItem(
    val id: Long,
    val uid: String,
    val label: String,
    val timestamp: String,
    val size: String = "1024 байт",
    val isMagic: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumpsScreen(
    onBack: () -> Unit,
    onDumpClick: (Long) -> Unit,
    onDumpDelete: (Long) -> Unit
) {
    // Пример данных
    val dumps = remember {
        listOf(
            DumpItem(1, "A2 33 0B 2A", "Ключ домофона (оригинал)", "20.07.2026", "1024 байт"),
            DumpItem(2, "83 DB B1 6F", "Magic Card Gen2 (тест)", "20.07.2026", "1024 байт", isMagic = true),
            DumpItem(3, "FD 02 B2 6F", "Чистая карта (заводская)", "19.07.2026", "1024 байт", isMagic = true),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сохранённые дампы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { /* импорт */ }) {
                        Icon(Icons.Filled.FileOpen, "Импорт")
                    }
                    IconButton(onClick = { /* экспорт всех */ }) {
                        Icon(Icons.Filled.IosShare, "Экспорт")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* импорт дампа */ },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Импорт дампа") }
            )
        }
    ) { padding ->
        if (dumps.isEmpty()) {
            // Пустое состояние
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Нет сохранённых дампов",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    "Считайте карту или импортируйте дамп из файла",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dumps, key = { it.id }) { dump ->
                    Card(
                        onClick = { onDumpClick(dump.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (dump.isMagic)
                                    Icons.Filled.AutoAwesome else Icons.Filled.CreditCard,
                                contentDescription = null,
                                tint = if (dump.isMagic) Warning else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dump.label.ifEmpty { "Без имени" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "UID: ${dump.uid}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Row {
                                    Text(
                                        text = dump.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dump.size,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    if (dump.isMagic) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Magic",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Warning
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { onDumpDelete(dump.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    "Удалить",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

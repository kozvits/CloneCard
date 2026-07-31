package com.kozvits.clonecard.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kozvits.clonecard.data.db.DumpEntity
import com.kozvits.clonecard.data.repository.DumpRepository
import com.kozvits.clonecard.ui.theme.*
import com.kozvits.clonecard.util.ExportImportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumpsScreen(
    onBack: () -> Unit,
    onDumpClick: (Long) -> Unit,
    repository: DumpRepository,
    scope: CoroutineScope
) {
    val dumps by repository.allDumps.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val dump = ExportImportManager.readDumpFromUri(context, uri)
                if (dump != null) {
                    repository.saveDump(dump)
                    Toast.makeText(context, "Дамп импортирован: ${dump.uid}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ошибка: неверный JSON-файл дампа", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сохранённые дампы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                icon = { Icon(Icons.Filled.FileOpen, null) },
                text = { Text("Импорт") }
            )
        }
    ) { padding ->
        if (dumps.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.FolderOff, null, modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Нет сохранённых дампов",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("Считайте карту или откройте симуляцию",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dumps, key = { it.id }) { dump ->
                    DumpCard(
                        dump = dump,
                        dateFormat = dateFormat,
                        onClick = { onDumpClick(dump.id) },
                        onDelete = {
                            scope.launch { repository.deleteDumpById(dump.id) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DumpCard(
    dump: DumpEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val blockCount = if (dump.blocks.isNotEmpty()) dump.blocks.size / 16 else 0
    val dateStr = dateFormat.format(Date(dump.timestamp))

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    dump.isSimulation -> Icons.Filled.Science
                    dump.isMagicCard -> Icons.Filled.AutoAwesome
                    else -> Icons.Filled.CreditCard
                },
                contentDescription = null,
                tint = when {
                    dump.isSimulation -> Warning
                    dump.isMagicCard -> Warning
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dump.label.ifEmpty { "Без имени" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dump.uid,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Row {
                    Text(dateStr, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    if (blockCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$blockCount блоков", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    if (dump.isSimulation) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Симуляция", style = MaterialTheme.typography.labelSmall, color = Warning)
                    }
                    if (dump.isMagicCard) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Magic", style = MaterialTheme.typography.labelSmall, color = Warning)
                    }
                }
            }
            if (!dump.isSimulation) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Удалить",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

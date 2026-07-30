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
import com.kozvits.clonecard.MainActivity
import com.kozvits.clonecard.data.db.DumpEntity
import com.kozvits.clonecard.data.repository.DumpRepository
import com.kozvits.clonecard.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumpDetailScreen(
    dumpId: Long,
    onBack: () -> Unit,
    onWrite: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    repository: DumpRepository,
    scope: CoroutineScope,
    activity: MainActivity,
    pendingAction: MutableState<MainActivity.PendingNfcAction?>,
    nfcState: MutableState<MainActivity.NfcStatus>
) {
    var dump by remember { mutableStateOf<DumpEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(dumpId) {
        dump = repository.getDumpById(dumpId)
        loading = false
    }

    if (showDeleteConfirm && dump != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить дамп?") },
            text = { Text("Дамп UID: ${dump!!.uid} будет безвозвратно удалён.") },
            confirmButton = {
                Button(onClick = {
                    onDelete(dumpId)
                    showDeleteConfirm = false
                }, colors = ButtonDefaults.buttonColors(containerColor = NfcError)) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dump?.label ?: "Загрузка...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (dump != null && !dump!!.isSimulation) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (dump != null) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                pendingAction.value = MainActivity.PendingNfcAction.ReadCard { data ->
                                    scope.launch {
                                        val entity = DumpEntity(
                                            uid = data.uid,
                                            uidBytes = data.uidBytes,
                                            blocks = data.blocks,
                                            label = "Сравнение с ${dump!!.uid}"
                                        )
                                        repository.saveDump(entity)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Nfc, null, modifier = Modifier.size(18.dp))
                            Text("Сравнить с NFC")
                        }
                        Button(
                            onClick = { onWrite(dumpId) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                            Text("Записать на карту")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (dump == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Дамп не найден")
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                // Инфо
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (dump!!.isSimulation) {
                                Icon(Icons.Filled.Science, null, tint = Warning, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Симуляция", color = Warning, fontWeight = FontWeight.Medium)
                            } else {
                                Icon(Icons.Filled.Verified, null, tint = NfcSuccess, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Реальная карта", fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("UID: ${dump!!.uid}", fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                        Text("Размер: ${dump!!.blocks.size} байт (${dump!!.blocks.size / 16} блоков)")
                        Text("Дата: ${dateFormat.format(Date(dump!!.timestamp))}")
                        if (dump!!.isMagicCard) Text("⚠ Magic Card", color = Warning)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Hex-дамп", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        val lines = dump!!.blocks.chunked(16).mapIndexed { i, block ->
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
            }
        }
    }
}

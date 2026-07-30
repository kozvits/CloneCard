package com.kozvits.clonecard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kozvits.clonecard.MainActivity
import com.kozvits.clonecard.ui.navigation.Screen
import com.kozvits.clonecard.ui.theme.NfcReady
import com.kozvits.clonecard.ui.theme.NfcScanning
import kotlinx.coroutines.CoroutineScope

private val DEFAULT_MENU_COLOR = androidx.compose.ui.graphics.Color(0xFF6750A4)

data class MenuCard(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val color: androidx.compose.ui.graphics.Color = DEFAULT_MENU_COLOR
)

private val menuItems = listOf(
    MenuCard("Считать карту", "Чтение UID и полного дампа", Icons.Filled.Nfc, Screen.Read.route),
    MenuCard("Клонировать", "Мастер: чтение → запись → верификация", Icons.Filled.ContentCopy, Screen.Clone.route),
    MenuCard("Сравнить", "Карта vs дамп или два дампа", Icons.Filled.CompareArrows, Screen.Compare.route),
    MenuCard("Дампы", "Сохранённые, импорт/экспорт", Icons.Filled.Folder, Screen.Dumps.route),
    MenuCard("Настройки", "Режимы, очистка, симуляция", Icons.Filled.Settings, Screen.Settings.route),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    nfcState: MutableState<MainActivity.NfcStatus>,
    activity: MainActivity,
    pendingAction: MutableState<MainActivity.PendingNfcAction?>,
    scope: CoroutineScope
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CloneCard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // NFC статус
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (nfcState.value.scanning)
                        NfcScanning.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (nfcState.value.scanning) Icons.Filled.Sync
                        else if (nfcState.value.error != null) Icons.Filled.Error
                        else Icons.Filled.Nfc,
                        contentDescription = null,
                        tint = if (nfcState.value.scanning) NfcScanning
                        else NfcReady,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = nfcState.value.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        val dump = nfcState.value.dump
                        if (dump != null) {
                            Text(
                                text = "UID: ${dump.uid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Быстрые действия",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menuItems) { item ->
                    Card(
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.height(150.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = item.color
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

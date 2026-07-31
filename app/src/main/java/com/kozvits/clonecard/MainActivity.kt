package com.kozvits.clonecard

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kozvits.clonecard.data.SimulationData
import com.kozvits.clonecard.data.db.DumpEntity
import com.kozvits.clonecard.data.repository.DumpRepository
import com.kozvits.clonecard.nfc.MfCardHandler
import com.kozvits.clonecard.nfc.NfcManager
import com.kozvits.clonecard.ui.navigation.NavGraph
import com.kozvits.clonecard.ui.navigation.Screen
import com.kozvits.clonecard.ui.theme.CloneCardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var nfcManager: NfcManager
    private lateinit var repository: DumpRepository
    private val mfHandler = MfCardHandler()

    // Состояния, к которым обращаются экраны
    val nfcState = mutableStateOf(NfcStatus())
    val pendingAction = mutableStateOf<PendingNfcAction?>(null)

    data class NfcStatus(
        val scanning: Boolean = false,
        val message: String = "Готов к чтению",
        val dump: ReadResultData? = null,
        val error: String? = null
    )

    data class ReadResultData(
        val uid: String,
        val uidBytes: List<Int>,
        val blocks: List<Int>,
        val type: String = "MIFARE Classic 1K"
    )

    sealed class PendingNfcAction {
        data class ReadCard(val onResult: (ReadResultData) -> Unit) : PendingNfcAction()
        data class WriteCard(val dumpEntity: DumpEntity, val unsafe: Boolean, val onResult: (Boolean) -> Unit) : PendingNfcAction()
        data class EraseCard(val onResult: (Boolean) -> Unit) : PendingNfcAction()
        data class CompareCard(val dumpEntity: DumpEntity, val onResult: (ReadResultData) -> Unit) : PendingNfcAction()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = DumpRepository.getInstance(this)
        nfcManager = NfcManager(this)

        // Показываем NFC-статус при старте
        if (!nfcManager.isAvailable) {
            nfcState.value = nfcState.value.copy(message = "NFC не поддерживается на этом устройстве")
        } else if (!nfcManager.isEnabled) {
            nfcState.value = nfcState.value.copy(message = "Включите NFC в настройках")
        }

        // Холодный старт: приложение запущено поднесённой картой
        if (nfcManager.isNfcAction(intent)) {
            handleNfcIntent(intent)
        }

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }
            val scope = rememberCoroutineScope()

            CloneCardTheme {
                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                Screen.bottomNavItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, screen.shortDesc) },
                                        label = { Text(screen.shortDesc) },
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(Screen.Home.route) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavGraph(
                            navController = navController,
                            repository = repository,
                            activity = this@MainActivity,
                            nfcState = nfcState,
                            pendingAction = pendingAction,
                            scope = scope
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (nfcManager.isAvailable) {
            nfcManager.enableForegroundDispatch()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            nfcManager.disableForegroundDispatch()
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        val actionName = intent.action
        val hasTagExtra = intent.hasExtra(NfcAdapter.EXTRA_TAG)
        android.util.Log.d("CloneCard", "NFC intent: action=$actionName, hasExtra=$hasTagExtra")
        val tag = nfcManager.resolveIntent(intent)
        if (tag == null) {
            // Ошибку показываем ТОЛЬКО для настоящих NFC-действий без тега.
            // Мусорные интенты (MAIN, null action без EXTRA_TAG и т.п.) — молча.
            if (nfcManager.isNfcAction(intent)) {
                android.util.Log.w("CloneCard", "NFC action $actionName but Tag extra missing")
                nfcState.value = nfcState.value.copy(
                    scanning = false,
                    error = "Тег не распознан ($actionName)",
                    message = "Неподдерживаемый тип карты"
                )
            } else {
                android.util.Log.w("CloneCard", "Non-NFC intent ignored: $actionName (hasExtra=$hasTagExtra)")
            }
            return
        }

        val uidPreview = tag.id.joinToString(" ") { "%02X".format(it) }
        android.util.Log.d("CloneCard", "Tag detected, UID=$uidPreview, techs=${tag.techList.joinToString(",")}")

        val action = pendingAction.value
        if (action == null) {
            android.util.Log.w("CloneCard", "No pending action — card ignored")
            Toast.makeText(this, "Карта обнаружена! Сначала выберите действие", Toast.LENGTH_SHORT).show()
            nfcState.value = nfcState.value.copy(
                scanning = false,
                message = "Карта обнаружена. Выберите действие на экране.",
                error = null
            )
            return
        }

        // Потребляем действие сразу — экраны взводят новое при необходимости
        pendingAction.value = null

        when (action) {
            is PendingNfcAction.ReadCard -> {
                readCardAsync(tag, action.onResult)
            }
            is PendingNfcAction.WriteCard -> {
                writeCardAsync(tag, action.dumpEntity, action.unsafe, action.onResult)
            }
            is PendingNfcAction.EraseCard -> {
                eraseCardAsync(tag, action.onResult)
            }
            is PendingNfcAction.CompareCard -> {
                readCardAsync(tag, action.onResult)
            }
        }
    }

    private fun readCardAsync(tag: Tag, onResult: (ReadResultData) -> Unit) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            nfcState.value = nfcState.value.copy(scanning = true, message = "Чтение карты...")
            try {
                val uid = mfHandler.readUid(tag)
                val result = mfHandler.readCard(tag) { progress, msg ->
                    nfcState.value = nfcState.value.copy(
                        message = "Чтение: ${(progress * 100).toInt()}% — $msg"
                    )
                }
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        val data = ReadResultData(
                            uid = result.uidHex,
                            uidBytes = result.uid,
                            blocks = result.blocks
                        )
                        nfcState.value = nfcState.value.copy(
                            scanning = false,
                            message = "Карта прочитана: ${result.uidHex}",
                            dump = data
                        )
                        onResult(data)
                    } else {
                        nfcState.value = nfcState.value.copy(
                            scanning = false,
                            error = "Не удалось прочитать карту",
                            message = "Ошибка чтения"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    nfcState.value = nfcState.value.copy(
                        scanning = false,
                        error = e.message ?: "Ошибка NFC",
                        message = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    private fun writeCardAsync(tag: Tag, dump: DumpEntity, unsafe: Boolean, onResult: (Boolean) -> Unit) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            nfcState.value = nfcState.value.copy(scanning = true, message = "Запись карты...")
            try {
                val ok = mfHandler.writeCard(tag, dump.blocks, unsafe) { progress, msg ->
                    nfcState.value = nfcState.value.copy(
                        message = "Запись: ${(progress * 100).toInt()}% — $msg"
                    )
                }
                withContext(Dispatchers.Main) {
                    nfcState.value = nfcState.value.copy(
                        scanning = false,
                        message = if (ok) "Карта записана успешно" else "Запись с ошибками"
                    )
                    onResult(ok)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    nfcState.value = nfcState.value.copy(
                        scanning = false,
                        message = "Ошибка записи: ${e.message}"
                    )
                    onResult(false)
                }
            }
        }
    }

    private fun eraseCardAsync(tag: Tag, onResult: (Boolean) -> Unit) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            nfcState.value = nfcState.value.copy(scanning = true, message = "Очистка карты...")
            try {
                val uid = mfHandler.readUid(tag)
                val uidHex = uid.joinToString(" ") { "%02X".format(it) }
                val blankDump = SimulationData.createBlankDump(uidHex)
                val ok = mfHandler.writeCard(tag, blankDump.blocks, unsafe = true) { progress, msg ->
                    nfcState.value = nfcState.value.copy(
                        message = "Очистка: ${(progress * 100).toInt()}% — $msg"
                    )
                }
                withContext(Dispatchers.Main) {
                    nfcState.value = nfcState.value.copy(
                        scanning = false,
                        message = if (ok) "Карта очищена" else "Очистка с ошибками"
                    )
                    onResult(ok)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    nfcState.value = nfcState.value.copy(
                        scanning = false,
                        message = "Ошибка очистки: ${e.message}"
                    )
                    onResult(false)
                }
            }
        }
    }
}

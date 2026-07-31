package com.kozvits.clonecard.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.os.Build
import com.kozvits.clonecard.util.NfcLog

/**
 * Управление NFC: Reader Mode (основной путь — тег приходит в колбэк,
 * минуя интенты; надёжен на MIUI/HyperOS) + Foreground Dispatch (резерв).
 */
class NfcManager(private val activity: Activity) : NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var tagListener: ((Tag) -> Unit)? = null
    private var foregroundActive = false
    private var readerActive = false

    val isForegroundActive: Boolean get() = foregroundActive
    val isReaderActive: Boolean get() = readerActive

    init {
        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        } catch (e: Exception) {
            NfcLog.log("Ошибка getDefaultAdapter: ${e.message}")
        }
        NfcLog.log(if (nfcAdapter != null) "NFC адаптер найден" else "NFC адаптер НЕ найден")
    }

    val isAvailable: Boolean get() = nfcAdapter != null

    val isEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    fun setTagListener(listener: (Tag) -> Unit) {
        tagListener = listener
    }

    /**
     * Основной путь: Reader Mode. Тег доставляется в onTagDiscovered
     * напрямую, без intent-механики, которая на HyperOS работает нестабильно.
     */
    fun enableReaderMode() {
        val adapter = nfcAdapter ?: return
        try {
            adapter.enableReaderMode(
                activity, this,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
            readerActive = true
            NfcLog.log("Reader mode: ВКЛ")
        } catch (e: Exception) {
            readerActive = false
            NfcLog.log("Reader mode: ОШИБКА ${e.message}")
        }
    }

    fun disableReaderMode() {
        try {
            nfcAdapter?.disableReaderMode(activity)
        } catch (_: Exception) {}
        readerActive = false
        NfcLog.log("Reader mode: ВЫКЛ")
    }

    /** Резервный путь: Foreground Dispatch (интент-доставка). */
    fun enableForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        try {
            val intent = Intent(activity.applicationContext, activity.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = android.app.PendingIntent.getActivity(
                activity, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        android.app.PendingIntent.FLAG_IMMUTABLE
            )
            // Широкий набор tech-списков: ловим любую карту 13.56 МГц
            val techLists = arrayOf(
                arrayOf(MifareClassic::class.java.name),
                arrayOf(NfcA::class.java.name),
                arrayOf(Ndef::class.java.name),
                arrayOf(IsoDep::class.java.name),
                arrayOf(NfcB::class.java.name),
                arrayOf(NfcF::class.java.name),
            )
            adapter.enableForegroundDispatch(activity, pendingIntent, null, techLists)
            foregroundActive = true
            NfcLog.log("Foreground dispatch: ВКЛ")
        } catch (e: Exception) {
            foregroundActive = false
            NfcLog.log("Foreground dispatch: ОШИБКА ${e.message}")
        }
    }

    fun disableForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
        } catch (_: Exception) {}
        foregroundActive = false
        NfcLog.log("Foreground dispatch: ВЫКЛ")
    }

    /**
     * Достать Tag из intent (для резервного пути).
     * action НЕ проверяем: на HyperOS интент может прийти с пустым action.
     */
    fun resolveIntent(intent: Intent?): Tag? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag
        }
    }

    /** True, если action — одно из штатных NFC-действий */
    fun isNfcAction(intent: Intent?): Boolean {
        val action = intent?.action ?: return false
        return action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
                action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                action == NfcAdapter.ACTION_TAG_DISCOVERED
    }

    override fun onTagDiscovered(tag: Tag) {
        NfcLog.log("Reader mode: тег получен")
        tagListener?.invoke(tag)
    }

    companion object {
        const val REQUEST_ENABLE_NFC = 1001
    }
}

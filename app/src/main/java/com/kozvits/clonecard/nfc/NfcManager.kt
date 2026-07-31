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
import android.util.Log

/**
 * Управление NFC foreground dispatch.
 * Позволяет Activity получать NFC-теги в режиме foreground.
 */
class NfcManager(private val activity: Activity) {

    private var nfcAdapter: NfcAdapter? = null

    init {
        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        } catch (_: Exception) {}
    }

    val isAvailable: Boolean get() = nfcAdapter != null

    val isEnabled: Boolean get() = nfcAdapter?.isEnabled == true

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
            // Широкий набор tech-списков: ловим любую карту 13.56 МГц,
            // а не только MIFARE Classic. Мэтчинг = "тег содержит ВСЕ tech из списка".
            val techLists = arrayOf(
                arrayOf(MifareClassic::class.java.name),
                arrayOf(NfcA::class.java.name),
                arrayOf(Ndef::class.java.name),
                arrayOf(IsoDep::class.java.name),
                arrayOf(NfcB::class.java.name),
                arrayOf(NfcF::class.java.name),
            )
            adapter.enableForegroundDispatch(activity, pendingIntent, null, techLists)
            Log.d("CloneCard", "Foreground dispatch enabled")
        } catch (e: Exception) {
            Log.e("CloneCard", "enableForegroundDispatch failed", e)
        }
    }

    fun disableForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
        } catch (_: Exception) {}
    }

    /**
     * Достать Tag из intent.
     * На Android 13+ используем типизированный getParcelableExtra,
     * т.к. устаревший вариант может вернуть null.
     *
     * ВАЖНО: action НЕ проверяем — на некоторых прошивках (MIUI/HyperOS)
     * foreground-dispatch интент приходит с пустым action, но с EXTRA_TAG.
     * Тег извлекается напрямую; если его нет — это не NFC-интент.
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

    companion object {
        const val REQUEST_ENABLE_NFC = 1001
    }
}

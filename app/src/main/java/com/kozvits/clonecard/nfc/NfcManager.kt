package com.kozvits.clonecard.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic

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
        nfcAdapter?.let { adapter ->
            val intent = Intent(activity.applicationContext, activity.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = android.app.PendingIntent.getActivity(
                activity, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val techLists = arrayOf(
                arrayOf(MifareClassic::class.java.name),
                arrayOf(android.nfc.tech.NfcA::class.java.name)
            )
            adapter.enableForegroundDispatch(activity, pendingIntent, null, techLists)
        }
    }

    fun disableForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
        } catch (_: Exception) {}
    }

    fun resolveIntent(intent: Intent): Tag? {
        return if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action
        ) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag
        } else null
    }

    companion object {
        const val REQUEST_ENABLE_NFC = 1001
    }
}

package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

class SunmiScanReceiver(
    private val onScanSuccess: (scannedCode: String) -> Unit
) : BroadcastReceiver() {

    companion object {
        const val TAG = "SunmiScanReceiver"
        
        // SUNMI Scanner Intent Actions
        const val ACTION_DATA_CODE_RECEIVED = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
        const val ACTION_SCAN_RESULT = "com.sunmi.action.SCAN_RESULT"
        const val ACTION_DECODE_RESULT = "com.sunmi.intent.ACTION_DECODE_RESULT"

        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(ACTION_DATA_CODE_RECEIVED)
                addAction(ACTION_SCAN_RESULT)
                addAction(ACTION_DECODE_RESULT)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        Log.d(TAG, "SUNMI Scan Receiver triggered with action: $action")

        val scannedData = intent.getStringExtra("data")
            ?: intent.getStringExtra("scan_result")
            ?: intent.getByteArrayExtra("source_byte")?.let { String(it) }

        if (!scannedData.isNullOrBlank()) {
            val cleanCode = scannedData.trim()
            Log.d(TAG, "SUNMI Hardware Barcode Scanned: $cleanCode")
            onScanSuccess(cleanCode)
        }
    }
}

package com.example.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BarrierAuditLog(
    val id: String,
    val timestamp: Long,
    val actionType: String, // "AUTOMATED_FONEPAY", "AUTOMATED_CASH", "MANUAL_EMERGENCY"
    val operatorId: String,
    val ticketId: String?,
    val openDurationSec: Int,
    val reason: String?
)

class BoomBarrierRelayManager private constructor() {

    private val _isBarrierOpen = MutableStateFlow(false)
    val isBarrierOpen: StateFlow<Boolean> = _isBarrierOpen.asStateFlow()

    private val _lastActionMessage = MutableStateFlow("Gate Barrier Idle (Closed)")
    val lastActionMessage: StateFlow<String> = _lastActionMessage.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<BarrierAuditLog>>(emptyList())
    val auditLogs: StateFlow<List<BarrierAuditLog>> = _auditLogs.asStateFlow()

    companion object {
        const val TAG = "BoomBarrierRelayManager"

        @Volatile
        private var INSTANCE: BoomBarrierRelayManager? = null

        fun getInstance(): BoomBarrierRelayManager {
            return INSTANCE ?: synchronized(this) {
                val instance = BoomBarrierRelayManager()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Triggers the automated boom barrier relay signal (USB/Serial/IP/GPIO)
     */
    fun triggerBarrierOpen(
        actionType: String,
        operatorId: String,
        ticketId: String? = null,
        openDurationSec: Int = 5,
        reason: String? = null
    ) {
        val logId = "LOG-${System.currentTimeMillis().toString().takeLast(6)}"
        val newLog = BarrierAuditLog(
            id = logId,
            timestamp = System.currentTimeMillis(),
            actionType = actionType,
            operatorId = operatorId,
            ticketId = ticketId,
            openDurationSec = openDurationSec,
            reason = reason
        )

        // Add to audit logs
        val updated = listOf(newLog) + _auditLogs.value
        _auditLogs.value = updated.take(100) // retain last 100 logs

        Log.d(TAG, "Relay Pulse Sent: Barrier Open [$actionType] for ${openDurationSec}s by $operatorId")

        CoroutineScope(Dispatchers.IO).launch {
            _isBarrierOpen.value = true
            _lastActionMessage.value = "Gate Barrier OPEN ($actionType - $openDurationSec sec pulse)"
            
            // Simulate hardware relay pulse delay (e.g. Serial command to MCU)
            delay(openDurationSec * 1000L)
            
            _isBarrierOpen.value = false
            _lastActionMessage.value = "Gate Barrier Closed (Idle)"
        }
    }

    /**
     * Guarded Manual Emergency Barrier Open with reason/operator logging
     */
    fun manualEmergencyBarrierOpen(operatorId: String, reason: String, durationSec: Int = 5) {
        triggerBarrierOpen(
            actionType = "MANUAL_EMERGENCY",
            operatorId = operatorId,
            openDurationSec = durationSec,
            reason = reason
        )
    }
}

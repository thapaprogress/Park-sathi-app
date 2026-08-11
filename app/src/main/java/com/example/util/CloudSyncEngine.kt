package com.example.util

import android.content.Context
import com.example.data.ParkingTicketDao
import com.example.network.ParkSathiNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class CloudSyncEngine(
    private val parkingTicketDao: ParkingTicketDao,
    private val context: Context? = null
) {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _syncStatusLog = MutableStateFlow("SaaS Cloud Sync Ready (Multi-Gate Active)")
    val syncStatusLog: StateFlow<String> = _syncStatusLog.asStateFlow()

    suspend fun performSync(): SyncResult {
        if (_isSyncing.value) return SyncResult.AlreadyRunning
        _isSyncing.value = true
        _syncStatusLog.value = "Connecting to SaaS Cloud Gateway (POST /api/v1/ird/cbms/sync)..."

        return withContext(Dispatchers.IO) {
            try {
                val unsynced = parkingTicketDao.getUnsyncedTickets()
                if (unsynced.isEmpty()) {
                    _syncStatusLog.value = "All records up to date with Prajna World SaaS Cloud!"
                    _lastSyncTime.value = System.currentTimeMillis()
                    _isSyncing.value = false
                    return@withContext SyncResult.Success(0)
                }

                _syncStatusLog.value = "Uploading ${unsynced.size} tax invoices to IRD CBMS Server..."
                
                val deviceId = if (context != null) ParkSathiNetworkClient.getDeviceId(context) else "SUNMI-HWID-9912"
                val syncedIds = mutableListOf<String>()

                for (ticket in unsynced) {
                    val totalNpr = ticket.totalAmount ?: 0.0
                    val vatNpr = ticket.vatAmount ?: (totalNpr * 0.13 / 1.13)
                    val netNpr = ticket.netAmount ?: (totalNpr - vatNpr)
                    val billNo = ticket.irdInvoiceNo ?: "PS-8283-${ticket.ticketId.take(5)}"

                    val response = ParkSathiNetworkClient.safeSyncIrdCbms(
                        fiscalYear = "2082/83",
                        billNo = billNo,
                        customerPan = "609874123",
                        totalAmountNpr = totalNpr,
                        taxableAmountNpr = netNpr,
                        vatAmountNpr = vatNpr,
                        paymentMethod = ticket.paymentMethod ?: "CASH",
                        deviceId = deviceId
                    )

                    if (response.success) {
                        syncedIds.add(ticket.ticketId)
                    }
                }

                if (syncedIds.isNotEmpty()) {
                    parkingTicketDao.markTicketsSynced(syncedIds)
                }

                _lastSyncTime.value = System.currentTimeMillis()
                _syncStatusLog.value = "Successfully synced ${syncedIds.size} invoices to IRD CBMS SaaS!"
                _isSyncing.value = false
                SyncResult.Success(syncedIds.size)
            } catch (e: Exception) {
                _syncStatusLog.value = "Sync Error: ${e.localizedMessage}"
                _isSyncing.value = false
                SyncResult.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        data class Error(val message: String) : SyncResult()
        object AlreadyRunning : SyncResult()
    }
}

package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.network.ParkSathiNetworkClient
import java.util.concurrent.TimeUnit

class IrdSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "IrdSyncWorker"
        const val WORK_NAME = "ird_cbms_batch_sync_worker"

        /**
         * Schedules periodic background sync every 15 minutes when connected to network
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<IrdSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d(TAG, "Scheduled 15-min periodic IRD CBMS batch sync worker.")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.parkingTicketDao()
            val unsynced = dao.getUnsyncedTickets()

            if (unsynced.isEmpty()) {
                Log.d(TAG, "No unsynced IRD invoices found.")
                return Result.success()
            }

            Log.d(TAG, "Found ${unsynced.size} unsynced tickets. Uploading to IRD CBMS SaaS API...")

            val deviceId = ParkSathiNetworkClient.getDeviceId(applicationContext)
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
                dao.markTicketsSynced(syncedIds)
                Log.d(TAG, "Successfully synced ${syncedIds.size} tickets/invoices to IRD CBMS SaaS!")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "IrdSyncWorker failed: ${e.message}", e)
            Result.retry()
        }
    }
}

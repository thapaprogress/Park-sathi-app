package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parking_tickets",
    indices = [
        Index(value = ["vehicleNumber"]),
        Index(value = ["status"])
    ]
)
data class ParkingTicket(
    @PrimaryKey val ticketId: String,
    val vehicleNumber: String,
    val vehicleType: VehicleType,
    val checkInTime: Long,
    val checkOutTime: Long? = null,
    val ratePerHour: Double,
    val totalAmount: Double? = null,
    val status: TicketStatus = TicketStatus.ACTIVE,
    val attendantId: String,
    val hasCafeSeal: Boolean = false,
    val paymentMethod: String? = "CASH",
    val isSyncedWithCloud: Boolean = false,
    val irdInvoiceNo: String? = null,
    val vatAmount: Double? = 0.0,
    val netAmount: Double? = 0.0,
    val discountAmount: Double? = 0.0,
    val gateId: String = "GATE-01"
)

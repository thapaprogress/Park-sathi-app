package com.example.data

import com.example.util.NepalIrdInvoiceHelper
import com.example.util.NepalTariffConfig
import kotlinx.coroutines.flow.Flow
import kotlin.math.ceil

class ParkingTicketRepository(private val parkingTicketDao: ParkingTicketDao) {

    fun getActiveTicketsFlow(query: String): Flow<List<ParkingTicket>> {
        return if (query.isEmpty()) {
            parkingTicketDao.getActiveTicketsFlow()
        } else {
            parkingTicketDao.getActiveTicketsByVehicleNumberFlow(query)
        }
    }

    suspend fun getTicketById(ticketId: String): ParkingTicket? {
        return parkingTicketDao.getTicketById(ticketId)
    }

    suspend fun insert(ticket: ParkingTicket) {
        parkingTicketDao.insertTicket(ticket)
    }

    suspend fun checkoutTicket(
        ticketId: String,
        checkOutTime: Long,
        hasCafeSeal: Boolean = false,
        paymentMethod: String = "CASH",
        tariffConfig: NepalTariffConfig = NepalTariffConfig()
    ): ParkingTicket? {
        val ticket = parkingTicketDao.getTicketById(ticketId) ?: return null
        if (ticket.status == TicketStatus.COMPLETED) return ticket

        val calc = tariffConfig.calculateParkingFee(
            checkInTimeMillis = ticket.checkInTime,
            checkOutTimeMillis = checkOutTime,
            hourlyRate = ticket.ratePerHour,
            hasCafeSeal = hasCafeSeal
        )

        val invNo = NepalIrdInvoiceHelper.generateIrdInvoiceNumber(ticket.ticketId, checkOutTime)

        val updatedTicket = ticket.copy(
            checkOutTime = checkOutTime,
            totalAmount = calc.finalTotal,
            vatAmount = calc.vatAmount,
            netAmount = calc.netAmount,
            discountAmount = calc.discountAmount,
            hasCafeSeal = hasCafeSeal,
            paymentMethod = paymentMethod,
            irdInvoiceNo = invNo,
            status = TicketStatus.COMPLETED,
            isSyncedWithCloud = false
        )
        parkingTicketDao.updateTicket(updatedTicket)
        return updatedTicket
    }

    fun getDailySummary(startOfDay: Long): Flow<DailySummary> {
        return parkingTicketDao.getDailySummaryFlow(startOfDay)
    }

    fun getAllTickets(): Flow<List<ParkingTicket>> {
        return parkingTicketDao.getAllTicketsFlow()
    }

    fun getCompletedTickets(): Flow<List<ParkingTicket>> {
        return parkingTicketDao.getCompletedTicketsFlow()
    }
}

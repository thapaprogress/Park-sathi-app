package com.example.util

import com.example.data.ParkingTicket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NepalIrdInvoiceHelper {

    // Default IRD PAN & Operator details for Nepal market
    const val DEFAULT_PAN_NUMBER = "609874123"
    const val DEFAULT_COMPANY_NAME = "Utpala Parking Services Pvt. Ltd."
    const val DEFAULT_OPERATOR_ADDRESS = "Bouddha, Kathmandu, Nepal"

    fun generateIrdInvoiceNumber(ticketId: String, checkOutTimeMillis: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val datePrefix = sdf.format(Date(checkOutTimeMillis))
        val shortHash = ticketId.takeLast(5).uppercase()
        return "IRD-$datePrefix-$shortHash"
    }

    fun formatNprCurrency(amount: Double?): String {
        val validAmount = amount ?: 0.0
        return "NPR ${String.format(Locale.US, "%.2f", validAmount)}"
    }

    fun generateCsvExportData(tickets: List<ParkingTicket>, panNumber: String = DEFAULT_PAN_NUMBER): String {
        val sb = StringBuilder()
        sb.append("IRD Fiscal Invoice Report - Tax Year 2081/82\n")
        sb.append("Company PAN: $panNumber\n")
        sb.append("Generated Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n\n")
        
        // Header
        sb.append("Invoice No,Ticket ID,Vehicle No,Vehicle Type,Check-In,Check-Out,Gross (NPR),Discount (NPR),Net Taxable (NPR),VAT 13% (NPR),Total (NPR),Payment Method,Gate ID,Cloud Sync\n")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        tickets.forEach { ticket ->
            val invNo = ticket.irdInvoiceNo ?: generateIrdInvoiceNumber(ticket.ticketId, ticket.checkOutTime ?: System.currentTimeMillis())
            val checkIn = sdf.format(Date(ticket.checkInTime))
            val checkOut = if (ticket.checkOutTime != null) sdf.format(Date(ticket.checkOutTime)) else "N/A"
            val total = ticket.totalAmount ?: 0.0
            val vat = ticket.vatAmount ?: (total * 0.13 / 1.13)
            val net = ticket.netAmount ?: (total - vat)
            val discount = ticket.discountAmount ?: 0.0
            val gross = net + vat + discount

            sb.append("${invNo},${ticket.ticketId},${ticket.vehicleNumber},${ticket.vehicleType.name},")
            sb.append("${checkIn},${checkOut},")
            sb.append("${String.format(Locale.US, "%.2f", gross)},${String.format(Locale.US, "%.2f", discount)},")
            sb.append("${String.format(Locale.US, "%.2f", net)},${String.format(Locale.US, "%.2f", vat)},")
            sb.append("${String.format(Locale.US, "%.2f", total)},${ticket.paymentMethod ?: "CASH"},")
            sb.append("${ticket.gateId},${if (ticket.isSyncedWithCloud) "SYNCED" else "PENDING"}\n")
        }

        return sb.toString()
    }
}

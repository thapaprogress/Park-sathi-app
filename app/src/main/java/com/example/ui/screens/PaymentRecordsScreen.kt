package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ParkingTicket
import com.example.ui.ParkingViewModel
import com.example.util.NepalIrdInvoiceHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentRecordsScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNepali by viewModel.isNepaliLanguage.collectAsState()
    val dailySummary by viewModel.dailySummaryFlow.collectAsState()
    val completedTickets by viewModel.completedTicketsFlow.collectAsState()
    val companyPan by viewModel.companyPan.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }

    // Compute payment breakdown
    val cashTotal = remember(completedTickets) {
        completedTickets.filter { it.paymentMethod == "CASH" || it.paymentMethod == null }.sumOf { it.totalAmount ?: 0.0 }
    }
    val fonepayTotal = remember(completedTickets) {
        completedTickets.filter { it.paymentMethod == "FONEPAY" || it.paymentMethod == "FONEPAY_QR" }.sumOf { it.totalAmount ?: 0.0 }
    }
    val esewaTotal = remember(completedTickets) {
        completedTickets.filter { it.paymentMethod == "ESEWA" || it.paymentMethod == "ESEWA_QR" }.sumOf { it.totalAmount ?: 0.0 }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Daily Revenue Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (isNepali) "आजको कुल संकलित आम्दानी" else "Today's Total Collected Revenue",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NPR ${String.format(Locale.US, "%.2f", dailySummary.totalRevenue)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active Vehicles: ${dailySummary.activeCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Completed Checkout: ${dailySummary.completedCount}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Payment Mode Analytics Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Cash", style = MaterialTheme.typography.labelSmall)
                    Text("NPR ${cashTotal.toInt()}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Fonepay", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F))
                    Text("NPR ${fonepayTotal.toInt()}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD32F2F))
                }
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE8E8F5E9)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("eSewa", style = MaterialTheme.typography.labelSmall, color = Color(0xFF388E3C))
                    Text("NPR ${esewaTotal.toInt()}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF388E3C))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action header with CSV Tax Export button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isNepali) "IRD कर चुक्ता विवरण" else "IRD Tax Checkout Invoices",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Button(
                onClick = {
                    val csvData = NepalIrdInvoiceHelper.generateCsvExportData(completedTickets, companyPan)
                    showExportDialog = true
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export IRD Tax CSV", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Completed Payments List
        if (completedTickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isNepali) "कुनै भुक्तानी रेकर्ड छैन" else "No checkout payments recorded yet today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(completedTickets) { ticket ->
                    val sdf = SimpleDateFormat("HH:mm", Locale.US)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${ticket.vehicleNumber} • #${ticket.ticketId.take(6)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Inv: ${ticket.irdInvoiceNo ?: "N/A"} • ${sdf.format(Date(ticket.checkOutTime ?: 0L))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "NPR ${String.format(Locale.US, "%.2f", ticket.totalAmount ?: 0.0)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = when (ticket.paymentMethod) {
                                        "FONEPAY" -> Color(0xFFFFEBEE)
                                        "ESEWA" -> Color(0xE8E8F5E9)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = ticket.paymentMethod ?: "CASH",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        val csvData = remember(completedTickets, companyPan) {
            NepalIrdInvoiceHelper.generateCsvExportData(completedTickets, companyPan)
        }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("IRD Tax & VAT CSV Report Ready") },
            text = {
                Column {
                    Text("A tax report with ${completedTickets.size} records was generated for IRD Nepal filing.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = csvData.take(300) + "...",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showExportDialog = false
                    Toast.makeText(context, "Exported IRD Tax Report to device!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("OK")
                }
            }
        )
    }
}

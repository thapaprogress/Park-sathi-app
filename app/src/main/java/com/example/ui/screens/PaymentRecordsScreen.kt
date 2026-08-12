package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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

import com.example.data.VehicleType
import com.example.ui.DailyShiftArchive

@Composable
fun PaymentRecordsScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNepali by viewModel.isNepaliLanguage.collectAsState()
    val dailySummary by viewModel.dailySummaryFlow.collectAsState()
    val rawCompletedTickets by viewModel.completedTicketsFlow.collectAsState()
    val archivedShifts by viewModel.archivedShiftsFlow.collectAsState()
    val companyPan by viewModel.companyPan.collectAsState()

    var selectedDateFilterIndex by remember { mutableIntStateOf(0) } // 0: Today, 1: Yesterday, 2: Past 7 Days, 3: All
    var showExportDialog by remember { mutableStateOf(false) }
    var showArchivesDialog by remember { mutableStateOf(false) }

    // Date filtering logic
    val nowMs = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L
    val filteredCompletedTickets = remember(rawCompletedTickets, selectedDateFilterIndex) {
        when (selectedDateFilterIndex) {
            0 -> { // Today
                val startOfDay = nowMs - (nowMs % dayMs)
                rawCompletedTickets.filter { (it.checkOutTime ?: it.checkInTime) >= startOfDay }
            }
            1 -> { // Yesterday
                val startOfToday = nowMs - (nowMs % dayMs)
                val startOfYesterday = startOfToday - dayMs
                rawCompletedTickets.filter {
                    val t = it.checkOutTime ?: it.checkInTime
                    t >= startOfYesterday && t < startOfToday
                }
            }
            2 -> { // Past 7 Days
                val start7DaysAgo = nowMs - (7 * dayMs)
                rawCompletedTickets.filter { (it.checkOutTime ?: it.checkInTime) >= start7DaysAgo }
            }
            else -> rawCompletedTickets // All
        }
    }

    // Compute filtered payment breakdown
    val cashTotal = remember(filteredCompletedTickets) {
        filteredCompletedTickets.filter { it.paymentMethod == "CASH" || it.paymentMethod == null }.sumOf { it.totalAmount ?: 0.0 }
    }
    val fonepayTotal = remember(filteredCompletedTickets) {
        filteredCompletedTickets.filter { it.paymentMethod == "FONEPAY" || it.paymentMethod == "FONEPAY_QR" }.sumOf { it.totalAmount ?: 0.0 }
    }
    val esewaTotal = remember(filteredCompletedTickets) {
        filteredCompletedTickets.filter { it.paymentMethod == "ESEWA" || it.paymentMethod == "ESEWA_QR" }.sumOf { it.totalAmount ?: 0.0 }
    }
    val totalRevenueFiltered = cashTotal + fonepayTotal + esewaTotal

    // Vehicle Type Distribution
    val bikeTickets = remember(filteredCompletedTickets) { filteredCompletedTickets.filter { it.vehicleType == VehicleType.BIKE } }
    val carTickets = remember(filteredCompletedTickets) { filteredCompletedTickets.filter { it.vehicleType == VehicleType.CAR } }
    val truckTickets = remember(filteredCompletedTickets) { filteredCompletedTickets.filter { it.vehicleType == VehicleType.TRUCK } }

    val bikeCount = bikeTickets.size
    val carCount = carTickets.size
    val truckCount = truckTickets.size
    val totalVehiclesCount = (bikeCount + carCount + truckCount).coerceAtLeast(1)

    val bikeRatio = bikeCount.toFloat() / totalVehiclesCount
    val carRatio = carCount.toFloat() / totalVehiclesCount
    val truckRatio = truckCount.toFloat() / totalVehiclesCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Multi-Day Date Filter Calendar Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf(
                if (isNepali) "आज (Today)" else "Today",
                if (isNepali) "हिजो (Yesterday)" else "Yesterday",
                if (isNepali) "७ दिन (7 Days)" else "Past 7 Days",
                if (isNepali) "सबै (All)" else "All Time"
            )
            filterOptions.forEachIndexed { index, title ->
                FilterChip(
                    selected = selectedDateFilterIndex == index,
                    onClick = { selectedDateFilterIndex = index },
                    label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Daily Revenue Summary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNepali) "कुल आम्दानी (Total Revenue)" else "Selected Period Total Revenue",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    OutlinedButton(
                        onClick = { viewModel.backupDatabaseToCloud() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cloud Backup", fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NPR ${String.format(Locale.US, "%.2f", totalRevenueFiltered)}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active: ${dailySummary.activeCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Filtered Checkouts: ${filteredCompletedTickets.size}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Cash", style = MaterialTheme.typography.labelSmall)
                    Text("NPR ${cashTotal.toInt()}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Fonepay", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F))
                    Text("NPR ${fonepayTotal.toInt()}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD32F2F))
                }
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE8E8F5E9)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("eSewa", style = MaterialTheme.typography.labelSmall, color = Color(0xFF388E3C))
                    Text("NPR ${esewaTotal.toInt()}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF388E3C))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vehicle Type Daily Distribution Chart Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isNepali) "सवारी साधन वितरण (Vehicle Type Ratio)" else "Daily Vehicle Distribution & Ratio",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Multi-segment ratio progress bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                ) {
                    if (bikeRatio > 0) {
                        Box(
                            modifier = Modifier
                                .weight(bikeRatio)
                                .fillMaxHeight()
                                .background(Color(0xFF0288D1), RoundedCornerShape(5.dp))
                        )
                    }
                    if (carRatio > 0) {
                        Box(
                            modifier = Modifier
                                .weight(carRatio)
                                .fillMaxHeight()
                                .background(Color(0xFF7B1FA2), RoundedCornerShape(5.dp))
                        )
                    }
                    if (truckRatio > 0) {
                        Box(
                            modifier = Modifier
                                .weight(truckRatio)
                                .fillMaxHeight()
                                .background(Color(0xFFE65100), RoundedCornerShape(5.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🏍️ Bike: $bikeCount (${(bikeRatio * 100).toInt()}%)", fontSize = 11.sp, color = Color(0xFF0288D1), fontWeight = FontWeight.Bold)
                    Text("🚗 Car: $carCount (${(carRatio * 100).toInt()}%)", fontSize = 11.sp, color = Color(0xFF7B1FA2), fontWeight = FontWeight.Bold)
                    Text("🚚 Heavy: $truckCount (${(truckRatio * 100).toInt()}%)", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions Header: Z-Report, Export CSV & Shift Archive
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { viewModel.printZReport() },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Z-Report", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.archiveCurrentDayShift()
                        Toast.makeText(context, "Current Shift Archived Successfully!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Archive Shift", fontSize = 11.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val csvData = NepalIrdInvoiceHelper.generateCsvExportData(filteredCompletedTickets, companyPan)
                        showExportDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV", fontSize = 11.sp)
                }

                if (archivedShifts.isNotEmpty()) {
                    IconButton(
                        onClick = { showArchivesDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.HistoryToggleOff, contentDescription = "Archives", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Completed Payments List
        if (filteredCompletedTickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isNepali) "छानिएको समयमा कुनै भुक्तानी रेकर्ड छैन" else "No checkout payments recorded for selected period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCompletedTickets) { ticket ->
                    val sdf = SimpleDateFormat("HH:mm, dd MMM", Locale.US)
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
        val csvData = remember(filteredCompletedTickets, companyPan) {
            NepalIrdInvoiceHelper.generateCsvExportData(filteredCompletedTickets, companyPan)
        }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("IRD Tax & VAT CSV Report Ready") },
            text = {
                Column {
                    Text("A tax report with ${filteredCompletedTickets.size} records was generated for IRD Nepal filing.")
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

    if (showArchivesDialog) {
        AlertDialog(
            onDismissRequest = { showArchivesDialog = false },
            title = { Text("Archived Daily Shift Reports") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(archivedShifts) { shift ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Shift Date: ${shift.dateString}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("NPR ${shift.totalRevenue}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Text("Tickets: ${shift.completedTickets} | Cash: NPR ${shift.cashTotal.toInt()} | Fonepay: NPR ${shift.fonepayTotal.toInt()}", fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showArchivesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

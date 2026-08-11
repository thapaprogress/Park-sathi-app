package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ParkingTicket
import com.example.data.TicketStatus
import com.example.data.VehicleType
import com.example.ui.ParkingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    val isNepali by viewModel.isNepaliLanguage.collectAsState()
    val allTickets by viewModel.allTicketsFlow.collectAsState()
    val showOnlyCurrentSession by viewModel.showOnlyCurrentSession.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Filter Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isNepali) "सवारी साधन रेकर्ड सूची (${allTickets.size})" else "Parking Records (${allTickets.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isNepali) "आजको सेसन मात्र" else "24h Session Only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                FilterChip(
                    selected = showOnlyCurrentSession,
                    onClick = { viewModel.setShowOnlyCurrentSession(!showOnlyCurrentSession) },
                    label = { Text(if (showOnlyCurrentSession) "Active Shift" else "All History") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showOnlyCurrentSession) Icons.Default.Today else Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (allTickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isNepali) "कुनै रेकर्ड उपलब्ध छैन" else "No tickets logged for selected filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allTickets) { ticket ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (ticket.vehicleType) {
                                            VehicleType.BIKE -> Icons.Default.TwoWheeler
                                            VehicleType.CAR -> Icons.Default.DirectionsCar
                                            VehicleType.TRUCK -> Icons.Default.LocalShipping
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = ticket.vehicleNumber,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Surface(
                                    color = if (ticket.status == TicketStatus.ACTIVE) Color(0xFF1976D2) else Color(0xFF388E3C),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = ticket.status.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "In: ${sdf.format(Date(ticket.checkInTime))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (ticket.checkOutTime != null) "Out: ${sdf.format(Date(ticket.checkOutTime))}" else "Parked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (ticket.status == TicketStatus.COMPLETED) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Inv: ${ticket.irdInvoiceNo ?: "N/A"} (${ticket.paymentMethod ?: "CASH"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Paid: NPR ${String.format(Locale.US, "%.2f", ticket.totalAmount ?: 0.0)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

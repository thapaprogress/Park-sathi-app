package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import com.example.data.ParkingTicket
import com.example.data.VehicleType
import com.example.ui.ParkingViewModel
import com.example.util.FonepayEsewaQrGenerator.PaymentGateway
import com.example.util.NepalIrdInvoiceHelper
import com.example.util.NepalTariffConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CheckoutScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    val isNepali by viewModel.isNepaliLanguage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResultsFlow.collectAsState()
    val selectedTicket by viewModel.selectedCheckoutTicket.collectAsState()
    val isScanActive by viewModel.isScanActive.collectAsState()
    val statusMessage by viewModel.printStatusMessage.collectAsState()

    val freeMinutes by viewModel.freeMinutes.collectAsState()
    val graceMinutes by viewModel.graceMinutes.collectAsState()
    val dailyCap by viewModel.dailyMaxCap.collectAsState()

    val companyName by viewModel.companyName.collectAsState()
    val companyPan by viewModel.companyPan.collectAsState()

    var activePaymentModalGateway by remember { mutableStateOf<PaymentGateway?>(null) }
    var hasCafeSealState by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Camera QR Scanner Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text(if (isNepali) "टिकट ID वा नम्बर प्लेट खोज्नुहोस्" else "Search Ticket ID or Plate No") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    if (isScanActive) viewModel.stopScanning() else viewModel.startScanning()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = if (isScanActive) Icons.Default.Close else Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR"
                )
            }
        }

        // Camera Scanner Viewfinder Modal / Card
        if (isScanActive) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    com.example.ui.components.CameraXScannerPreview(
                        scanMode = com.example.ui.components.CameraScanMode.QR_SCANNER,
                        onCodeDetected = { scannedData ->
                            viewModel.handleScannedQrCode(scannedData)
                            viewModel.stopScanning()
                        }
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(160.dp)
                            .border(2.dp, Color(0xFF00E676), RoundedCornerShape(12.dp))
                    )
                    Text(
                        text = if (isNepali) "टिकट क्यूआर कोड बक्समा मिलाउनुहोस्" else "Align Ticket QR Code in Green Frame",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!statusMessage.isNullOrEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = statusMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Selected Checkout Ticket Calculation Panel
        if (selectedTicket != null) {
            val ticket = selectedTicket!!
            val tariffConfig = remember(freeMinutes, graceMinutes, dailyCap) {
                NepalTariffConfig(
                    freeMinutes = freeMinutes,
                    gracePeriodMinutes = graceMinutes,
                    dailyMaxCap = dailyCap.toDouble()
                )
            }

            val calcResult = remember(ticket, hasCafeSealState, tariffConfig) {
                tariffConfig.calculateParkingFee(
                    checkInTimeMillis = ticket.checkInTime,
                    checkOutTimeMillis = System.currentTimeMillis(),
                    hourlyRate = ticket.ratePerHour,
                    hasCafeSeal = hasCafeSealState
                )
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TICKET #${ticket.ticketId.take(10)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Vehicle: ${ticket.vehicleNumber} (${ticket.vehicleType.name})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.selectCheckoutTicket(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Deselect")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    val sdf = SimpleDateFormat("HH:mm, dd MMM", Locale.US)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Check-In: ${sdf.format(Date(ticket.checkInTime))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Duration: ${calcResult.durationMinutes} mins",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tariff & Tax breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rate: NPR ${ticket.ratePerHour}/hr", style = MaterialTheme.typography.bodySmall)
                        if (calcResult.isFreeTierApplied) {
                            Text(
                                "FREE ($freeMinutes mins tier)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text("Billable: ${calcResult.billableHours} hrs", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Staff Seal Switch (100% Waived Pass)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isNepali) "स्टाफ सिल (नि:शुल्क निस्कने Pass)" else "Staff Seal (100% Waived Pass)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = hasCafeSealState,
                            onCheckedChange = { hasCafeSealState = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // IRD 13% VAT & Net Taxable display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net Taxable:", style = MaterialTheme.typography.bodySmall)
                        Text("NPR ${calcResult.netAmount}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("13% IRD VAT:", style = MaterialTheme.typography.bodySmall)
                        Text("NPR ${calcResult.vatAmount}", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNepali) "कुल भुक्तानी रकम:" else "Total Payable:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "NPR ${calcResult.finalTotal}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment Mode Selection Options
                    Text(
                        text = if (isNepali) "भुक्तानी विधि छान्नुहोस्:" else "Select Payment Mode:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.checkoutTicket(hasCafeSeal = hasCafeSealState, paymentMethod = "CASH")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cash")
                        }

                        Button(
                            onClick = { activePaymentModalGateway = PaymentGateway.FONEPAY },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fonepay")
                        }

                        Button(
                            onClick = { activePaymentModalGateway = PaymentGateway.ESEWA },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("eSewa")
                        }
                    }
                }
            }
        } else {
            // Search Results Active Tickets List
            Text(
                text = if (searchQuery.isEmpty()) {
                    if (isNepali) "हाल पार्किङमा रहेका सवारीहरू (${searchResults.size})" else "Active Parked Vehicles (${searchResults.size})"
                } else {
                    if (isNepali) "खोज नतिजाहरू (${searchResults.size})" else "Matching Search Results (${searchResults.size})"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isNepali) "कुनै सक्रिय पार्किङ भेटिएन" else "No active tickets match your search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { ticket ->
                        val sdf = SimpleDateFormat("HH:mm", Locale.US)
                        Card(
                            onClick = { viewModel.selectCheckoutTicket(ticket) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
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
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = ticket.vehicleNumber,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "In: ${sdf.format(Date(ticket.checkInTime))} • ID: #${ticket.ticketId.take(6)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = { viewModel.selectCheckoutTicket(ticket) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Checkout")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal popup for Fonepay / eSewa Payment Confirmation
    if (activePaymentModalGateway != null && selectedTicket != null) {
        val gateway = activePaymentModalGateway!!
        val ticket = selectedTicket!!
        val calc = NepalTariffConfig(freeMinutes, graceMinutes, dailyCap.toDouble()).calculateParkingFee(
            checkInTimeMillis = ticket.checkInTime,
            checkOutTimeMillis = System.currentTimeMillis(),
            hourlyRate = ticket.ratePerHour,
            hasCafeSeal = hasCafeSealState
        )

        FonepayEsewaPaymentDialog(
            ticket = ticket,
            gateway = gateway,
            amountNpr = calc.finalTotal,
            companyName = companyName,
            companyPan = companyPan,
            onDismiss = { activePaymentModalGateway = null },
            onPaymentConfirmed = { paidGateway ->
                activePaymentModalGateway = null
                viewModel.checkoutTicket(hasCafeSeal = hasCafeSealState, paymentMethod = paidGateway)
            }
        )
    }
}

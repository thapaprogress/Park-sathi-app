package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.VehicleType
import com.example.ui.ParkingViewModel
import com.example.util.NepalIrdInvoiceHelper
import kotlinx.coroutines.launch

@Composable
fun EntryScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    val isNepali by viewModel.isNepaliLanguage.collectAsState()
    val vehicleNumber by viewModel.vehicleNumber.collectAsState()
    val selectedType by viewModel.selectedVehicleType.collectAsState()
    val bikeRate by viewModel.bikeRate.collectAsState()
    val carRate by viewModel.carRate.collectAsState()
    val truckRate by viewModel.truckRate.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val statusMessage by viewModel.printStatusMessage.collectAsState()
    val freeMinutes by viewModel.freeMinutes.collectAsState()
    val subInfo by viewModel.subscriptionInfoFlow.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (subInfo.isExpired) {
            var showRenewalModal by remember { mutableStateOf(false) }

            Card(
                onClick = { showRenewalModal = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isNepali) "साास सब्सक्रिप्शन समाप्त भयो (नवीकरण आवश्यक)" else "License Expired - Renew via Fonepay QR",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (isNepali) "सम्पर्क: +977 9765985999 (प्रज्ञा वर्ल्ड हेल्पडेस्क)" else "Prajna World Support: +977 9765985999 • Tap to Renew",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = { showRenewalModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("RENEW", fontSize = 11.sp)
                    }
                }
            }

            if (showRenewalModal) {
                var modalSerialKey by remember { mutableStateOf("") }
                var isActivatingModal by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                AlertDialog(
                    onDismissRequest = { showRenewalModal = false },
                    title = {
                        Text("License Expired - Renew via Fonepay QR", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text(
                                "Your Park Sathi SaaS license has expired. Contact Prajna World Support or activate a new serial key.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Prajna World Hotline: +977 9765985999",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = modalSerialKey,
                                onValueChange = { modalSerialKey = it.uppercase() },
                                label = { Text("Serial Key (e.g. PARKSATHI-MONTHLY-2026)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (modalSerialKey.isNotEmpty()) {
                                    isActivatingModal = true
                                    scope.launch {
                                        val (success, msg) = viewModel.activateSaaSKeyOnline(modalSerialKey)
                                        Toast.makeText(viewModel.getApplication(), msg, Toast.LENGTH_LONG).show()
                                        isActivatingModal = false
                                        if (success) showRenewalModal = false
                                    }
                                }
                            },
                            enabled = modalSerialKey.isNotEmpty() && !isActivatingModal
                        ) {
                            Text(if (isActivatingModal) "Verifying..." else "Activate Serial Key")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showRenewalModal = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
        // Top Banner / Tariff summary
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isNepali) "नयाँ प्रवेश टिकट जारी" else "Issue Vehicle Entry Ticket",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isNepali) "पहिलो $freeMinutes मिनेट नि:शुल्क • १३% भ्याट समावेश" else "First $freeMinutes mins free • 13% VAT Included",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status banner if present
        if (!statusMessage.isNullOrEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearStatusMessage() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Vehicle Type Selector
        Text(
            text = if (isNepali) "सवारी साधन छान्नुहोस्:" else "Select Vehicle Category:",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val types = listOf(
                VehicleType.BIKE to (if (isNepali) "बाईक / स्कुटर" else "Motorbike") to bikeRate,
                VehicleType.CAR to (if (isNepali) "कार / जीप" else "Car / Jeep") to carRate,
                VehicleType.TRUCK to (if (isNepali) "बस / ट्रक" else "Truck / Bus") to truckRate
            )

            types.forEach { (typeInfo, rate) ->
                val (type, name) = typeInfo
                val isSelected = selectedType == type

                Card(
                    onClick = { viewModel.updateVehicleType(type) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (type) {
                                VehicleType.BIKE -> Icons.Default.TwoWheeler
                                VehicleType.CAR -> Icons.Default.DirectionsCar
                                VehicleType.TRUCK -> Icons.Default.LocalShipping
                            },
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "NPR $rate/hr",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Vehicle Plate Number Field
        var showPlateCameraModal by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = vehicleNumber,
                onValueChange = { viewModel.updateVehicleNumber(it) },
                label = { Text(if (isNepali) "सवारी नम्बर (उदा: BA 2 PA 8842)" else "Vehicle Number Plate (e.g. BA 2 PA 8842)") },
                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                trailingIcon = {
                    if (vehicleNumber.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateVehicleNumber("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { showPlateCameraModal = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Scan Plate")
                Spacer(modifier = Modifier.width(4.dp))
                Text("ANPR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showPlateCameraModal) {
            com.example.ui.components.CameraXScannerModal(
                scanMode = com.example.ui.components.CameraScanMode.PLATE_RECOGNIZER,
                onDismiss = { showPlateCameraModal = false },
                onCodeDetected = { plateText ->
                    viewModel.updateVehicleNumberFromScan(plateText)
                    showPlateCameraModal = false
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Nepal License Plate Quick Shortcuts
        Text(
            text = if (isNepali) "द्रुत नम्बर सिफारिस:" else "Quick License Plate Helpers:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val prefixes = listOf("BA 2 PA", "BAGMATI", "PRO-3", "LU 1 PA")
            prefixes.forEach { prefix ->
                AssistChip(
                    onClick = {
                        val randomDigits = (1000..9999).random()
                        viewModel.updateVehicleNumber("$prefix $randomDigits")
                    },
                    label = { Text(prefix, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Issue Ticket Action Button
        Button(
            onClick = { viewModel.issueTicket() },
            enabled = vehicleNumber.length >= 3 && !isPrinting && !subInfo.isExpired,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isPrinting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(if (isNepali) "प्रिन्ट हुँदैछ..." else "Printing POS Receipt...")
            } else {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isNepali) "प्रवेश पास प्रिन्ट गर्नुहोस्" else "PRINT ENTRY TICKET",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

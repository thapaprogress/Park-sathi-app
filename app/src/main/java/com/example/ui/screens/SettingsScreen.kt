package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.ParkingViewModel

@Composable
fun SettingsScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    val isNepali by viewModel.isNepaliLanguage.collectAsState()
    val bikeRate by viewModel.bikeRate.collectAsState()
    val carRate by viewModel.carRate.collectAsState()
    val truckRate by viewModel.truckRate.collectAsState()

    val freeMinutes by viewModel.freeMinutes.collectAsState()
    val graceMinutes by viewModel.graceMinutes.collectAsState()
    val dailyCap by viewModel.dailyMaxCap.collectAsState()

    val companyName by viewModel.companyName.collectAsState()
    val companyPan by viewModel.companyPan.collectAsState()
    val gateId by viewModel.gateId.collectAsState()

    val useBluetooth by viewModel.useBluetooth.collectAsState()
    val btMac by viewModel.bluetoothMacAddress.collectAsState()
    val footerText by viewModel.printFooterText.collectAsState()

    var bikeText by remember(bikeRate) { mutableStateOf(bikeRate.toString()) }
    var carText by remember(carRate) { mutableStateOf(carRate.toString()) }
    var truckText by remember(truckRate) { mutableStateOf(truckRate.toString()) }

    var freeMinsText by remember(freeMinutes) { mutableStateOf(freeMinutes.toString()) }
    var graceMinsText by remember(graceMinutes) { mutableStateOf(graceMinutes.toString()) }
    var dailyCapText by remember(dailyCap) { mutableStateOf(dailyCap.toString()) }

    var nameText by remember(companyName) { mutableStateOf(companyName) }
    var panText by remember(companyPan) { mutableStateOf(companyPan) }
    var gateText by remember(gateId) { mutableStateOf(gateId) }

    var footerInput by remember(footerText) { mutableStateOf(footerText) }
    var macInput by remember(btMac) { mutableStateOf(btMac) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Language Switch Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNepali) "भाषा चयन (Language Preference)" else "Language Preference",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isNepali) "नेपाली भाषा (Nepali Interface)" else "Nepali Interface")
                    LanguageToggleHeader(
                        isNepali = isNepali,
                        onLanguageToggle = { viewModel.setLanguageNepali(it) }
                    )
                }
            }
        }

        // Vehicle Hourly Tariff Rates
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNepali) "प्रति घण्टा पार्किङ दर (Hourly Tariff Rates)" else "Hourly Parking Rates (NPR)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bikeText,
                        onValueChange = { bikeText = it },
                        label = { Text("Bike") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carText,
                        onValueChange = { carText = it },
                        label = { Text("Car/Jeep") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = truckText,
                        onValueChange = { truckText = it },
                        label = { Text("Truck/Bus") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val b = bikeText.toFloatOrNull() ?: bikeRate
                        val c = carText.toFloatOrNull() ?: carRate
                        val t = truckText.toFloatOrNull() ?: truckRate
                        viewModel.updateRates(b, c, t)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Rates")
                }
            }
        }

        // Tariff Tiers & Grace Periods
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNepali) "ट्यारिफ संरचना नियम (Tariff Tiers & Caps)" else "Tariff Tiers & Grace Period",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = freeMinsText,
                        onValueChange = { freeMinsText = it },
                        label = { Text("Free Mins") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = graceMinsText,
                        onValueChange = { graceMinsText = it },
                        label = { Text("Grace Mins") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dailyCapText,
                        onValueChange = { dailyCapText = it },
                        label = { Text("Max Cap (NPR)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val fm = freeMinsText.toIntOrNull() ?: freeMinutes
                        val gm = graceMinsText.toIntOrNull() ?: graceMinutes
                        val cap = dailyCapText.toFloatOrNull() ?: dailyCap
                        viewModel.updateTariffConfig(fm, gm, cap)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Tariff Rules")
                }
            }
        }

        // IRD Nepal Fiscal & POS Gate Settings
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNepali) "आन्तरिक राजस्व विभाग (IRD PAN/VAT System)" else "IRD Nepal Fiscal Registration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = panText,
                        onValueChange = { panText = it },
                        label = { Text("PAN Number") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = gateText,
                        onValueChange = { gateText = it },
                        label = { Text("Gate Terminal ID") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.updateIrdDetails(nameText, panText, gateText)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Fiscal Details")
                }
            }
        }

        // POS Printer Connection Setup
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNepali) "प्रिन्टर तथा हार्डवेयर सेटअप" else "Hardware & POS Printer Setup",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Use External Bluetooth Printer")
                    Switch(
                        checked = useBluetooth,
                        onCheckedChange = { viewModel.setUseBluetooth(it) }
                    )
                }

                if (useBluetooth) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = macInput,
                        onValueChange = {
                            macInput = it
                            viewModel.updateBluetoothMacAddress(it)
                        },
                        label = { Text("Bluetooth Printer MAC Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = footerInput,
                    onValueChange = {
                        footerInput = it
                        viewModel.updatePrintFooterText(it)
                    },
                    label = { Text("Receipt Footer Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

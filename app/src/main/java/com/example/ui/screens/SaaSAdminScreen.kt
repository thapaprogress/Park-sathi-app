package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ParkingTicket
import com.example.data.VehicleType
import com.example.ui.ParkingViewModel
import com.example.util.FonepayEsewaQrGenerator.PaymentGateway
import com.example.util.SubscriptionPlan
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SaaSAdminScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNepali by viewModel.isNepaliLanguage.collectAsState()
    val unsyncedCount by viewModel.unsyncedCountFlow.collectAsState()
    val isSyncing by viewModel.syncEngine.isSyncing.collectAsState()
    val lastSyncTime by viewModel.syncEngine.lastSyncTime.collectAsState()
    val syncLog by viewModel.syncEngine.syncStatusLog.collectAsState()

    val gateId by viewModel.gateId.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val companyPan by viewModel.companyPan.collectAsState()

    val subInfo by viewModel.subscriptionInfoFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var isActivating by remember { mutableStateOf(false) }

    var licenseInput by remember { mutableStateOf("") }
    var selectedPlanForQrPayment by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var selectedGatewayForQrPayment by remember { mutableStateOf<PaymentGateway?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SaaS Facility Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SaaS Cloud Portal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Surface(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ONLINE GATE: $gateId",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = companyName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Multi-Gate Cloud Architecture • Nepal Region Server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // ==========================================
        // SAAS SUBSCRIPTION & TIME-LIMIT STATUS CARD
        // ==========================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (subInfo.isExpired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (subInfo.isExpired) Icons.Default.LockClock else Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (subInfo.isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isNepali) "साास सब्सक्रिप्शन स्थिति" else "SaaS License Plan",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (subInfo.isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = subInfo.plan.displayName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (subInfo.isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        color = if (subInfo.isExpired) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (subInfo.isExpired) "EXPIRED" else "${subInfo.daysRemaining} DAYS LEFT",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = (if (subInfo.isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active License Key:", style = MaterialTheme.typography.labelSmall)
                        Text(subInfo.licenseKey, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Expires On:", style = MaterialTheme.typography.labelSmall)
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        Text(sdf.format(Date(subInfo.expiresAtMillis)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (subInfo.isExpired) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⚠️ Ticket issuing is locked! Please activate a Monthly or Yearly license below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // LICENSE KEY SERIAL ACTIVATION PANEL
        // ==========================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNepali) "लाइसेन्स कुञ्जी सक्रिय गर्नुहोस्" else "Activate License Serial Key",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter subscription activation code or choose a demo key below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = licenseInput,
                    onValueChange = { licenseInput = it.uppercase() },
                    label = { Text("Serial Key (e.g., PARKSATHI-MONTHLY-2026)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        if (licenseInput.isNotEmpty()) {
                            IconButton(onClick = { licenseInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Demo Keys Helper Chips
                Text("Quick Demo Activation Keys:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { licenseInput = "PARKSATHI-MONTHLY-2026" },
                        label = { Text("Monthly Pro", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    AssistChip(
                        onClick = { licenseInput = "PARKSATHI-YEARLY-2026" },
                        label = { Text("Yearly Enterprise", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    AssistChip(
                        onClick = { licenseInput = "PARKSATHI-TRIAL-14D" },
                        label = { Text("14D Trial", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (licenseInput.isNotEmpty() && !isActivating) {
                            isActivating = true
                            scope.launch {
                                val (success, msg) = viewModel.activateSaaSKeyOnline(licenseInput)
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) licenseInput = ""
                                isActivating = false
                            }
                        }
                    },
                    enabled = licenseInput.isNotEmpty() && !isActivating,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isActivating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VERIFYING WITH PRAJNA WORLD API...")
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ACTIVATE SUBSCRIPTION KEY")
                    }
                }
            }
        }

        // ==========================================
        // MONTHLY & YEARLY SUBSCRIPTION PLANS
        // ==========================================
        Text(
            text = if (isNepali) "सब्स्क्रिप्शन योजनाहरू (Monthly & Yearly)" else "SaaS Subscription Plans & Pricing",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Monthly Plan
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("MONTHLY PRO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("NPR 1,500", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("/ month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 30-Day POS Access", style = MaterialTheme.typography.bodySmall)
                    Text("• IRD VAT Receipts", style = MaterialTheme.typography.bodySmall)
                    Text("• Up to 5 Gate Terminals", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            selectedPlanForQrPayment = SubscriptionPlan.MONTHLY
                            selectedGatewayForQrPayment = PaymentGateway.FONEPAY
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Renew Monthly", fontSize = 12.sp)
                    }
                }
            }

            // Yearly Plan
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Surface(color = Color(0xFF2E7D32), shape = RoundedCornerShape(6.dp)) {
                        Text("SAVE NPR 3,000", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text("YEARLY ENTERPRISE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("NPR 15,000", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("/ year (2 Mins Free)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 365-Day POS Access", style = MaterialTheme.typography.bodySmall)
                    Text("• Unlimited Multi-Gate Sync", style = MaterialTheme.typography.bodySmall)
                    Text("• Priority Regional Support", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            selectedPlanForQrPayment = SubscriptionPlan.YEARLY
                            selectedGatewayForQrPayment = PaymentGateway.FONEPAY
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Renew Yearly", fontSize = 12.sp)
                    }
                }
            }
        }

        // Multi-Gate Cloud Sync Control Panel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNepali) "क्लाउड मल्टि-गेट सिंक" else "Multi-Gate Background Sync Engine",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pending Unsynced Local Tickets:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$unsyncedCount Records",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (unsyncedCount > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                    }

                    Button(
                        onClick = { viewModel.triggerCloudSync() },
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sync Gateway Log:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(
                        text = syncLog,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (lastSyncTime != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val sdf = SimpleDateFormat("HH:mm:ss, dd MMM", Locale.US)
                    Text(
                        text = "Last Successful Sync: ${sdf.format(Date(lastSyncTime!!))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Multi-Device Gate Sync Topology Status
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNepali) "सक्रिय POS गेट हरू" else "Active POS Gate Network",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                val gates = listOf(
                    Triple("GATE-01 (Main Entry POS)", "Online • Entry Scanner", Color(0xFF2E7D32)),
                    Triple("GATE-02 (Exit Cashier POS)", "Online • Payment Gate", Color(0xFF2E7D32)),
                    Triple("GATE-03 (Cafe Seal Validator)", "Idle • Multi-device", Color(0xFF1976D2))
                )

                gates.forEach { (name, desc, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dvr, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Surface(color = color, shape = RoundedCornerShape(6.dp)) {
                            Text("ACTIVE", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }

        // ==========================================
        // PHASE 2 HARDWARE & SHIFT OPERATIONS PANEL
        // ==========================================
        var emergencyReasonInput by remember { mutableStateOf("") }
        var showEmergencyModal by remember { mutableStateOf(false) }
        val activeOpId by viewModel.activeOperatorId.collectAsState()
        val isOpLoggedIn by viewModel.isOperatorLoggedIn.collectAsState()
        val barrierStatusMsg by viewModel.barrierRelayManager.lastActionMessage.collectAsState()
        val isBarrierOpen by viewModel.barrierRelayManager.isBarrierOpen.collectAsState()

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hardware & Shift Operations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        color = if (isOpLoggedIn) Color(0xFF1976D2) else MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isOpLoggedIn) "OPERATOR: $activeOpId" else "LOGGED OUT",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SUNMI Printer Status & Z-Report
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val status = viewModel.checkPrinterStatusMessage()
                            Toast.makeText(context, "Printer Status: $status", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check Paper", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.printZReport() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Z-Report", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Boom Barrier Relay Signal Controller
                Text(
                    text = "Boom Barrier Gate Relay:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = if (isBarrierOpen) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = barrierStatusMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBarrierOpen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showEmergencyModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MANUAL EMERGENCY BARRIER OPEN", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Emergency Barrier Open Modal
        if (showEmergencyModal) {
            AlertDialog(
                onDismissRequest = { showEmergencyModal = false },
                title = { Text("Emergency Manual Barrier Gate Open") },
                text = {
                    Column {
                        Text("Enter mandatory audit reason for triggering manual barrier override:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = emergencyReasonInput,
                            onValueChange = { emergencyReasonInput = it },
                            label = { Text("Reason (e.g. VIP Pass, Power Fault)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (emergencyReasonInput.isNotBlank()) {
                                viewModel.triggerEmergencyBarrierOpen(emergencyReasonInput)
                                showEmergencyModal = false
                                emergencyReasonInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("PULSE BARRIER OPEN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmergencyModal = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Modal popup for Fonepay / eSewa SaaS Renewal Payment
    if (selectedPlanForQrPayment != null && selectedGatewayForQrPayment != null) {
        val plan = selectedPlanForQrPayment!!
        val gateway = selectedGatewayForQrPayment!!
        val tempTicket = ParkingTicket(
            ticketId = "SAAS-${plan.name}-${System.currentTimeMillis().toString().takeLast(6)}",
            vehicleNumber = "SAAS_RENEWAL",
            vehicleType = VehicleType.CAR,
            checkInTime = System.currentTimeMillis(),
            ratePerHour = 0.0,
            attendantId = "SAAS-ADMIN"
        )

        FonepayEsewaPaymentDialog(
            ticket = tempTicket,
            gateway = gateway,
            amountNpr = plan.priceNpr.toDouble(),
            companyName = "Park Sathi SaaS Cloud Nepal",
            companyPan = "600987654",
            onDismiss = {
                selectedPlanForQrPayment = null
                selectedGatewayForQrPayment = null
            },
            onPaymentConfirmed = { paidGateway ->
                selectedPlanForQrPayment = null
                selectedGatewayForQrPayment = null
                val (success, msg) = viewModel.renewSaaSPlan(plan)
                Toast.makeText(context, "Payment Confirmed via $paidGateway! $msg", Toast.LENGTH_LONG).show()
            }
        )
    }
}


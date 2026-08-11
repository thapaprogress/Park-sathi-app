package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.example.data.ActivationRecord
import com.example.network.ParkSathiNetworkClient
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
    val activationRecords by viewModel.activationRecordsFlow.collectAsState()
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Offline Operation Grace:",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = if (subInfo.isGraceExpired) {
                            "⛔ GRACE EXPIRED (${subInfo.daysOffline}d offline)"
                        } else {
                            "🟢 ACTIVE (${subInfo.graceDaysRemaining}/${subInfo.maxOfflineGraceDays} Days Left)"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (subInfo.isGraceExpired) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                    )
                }

                if (subInfo.isExpired) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (subInfo.isGraceExpired) {
                            "⚠️ Offline Grace Expired (${subInfo.daysOffline} days without server check). Connect to Wi-Fi/4G to re-verify license."
                        } else {
                            "⚠️ Ticket issuing is locked! Please activate a Monthly or Yearly license below."
                        },
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

                Spacer(modifier = Modifier.height(8.dp))

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
        // SAAS WEBSITE DEMO ACTIVATION KEYS
        // ==========================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Activation Keys for SaaS Website Demo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "Instant Hardware Binding Serial Keys available for testing or activation:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Demo Key 1: 14-Day Free Trial
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("14-DAY FREE TRIAL KEY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("PARKSATHI-TRIAL-14D", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Instant test activation code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Button(
                            onClick = {
                                licenseInput = "PARKSATHI-TRIAL-14D"
                                scope.launch {
                                    val (success, msg) = viewModel.activateSaaSKeyOnline("PARKSATHI-TRIAL-14D")
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Activate Trial", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Monthly & Yearly Subscription Purchasing Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "MONTHLY & YEARLY SUBSCRIPTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "To activate a paid Monthly or Yearly plan, purchase an activation key via WhatsApp or our official website.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val deviceId = ParkSathiNetworkClient.getDeviceId(context)
                            
                            // WhatsApp Contact Button
                            Button(
                                onClick = {
                                    val message = "Hello ParkSathi Team, I would like to buy an Activation Key for my POS Terminal.\nDevice Hardware ID: $deviceId"
                                    val url = "https://api.whatsapp.com/send?phone=9779800000000&text=" + Uri.encode(message)
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Opening WhatsApp / Browser...", Toast.LENGTH_SHORT).show()
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://prajnaworld.com"))
                                        context.startActivity(webIntent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Contact WhatsApp", fontSize = 11.sp, color = Color.White)
                            }

                            // Website Link Button
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://prajnaworld.com"))
                                    context.startActivity(intent)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Visit Website", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // ROOM DATABASE ACTIVATION RECORDS LOG
        // ==========================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Room Database Activation Log (${activationRecords.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "Persistent local SQLite table storing hardware activations & serial key bindings:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (activationRecords.isEmpty()) {
                    Text(
                        text = "No activation history recorded in Room DB yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    activationRecords.take(5).forEach { record ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = record.licenseKey,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Surface(
                                        color = if (record.status == "ACTIVE") Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = record.status,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (record.status == "ACTIVE") Color(0xFF166534) else Color(0xFF991B1B),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Plan: ${record.planName}", style = MaterialTheme.typography.bodySmall)
                                    Text("Expires: ${sdf.format(Date(record.expiresAtMillis))}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("HW ID: ${record.deviceHardwareId.take(16)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
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
            val deviceId = ParkSathiNetworkClient.getDeviceId(context)

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
                            val msg = "Hi, I want to purchase a Monthly Pro Subscription (NPR 1,500) for my POS Device HW ID: $deviceId"
                            val url = "https://api.whatsapp.com/send?phone=9779800000000&text=" + Uri.encode(msg)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://prajnaworld.com"))
                                context.startActivity(webIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Buy via WhatsApp", fontSize = 11.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            selectedPlanForQrPayment = SubscriptionPlan.MONTHLY
                            selectedGatewayForQrPayment = PaymentGateway.FONEPAY
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pay Fonepay QR", fontSize = 11.sp)
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
                    Text("/ year (2 Months Free)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 365-Day POS Access", style = MaterialTheme.typography.bodySmall)
                    Text("• Unlimited Multi-Gate Sync", style = MaterialTheme.typography.bodySmall)
                    Text("• Priority Regional Support", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val msg = "Hi, I want to purchase a Yearly Enterprise Subscription (NPR 15,000) for my POS Device HW ID: $deviceId"
                            val url = "https://api.whatsapp.com/send?phone=9779800000000&text=" + Uri.encode(msg)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://prajnaworld.com"))
                                context.startActivity(webIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Buy via WhatsApp", fontSize = 11.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            selectedPlanForQrPayment = SubscriptionPlan.YEARLY
                            selectedGatewayForQrPayment = PaymentGateway.FONEPAY
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pay Fonepay QR", fontSize = 11.sp)
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


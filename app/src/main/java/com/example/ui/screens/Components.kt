package com.example.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.ParkingTicket
import com.example.network.ParkSathiNetworkClient
import com.example.ui.ParkingViewModel
import com.example.util.FonepayEsewaQrGenerator
import com.example.util.FonepayEsewaQrGenerator.PaymentGateway
import kotlinx.coroutines.delay

@Composable
fun LanguageToggleHeader(
    isNepali: Boolean,
    onLanguageToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = { onLanguageToggle(false) },
            shape = RoundedCornerShape(16.dp),
            color = if (!isNepali) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (!isNepali) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                text = "EN",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Surface(
            onClick = { onLanguageToggle(true) },
            shape = RoundedCornerShape(16.dp),
            color = if (isNepali) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isNepali) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                text = "नेपाली",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun CloudSyncHeaderBadge(
    unsyncedCount: Int,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onSyncClick,
        shape = RoundedCornerShape(20.dp),
        color = if (unsyncedCount > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (unsyncedCount > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = if (unsyncedCount > 0) Icons.Default.CloudUpload else Icons.Default.CloudDone,
                    contentDescription = "Cloud Sync",
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isSyncing) "Syncing..." else if (unsyncedCount > 0) "$unsyncedCount Pending" else "SaaS Synced",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
fun FonepayEsewaPaymentDialog(
    ticket: ParkingTicket,
    gateway: PaymentGateway,
    amountNpr: Double,
    companyName: String,
    companyPan: String,
    onDismiss: () -> Unit,
    onPaymentConfirmed: (String) -> Unit
) {
    var qrPayloadString by remember { mutableStateOf("") }
    var traceId by remember { mutableStateOf("") }
    var paymentStatusText by remember { mutableStateOf("Initiating Park Sathi Gateway...") }
    var isPolling by remember { mutableStateOf(true) }

    // Initiate payment via Park Sathi Cloud API
    LaunchedEffect(gateway, ticket, amountNpr) {
        val response = ParkSathiNetworkClient.safeInitiateFonepay(
            merchantId = companyName,
            amountNpr = amountNpr,
            ticketId = ticket.ticketId,
            vehicleNumber = ticket.vehicleNumber
        )
        qrPayloadString = response.qrPayload
        traceId = response.traceId
        paymentStatusText = "Scan QR with Fonepay / Banking App"

        // Poll status every 3 seconds
        while (isPolling && traceId.isNotEmpty()) {
            delay(3000)
            val statusResponse = ParkSathiNetworkClient.safeCheckFonepayStatus(traceId)
            if (statusResponse.status == "SUCCESS") {
                paymentStatusText = "Payment Verified! Completing Checkout..."
                delay(800)
                onPaymentConfirmed(gateway.name)
                break
            }
        }
    }

    val fallbackPayload = remember(gateway, ticket, amountNpr) {
        FonepayEsewaQrGenerator.createPaymentPayload(
            gateway = gateway,
            ticketId = ticket.ticketId,
            amountNpr = amountNpr,
            merchantName = companyName,
            merchantPan = companyPan
        )
    }

    val qrImageBitmap = remember(qrPayloadString) {
        val stringToRender = if (qrPayloadString.isNotEmpty()) qrPayloadString else fallbackPayload.qrPayloadString
        FonepayEsewaQrGenerator.generateQrBitmap(stringToRender, 450)
    }

    var isSimulatingPayment by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {
        isPolling = false
        onDismiss()
    }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header tag based on gateway
                val (gateName, gateColor) = when (gateway) {
                    PaymentGateway.FONEPAY -> "Fonepay Interoperable QR" to Color(0xFFD32F2F)
                    PaymentGateway.ESEWA -> "eSewa Direct Payment" to Color(0xFF388E3C)
                    PaymentGateway.KHALTI -> "Khalti Merchant QR" to Color(0xFF512DA8)
                    PaymentGateway.CASH -> "Cash Checkout" to MaterialTheme.colorScheme.primary
                }

                Surface(
                    color = gateColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = gateName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "NPR ${String.format("%.2f", amountNpr)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Ticket: #${ticket.ticketId.take(8)} • Vehicle: ${ticket.vehicleNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // QR Code Image
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrImageBitmap,
                        contentDescription = "Payment QR",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = gateColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = paymentStatusText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Merchant PAN: $companyPan • Trace: ${if (traceId.isNotEmpty()) traceId else "PENDING"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isPolling = false
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            isPolling = false
                            isSimulatingPayment = true
                            onPaymentConfirmed(gateway.name)
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = gateColor)
                    ) {
                        if (isSimulatingPayment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Paid")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(lifecycleOwner) {
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Throwable) {
                Log.e("CameraPreview", "Camera Viewfinder binding failed", e)
            }
        }, executor)
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Throwable) {
                Log.e("CameraPreview", "Failed to unbind camera on dispose", e)
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

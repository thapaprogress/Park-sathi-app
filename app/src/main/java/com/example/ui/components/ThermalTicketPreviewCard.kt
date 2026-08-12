package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleType
import com.example.util.FonepayEsewaQrGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThermalTicketPreviewCard(
    vehicleNumber: String,
    vehicleType: VehicleType,
    hourlyRate: Float,
    attendantId: String = "ATT-8842",
    gateId: String = "GATE-01",
    companyName: String = "UTPALA PARKING",
    companyPan: String = "609874123",
    footerText: String = "Scan to Checkout\nKeep ticket safe!",
    qrPrefix: String = "",
    isNepali: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayPlate = vehicleNumber.ifBlank { "BA 2 PA ____" }.uppercase()
    val mockTicketId = "PS-" + (displayPlate.hashCode().coerceAtLeast(100000) % 900000 + 100000)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val nowFormatted = sdf.format(Date())
    val qrPayload = qrPrefix + mockTicketId
    val qrBitmap = FonepayEsewaQrGenerator.generateQrBitmap(qrPayload, size = 320)

    val vehicleName = when (vehicleType) {
        VehicleType.BIKE -> if (isNepali) "मोटरसाइकल (BIKE)" else "MOTORBIKE"
        VehicleType.CAR -> if (isNepali) "कार / जिप (CAR/JEEP)" else "CAR / JEEP"
        VehicleType.TRUCK -> if (isNepali) "ट्रक / बस (TRUCK/BUS)" else "TRUCK / BUS"
    }

    Surface(
        modifier = modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp)),
        color = Color(0xFFFFFEFA), // Thermal paper light cream tint
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cut indicator line top
            Text(
                text = "✂ - - - - 58mm POS PAPER - - - - ✂",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Header - Bold Company Title
            Text(
                text = companyName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF111827),
                textAlign = TextAlign.Center
            )

            Text(
                text = "PAN: $companyPan • $gateId",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF4B5563),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Bouddha, Kathmandu, Nepal",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Dashed Divider
            Text(
                text = "--------------------------------",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF374151),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Receipt Key-Value Rows (58mm 32-column simulation)
            ReceiptRow(left = if (isNepali) "टिकट नम्बर:" else "Ticket ID:", right = mockTicketId)
            ReceiptRow(left = if (isNepali) "सवारी नम्बर:" else "Vehicle No:", right = displayPlate, isHighlighted = true)
            ReceiptRow(left = if (isNepali) "सवारी प्रकार:" else "Category:", right = vehicleName)
            ReceiptRow(left = if (isNepali) "घन्टाको दर:" else "Hourly Rate:", right = "NPR ${hourlyRate.toInt()}/hr")
            ReceiptRow(left = if (isNepali) "प्रवेश समय:" else "Check-In:", right = nowFormatted)
            ReceiptRow(left = if (isNepali) "गेट सम्हाल्ने:" else "Attendant:", right = attendantId)

            Spacer(modifier = Modifier.height(6.dp))

            // Dashed Divider
            Text(
                text = "--------------------------------",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF374151),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // QR Code Image
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "Ticket QR Code Preview",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Scan at Gate Exit for Auto Checkout",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4B5563),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Footer Custom Message
            footerText.split("\n").forEach { line ->
                Text(
                    text = line,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isNepali) "आन्तरिक राजस्व विभाग (IRD) कर प्रणाली अनुसार" else "IRD Fiscal Compliance App • 13% VAT Inc.",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )

            // Cut indicator line bottom
            Text(
                text = "✂ - - - - - - - - - - - - - - - ✂",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ReceiptRow(
    left: String,
    right: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = left,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF374151)
        )
        Text(
            text = right,
            fontSize = if (isHighlighted) 13.sp else 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.Bold,
            color = if (isHighlighted) Color(0xFF1E40AF) else Color(0xFF111827)
        )
    }
}

@Composable
fun TicketPreviewModalDialog(
    vehicleNumber: String,
    vehicleType: VehicleType,
    hourlyRate: Float,
    attendantId: String,
    gateId: String = "GATE-01",
    companyName: String = "UTPALA PARKING",
    companyPan: String = "609874123",
    footerText: String,
    qrPrefix: String,
    isNepali: Boolean,
    isPrinting: Boolean,
    onConfirmPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isNepali) "रसीद पूर्वअवलोकन (Ticket Preview)" else "Thermal Receipt Preview",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isNepali) "प्रिन्ट गर्नु अघि सवारी नम्बर र शुल्क विवरण जाँच गर्नुहोस्:" else "Verify thermal receipt layout and vehicle details before printing:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                ThermalTicketPreviewCard(
                    vehicleNumber = vehicleNumber,
                    vehicleType = vehicleType,
                    hourlyRate = hourlyRate,
                    attendantId = attendantId,
                    gateId = gateId,
                    companyName = companyName,
                    companyPan = companyPan,
                    footerText = footerText,
                    qrPrefix = qrPrefix,
                    isNepali = isNepali,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmPrint()
                    onDismiss()
                },
                enabled = !isPrinting && vehicleNumber.length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isNepali) "पुष्टि गरी प्रिन्ट गर्नुहोस्" else "CONFIRM & PRINT")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isNepali) "सच्याउनुहोस्" else "EDIT DETAILS")
            }
        }
    )
}

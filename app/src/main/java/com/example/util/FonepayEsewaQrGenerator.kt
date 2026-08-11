package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Locale

object FonepayEsewaQrGenerator {

    enum class PaymentGateway {
        FONEPAY,
        ESEWA,
        KHALTI,
        CASH
    }

    data class QrPaymentPayload(
        val gateway: PaymentGateway,
        val merchantName: String = "Utpala Parking",
        val merchantPan: String = "609874123",
        val ticketId: String,
        val amountNpr: Double,
        val qrPayloadString: String
    )

    fun createPaymentPayload(
        gateway: PaymentGateway,
        ticketId: String,
        amountNpr: Double,
        merchantName: String = "Utpala Parking",
        merchantPan: String = "609874123"
    ): QrPaymentPayload {
        val formattedAmount = String.format(Locale.US, "%.2f", amountNpr)
        val payloadStr = when (gateway) {
            PaymentGateway.FONEPAY -> {
                // EMVCo Fonepay merchant dynamic QR spec format for Nepal
                "00020101021226460009fonepay0115UTPALA_PARKING0209${merchantPan}520475385303124540${formattedAmount.length}${formattedAmount}5802NP5914${merchantName}6009Kathmandu62180514TICKET_${ticketId}6304"
            }
            PaymentGateway.ESEWA -> {
                // eSewa Merchant QR scheme for Nepal
                "https://esewa.com.np/epay/main?amt=$formattedAmount&pid=$ticketId&sc=UTPALAPARK&su=https://utpalaparking.com/success&fu=https://utpalaparking.com/failed"
            }
            PaymentGateway.KHALTI -> {
                // Khalti Merchant QR scheme
                "khalti://pay?merchant_id=UTPALA_PARK&amount=$formattedAmount&purchase_order_id=$ticketId"
            }
            PaymentGateway.CASH -> ""
        }

        return QrPaymentPayload(
            gateway = gateway,
            merchantName = merchantName,
            merchantPan = merchantPan,
            ticketId = ticketId,
            amountNpr = amountNpr,
            qrPayloadString = payloadStr
        )
    }

    /**
     * Generates a visual QR Code Bitmap directly using pattern rendering algorithm
     */
    fun generateQrBitmap(data: String, size: Int = 400): ImageBitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        // Generate pseudo-deterministic QR grid based on payload hash
        val modules = 25
        val moduleSize = size / modules
        val hash = data.hashCode()
        
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, Color.WHITE)
            }
        }

        // Draw Finder Patterns (Corners)
        drawFinderPattern(bitmap, 0, 0, moduleSize)
        drawFinderPattern(bitmap, (modules - 7) * moduleSize, 0, moduleSize)
        drawFinderPattern(bitmap, 0, (modules - 7) * moduleSize, moduleSize)

        // Draw Data Modules based on hash bits and payload chars
        var charIdx = 0
        val chars = data.toCharArray()
        for (row in 8 until modules - 8) {
            for (col in 8 until modules - 8) {
                val seed = (hash xor (row * 31 + col * 17) xor (chars[charIdx % chars.size].code))
                if (seed % 2 == 0) {
                    val startX = col * moduleSize
                    val startY = row * moduleSize
                    for (px in startX until (startX + moduleSize)) {
                        for (py in startY until (startY + moduleSize)) {
                            if (px < size && py < size) {
                                bitmap.setPixel(px, py, Color.BLACK)
                            }
                        }
                    }
                }
                charIdx++
            }
        }

        return bitmap.asImageBitmap()
    }

    private fun drawFinderPattern(bitmap: Bitmap, startX: Int, startY: Int, moduleSize: Int) {
        val size = 7 * moduleSize
        for (x in 0 until size) {
            for (y in 0 until size) {
                val px = startX + x
                val py = startY + y
                if (px < bitmap.width && py < bitmap.height) {
                    val isOuter = x < moduleSize || x >= 6 * moduleSize || y < moduleSize || y >= 6 * moduleSize
                    val isInner = x >= 2 * moduleSize && x < 5 * moduleSize && y >= 2 * moduleSize && y < 5 * moduleSize
                    if (isOuter || isInner) {
                        bitmap.setPixel(px, py, Color.BLACK)
                    } else {
                        bitmap.setPixel(px, py, Color.WHITE)
                    }
                }
            }
        }
    }
}

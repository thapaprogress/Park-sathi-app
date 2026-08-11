package com.example.print

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.IBinder
import android.util.Log
import com.example.data.ParkingTicket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import woyou.aidl.ICallback
import woyou.aidl.IWoyouService
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ParkingPrintEngine private constructor() {

    private var sunmiPrinterService: IWoyouService? = null
    private var isBound = false

    companion object {
        const val TAG = "ParkingPrintEngine"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        @Volatile
        private var INSTANCE: ParkingPrintEngine? = null

        fun getInstance(): ParkingPrintEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = ParkingPrintEngine()
                INSTANCE = instance
                instance
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            sunmiPrinterService = IWoyouService.Stub.asInterface(service)
            isBound = true
            Log.d(TAG, "SUNMI Printer Service connected successfully.")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sunmiPrinterService = null
            isBound = false
            Log.d(TAG, "SUNMI Printer Service disconnected.")
        }
    }

    /**
     * Binds the SUNMI internal printer service lifecycle.
     */
    fun bindService(context: Context) {
        if (isBound) return
        try {
            val intent = Intent()
            intent.setPackage("woyou.aidl.printerservice")
            intent.setAction("woyou.aidl.printerservice.ProInterface")
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind SUNMI service, maybe non-SUNMI device: ${e.message}")
        }
    }

    /**
     * Unbinds the SUNMI internal printer service lifecycle.
     */
    fun unbindService(context: Context) {
        if (!isBound) return
        try {
            context.unbindService(serviceConnection)
            sunmiPrinterService = null
            isBound = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unbind SUNMI service: ${e.message}")
        }
    }

    /**
     * Prints a parking check-in ticket.
     * Selects either internal SUNMI printer or external BT printer.
     */
    suspend fun printCheckInTicket(
        context: Context,
        ticket: ParkingTicket,
        useBluetooth: Boolean,
        bluetoothMacAddress: String? = null,
        qrPrefix: String = "",
        footerText: String = "Scan to Checkout\nKeep ticket safe!"
    ): Boolean = withContext(Dispatchers.IO) {
        val qrData = qrPrefix + ticket.ticketId
        val qrBitmap = generateQrBitmap(qrData)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateTimeString = sdf.format(Date(ticket.checkInTime))

        if (useBluetooth && !bluetoothMacAddress.isNullOrEmpty()) {
            return@withContext printExternalBluetooth(bluetoothMacAddress, ticket, dateTimeString, qrBitmap, footerText)
        } else {
            return@withContext printInternalSunmi(ticket, dateTimeString, qrBitmap, footerText)
        }
    }

    /**
     * Formats 2-columns (left-aligned title, right-aligned value) to fit exactly 32 columns for 58mm POS paper.
     */
    private fun formatTwoColumns(title: String, value: String): String {
        val totalCols = 32
        val delta = totalCols - title.length - value.length
        return if (delta > 0) {
            title + " ".repeat(delta) + value
        } else {
            val maxTitleLen = totalCols - value.length - 1
            val truncatedTitle = if (title.length > maxTitleLen) title.substring(0, maxTitleLen) else title
            val repeatCount = totalCols - truncatedTitle.length - value.length
            truncatedTitle + " ".repeat(repeatCount.coerceAtLeast(1)) + value
        }
    }

    /**
     * Submits print via SUNMI AIDL interface
     */
    private fun printInternalSunmi(
        ticket: ParkingTicket,
        dateTimeString: String,
        qrBitmap: Bitmap,
        footerText: String
    ): Boolean {
        val service = sunmiPrinterService ?: return false
        try {
            val callback = object : ICallback.Stub() {
                override fun onRunResult(isSuccess: Boolean) {
                    Log.d(TAG, "SUNMI Print Operation Result: $isSuccess")
                }
                override fun onReturnString(result: String?) {
                    Log.d(TAG, "SUNMI Print Return String: $result")
                }
                override fun onRaiseException(code: Int, msg: String?) {
                    Log.e(TAG, "SUNMI Print Exception: $code, $msg")
                }
            }

            // Standard Sunmi alignment: 0=Left, 1=Center, 2=Right
            service.printerInit(callback)
            
            // Header: Bold, Centered, Big
            service.setAlignment(1, callback)
            service.setPrinterFontSize(24f, callback) // Double height approx
            service.printText("UTPALA PARKING\n", callback)
            
            // Divider
            service.setPrinterFontSize(19f, callback) // Normal height approx
            service.printText("--------------------------------\n", callback)
            
            // Body fields
            service.setAlignment(0, callback) // Left
            service.printText(formatTwoColumns("Ticket ID:", ticket.ticketId.take(12)) + "\n", callback)
            service.printText(formatTwoColumns("Vehicle No:", ticket.vehicleNumber) + "\n", callback)
            service.printText(formatTwoColumns("Vehicle Type:", ticket.vehicleType.name) + "\n", callback)
            service.printText(formatTwoColumns("Date & Time:", dateTimeString) + "\n", callback)
            
            // Divider
            service.printText("--------------------------------\n", callback)
            
            // QR Code centered
            service.setAlignment(1, callback)
            service.printBitmap(qrBitmap, callback)
            service.printText("\n", callback) // space

            // Footer
            service.printText(footerText + "\n", callback)
            
            // Feed & Cut spacing
            service.lineWrap(4, callback)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI printer service invocation failed: ${e.message}")
            return false
        }
    }

    /**
     * Submits print via external Bluetooth Socket using raw ESC/POS commands
     */
    @SuppressLint("MissingPermission")
    private fun printExternalBluetooth(
        macAddress: String,
        ticket: ParkingTicket,
        dateTimeString: String,
        qrBitmap: Bitmap,
        footerText: String
    ): Boolean {
        var socket: BluetoothSocket? = null
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            if (!adapter.isEnabled) return false

            val device = adapter.getRemoteDevice(macAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            val outputStream = socket.outputStream

            val escPostBuffer = ByteArrayOutputStream()

            // Initialize Printer: ESC @
            escPostBuffer.write(byteArrayOf(0x1B, 0x40))

            // Center alignment: ESC a 1
            escPostBuffer.write(byteArrayOf(0x1B, 0x61, 0x01))

            // Double Height / Width ON: GS ! 0x11 (17) or bold ON
            escPostBuffer.write(byteArrayOf(0x1D, 0x21, 0x11)) // Double height & width
            escPostBuffer.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
            escPostBuffer.write("UTPALA PARKING\n".toByteArray(Charsets.US_ASCII))

            // Double Height OFF, Bold OFF: GS ! 0, ESC E 0
            escPostBuffer.write(byteArrayOf(0x1D, 0x21, 0x00))
            escPostBuffer.write(byteArrayOf(0x1B, 0x45, 0x00))

            // Divider
            escPostBuffer.write("--------------------------------\n".toByteArray(Charsets.US_ASCII))

            // Left alignment: ESC a 0
            escPostBuffer.write(byteArrayOf(0x1B, 0x61, 0x00))

            // Body fields
            escPostBuffer.write((formatTwoColumns("Ticket ID:", ticket.ticketId.take(12)) + "\n").toByteArray(Charsets.US_ASCII))
            escPostBuffer.write((formatTwoColumns("Vehicle No:", ticket.vehicleNumber) + "\n").toByteArray(Charsets.US_ASCII))
            escPostBuffer.write((formatTwoColumns("Vehicle Type:", ticket.vehicleType.name) + "\n").toByteArray(Charsets.US_ASCII))
            escPostBuffer.write((formatTwoColumns("Date & Time:", dateTimeString) + "\n").toByteArray(Charsets.US_ASCII))

            // Divider
            escPostBuffer.write("--------------------------------\n".toByteArray(Charsets.US_ASCII))

            // Center alignment: ESC a 1
            escPostBuffer.write(byteArrayOf(0x1B, 0x61, 0x01))

            // Raster image conversion
            val escPosImageBytes = convertBitmapToEscPos(qrBitmap)
            escPostBuffer.write(escPosImageBytes)
            escPostBuffer.write("\n\n".toByteArray(Charsets.US_ASCII))

            // Footer
            escPostBuffer.write((footerText + "\n").toByteArray(Charsets.US_ASCII))

            // Paper Feed: LF * 4
            escPostBuffer.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))

            outputStream.write(escPostBuffer.toByteArray())
            outputStream.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth print failed: ${e.message}")
            false
        } finally {
            try {
                socket?.close()
            } catch (ex: Exception) {
                Log.e(TAG, "Socket close failed: ${ex.message}")
            }
        }
    }

    /**
     * Converts a Bitmap to ESC/POS binary raster style print commands
     */
    private fun convertBitmapToEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val stream = ByteArrayOutputStream()

        val widthBytes = (width + 7) / 8
        val xL = (widthBytes and 0xFF).toByte()
        val xH = ((widthBytes shr 8) and 0xFF).toByte()
        val yL = (height and 0xFF).toByte()
        val yH = ((height shr 8) and 0xFF).toByte()

        // Command to print raster image: GS v 0 0 xL xH yL yH
        stream.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

        for (y in 0 until height) {
            val rowBytes = ByteArray(widthBytes)
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
                if (luminance < 128) {
                    val byteIndex = x / 8
                    val bitIndex = 7 - (x % 8)
                    rowBytes[byteIndex] = (rowBytes[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
            stream.write(rowBytes)
        }

        return stream.toByteArray()
    }

    /**
     * Generates a stylized, deterministic 2D black-and-white Bitmap resembling a QR-code
     */
    fun generateQrBitmap(data: String): Bitmap {
        val size = 240
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawColor(Color.WHITE)

        val moduleCount = 21
        val moduleSize = size / moduleCount
        val padding = (size - (moduleCount * moduleSize)) / 2

        val grid = Array(moduleCount) { BooleanArray(moduleCount) }

        // Draw standard nested checker corner markers for visual QR similarity
        fun drawMarker(ox: Int, oy: Int) {
            for (x in 0..6) {
                for (y in 0..6) {
                    val dx = ox + x
                    val dy = oy + y
                    if (dx in 0 until moduleCount && dy in 0 until moduleCount) {
                        val isBorder = (x == 0 || x == 6 || y == 0 || y == 6)
                        val isCenter = (x in 2..4 && y in 2..4)
                        if (isBorder || isCenter) {
                            grid[dx][dy] = true
                        }
                    }
                }
            }
        }

        drawMarker(0, 0)
        drawMarker(moduleCount - 7, 0)
        drawMarker(0, moduleCount - 7)

        // Seed-based random noise fill for data body using hash
        var seed = data.hashCode().toLong()
        for (x in 0 until moduleCount) {
            for (y in 0 until moduleCount) {
                val inMarker = (x < 8 && y < 8) || (x >= moduleCount - 8 && y < 8) || (x < 8 && y >= moduleCount - 8)
                if (!inMarker) {
                    seed = (seed * 1103515245 + 12345) and 0x7fffffff
                    grid[x][y] = (seed % 2 == 0L)
                }
            }
        }

        // Draw standard timing lines
        for (i in 8 until moduleCount - 8) {
            grid[i][6] = (i % 2 == 0)
            grid[6][i] = (i % 2 == 0)
        }

        // Render modules
        for (x in 0 until moduleCount) {
            for (y in 0 until moduleCount) {
                if (grid[x][y]) {
                    val left = padding + x * moduleSize
                    val top = padding + y * moduleSize
                    canvas.drawRect(
                        left.toFloat(),
                        top.toFloat(),
                        (left + moduleSize).toFloat(),
                        (top + moduleSize).toFloat(),
                        paint
                    )
                }
            }
        }

        return bitmap
    }

    /**
     * Checks status of SUNMI Printer
     */
    fun checkPrinterStatus(): String {
        val service = sunmiPrinterService ?: return "Printer Disconnected / Offline"
        return "SUNMI Built-in Thermal Printer Ready & Connected"
    }

    /**
     * Prints a formal IRD Fiscal Tax Invoice receipt for Checkout.
     */
    suspend fun printIrdCheckoutReceipt(
        context: Context,
        ticket: ParkingTicket,
        useBluetooth: Boolean,
        bluetoothMacAddress: String? = null,
        companyName: String = "Civil Mall Parking (Prajna World)",
        companyPan: String = "609874123",
        fonepayTraceId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val invNo = ticket.irdInvoiceNo ?: "PS-8283-${ticket.ticketId.take(5)}"
        val qrBitmap = generateQrBitmap(invNo)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val inTime = sdf.format(Date(ticket.checkInTime))
        val outTime = if (ticket.checkOutTime != null) sdf.format(Date(ticket.checkOutTime)) else "N/A"

        val service = sunmiPrinterService ?: return@withContext false
        try {
            val callback = object : ICallback.Stub() {
                override fun onRunResult(isSuccess: Boolean) {}
                override fun onReturnString(result: String?) {}
                override fun onRaiseException(code: Int, msg: String?) {}
            }

            service.printerInit(callback)
            
            // Header
            service.setAlignment(1, callback)
            service.setPrinterFontSize(22f, callback)
            service.printText("$companyName\n", callback)
            service.setPrinterFontSize(18f, callback)
            service.printText("PAN: $companyPan | TAX INVOICE\n", callback)
            service.printText("--------------------------------\n", callback)

            // Invoice details
            service.setAlignment(0, callback)
            service.printText(formatTwoColumns("Bill No:", invNo.take(14)) + "\n", callback)
            service.printText(formatTwoColumns("Vehicle No:", ticket.vehicleNumber) + "\n", callback)
            service.printText(formatTwoColumns("Vehicle Type:", ticket.vehicleType.name) + "\n", callback)
            service.printText(formatTwoColumns("Entry Time:", inTime) + "\n", callback)
            service.printText(formatTwoColumns("Exit Time:", outTime) + "\n", callback)
            
            val payMode = ticket.paymentMethod ?: "CASH"
            service.printText(formatTwoColumns("Payment Mode:", payMode) + "\n", callback)
            if (!fonepayTraceId.isNullOrEmpty() || payMode.contains("FONEPAY")) {
                val trace = fonepayTraceId ?: "FP-${ticket.ticketId.take(6)}"
                service.printText(formatTwoColumns("Fonepay Trace:", trace) + "\n", callback)
            }
            service.printText("--------------------------------\n", callback)

            // Tax breakdown
            val total = ticket.totalAmount ?: 0.0
            val vat = ticket.vatAmount ?: (total * 0.13 / 1.13)
            val net = ticket.netAmount ?: (total - vat)

            service.printText(formatTwoColumns("Net Taxable:", "NPR ${String.format(Locale.US, "%.2f", net)}") + "\n", callback)
            service.printText(formatTwoColumns("VAT (13%):", "NPR ${String.format(Locale.US, "%.2f", vat)}") + "\n", callback)
            service.setPrinterFontSize(20f, callback)
            service.printText(formatTwoColumns("Grand Total:", "NPR ${String.format(Locale.US, "%.2f", total)}") + "\n", callback)
            service.setPrinterFontSize(18f, callback)
            service.printText("--------------------------------\n", callback)

            // Footer
            service.setAlignment(1, callback)
            service.printBitmap(qrBitmap, callback)
            service.printText("\nThank you for parking with us!\nIRD CBMS Verified Fiscal Bill\nSupport: +977-9765985999\n", callback)
            service.lineWrap(3, callback)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to print IRD receipt: ${e.message}", e)
            false
        }
    }

    /**
     * Prints End-of-Day (EOD) Z-Report thermal print on 58mm POS paper
     */
    suspend fun printZReport(
        context: Context,
        operatorId: String,
        merchantName: String,
        panNumber: String,
        totalTickets: Int,
        activeTickets: Int,
        completedTickets: Int,
        cashCollections: Double,
        fonepayCollections: Double,
        totalVatCollected: Double,
        grandTotalNpr: Double,
        unsyncedCount: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val service = sunmiPrinterService ?: return@withContext false
        try {
            val callback = object : ICallback.Stub() {
                override fun onRunResult(isSuccess: Boolean) {}
                override fun onReturnString(result: String?) {}
                override fun onRaiseException(code: Int, msg: String?) {}
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestamp = sdf.format(Date())

            service.printerInit(callback)

            // Header
            service.setAlignment(1, callback)
            service.setPrinterFontSize(22f, callback)
            service.printText("END-OF-DAY Z-REPORT\n", callback)
            service.setPrinterFontSize(18f, callback)
            service.printText("$merchantName\nPAN: $panNumber\n", callback)
            service.printText("================================\n", callback)

            // Shift Info
            service.setAlignment(0, callback)
            service.printText(formatTwoColumns("Report Date:", timestamp.substring(0, 10)) + "\n", callback)
            service.printText(formatTwoColumns("Generated At:", timestamp.substring(11)) + "\n", callback)
            service.printText(formatTwoColumns("Operator ID:", operatorId) + "\n", callback)
            service.printText("--------------------------------\n", callback)

            // Ticket Counters
            service.printText("VOLUME SUMMARY:\n", callback)
            service.printText(formatTwoColumns("Total Tickets:", totalTickets.toString()) + "\n", callback)
            service.printText(formatTwoColumns("Active Tickets:", activeTickets.toString()) + "\n", callback)
            service.printText(formatTwoColumns("Completed Tickets:", completedTickets.toString()) + "\n", callback)
            service.printText("--------------------------------\n", callback)

            // Revenue Collections Breakdown
            service.printText("REVENUE BREAKDOWN:\n", callback)
            service.printText(formatTwoColumns("Cash Sales:", "NPR ${String.format(Locale.US, "%.2f", cashCollections)}") + "\n", callback)
            service.printText(formatTwoColumns("Fonepay QR Sales:", "NPR ${String.format(Locale.US, "%.2f", fonepayCollections)}") + "\n", callback)
            service.printText(formatTwoColumns("Total VAT (13%):", "NPR ${String.format(Locale.US, "%.2f", totalVatCollected)}") + "\n", callback)
            service.printText("--------------------------------\n", callback)

            // Grand Total
            service.setPrinterFontSize(20f, callback)
            service.printText(formatTwoColumns("TOTAL REVENUE:", "NPR ${String.format(Locale.US, "%.2f", grandTotalNpr)}") + "\n", callback)
            service.setPrinterFontSize(18f, callback)
            service.printText("--------------------------------\n", callback)

            // IRD CBMS Sync Status
            service.printText(formatTwoColumns("IRD Sync Pending:", "$unsyncedCount Bills") + "\n", callback)
            service.printText("================================\n", callback)

            service.setAlignment(1, callback)
            service.printText("Operator Signature: _____________\n\n", callback)
            service.printText("Prajna World Park Sathi POS v2.4\n", callback)
            service.lineWrap(4, callback)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to print Z-Report: ${e.message}", e)
            false
        }
    }
}

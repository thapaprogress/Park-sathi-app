package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.util.CameraPermissionHandler
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

enum class CameraScanMode {
    QR_SCANNER,
    PLATE_RECOGNIZER
}

@Composable
fun CameraXScannerModal(
    scanMode: CameraScanMode,
    onDismiss: () -> Unit,
    onCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(CameraPermissionHandler.hasCameraPermission(context))
    }

    val (permissionGranted, launchPermissionRequest) = CameraPermissionHandler.rememberCameraPermissionLauncher(
        onPermissionGranted = {
            hasPermission = true
        },
        onPermissionDenied = {
            hasPermission = false
        }
    )

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launchPermissionRequest()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasPermission) {
                    CameraXScannerPreview(
                        scanMode = scanMode,
                        onCodeDetected = { result ->
                            onCodeDetected(result)
                            onDismiss()
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Camera Permission Required",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Please grant camera permission to scan parking QR tickets or vehicle license plates.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                launchPermissionRequest()
                            }
                        ) {
                            Text("Grant Camera Permission")
                        }
                    }
                }

                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (scanMode == CameraScanMode.QR_SCANNER) Icons.Default.QrCodeScanner else Icons.Default.Pin,
                                contentDescription = null,
                                tint = Color(0xFF00E676)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (scanMode == CameraScanMode.QR_SCANNER) "Scan Ticket QR" else "Plate OCR Scanner",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Scanner",
                            tint = Color.White
                        )
                    }
                }

                // Overlay Finder Frame
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(if (scanMode == CameraScanMode.QR_SCANNER) 260.dp else 280.dp, if (scanMode == CameraScanMode.QR_SCANNER) 260.dp else 160.dp)
                        .border(
                            width = 3.dp,
                            color = Color(0xFF00E676),
                            shape = RoundedCornerShape(16.dp)
                        )
                )

                // Subtitle Guidance
                Text(
                    text = if (scanMode == CameraScanMode.QR_SCANNER)
                        "Align Parking Ticket QR Code inside the box"
                    else "Align Vehicle License Plate in viewfinder",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun CameraXScannerPreview(
    scanMode: CameraScanMode,
    onCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var isProcessed = false

                if (scanMode == CameraScanMode.QR_SCANNER) {
                    val barcodeScanner = BarcodeScanning.getClient()
                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxyForQr(imageProxy, barcodeScanner) { result ->
                            if (!isProcessed && result.isNotBlank()) {
                                isProcessed = true
                                onCodeDetected(result)
                            }
                        }
                    }
                } else {
                    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxyForText(imageProxy, textRecognizer) { plateText ->
                            if (!isProcessed && plateText.isNotBlank()) {
                                isProcessed = true
                                onCodeDetected(plateText)
                            }
                        }
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraXScannerPreview", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxyForQr(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onSuccess(rawValue)
                        break
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxyForText(
    imageProxy: ImageProxy,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                if (text.isNotBlank()) {
                    val cleanPlate = filterPlateText(text)
                    if (cleanPlate.length >= 4) {
                        onSuccess(cleanPlate)
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

private fun String?.isNullProgrammatic(): Boolean {
    return this == null || this.isBlank()
}

/**
 * Normalizes Nepali/Standard vehicle license plate text from camera OCR
 */
private fun filterPlateText(rawText: String): String {
    val lines = rawText.split("\n", " ").map { it.trim().uppercase() }
    for (line in lines) {
        val cleaned = line.replace(Regex("[^A-Z0-9-]"), " ").trim().replace(Regex("\\s+"), " ")
        if (cleaned.matches(Regex(".*[0-9]{3,4}.*"))) {
            return cleaned
        }
    }
    return rawText.take(15).uppercase()
}

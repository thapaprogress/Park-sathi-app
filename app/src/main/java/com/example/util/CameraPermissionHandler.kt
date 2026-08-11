package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object CameraPermissionHandler {

    /**
     * Checks if camera permission is currently granted.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Composable helper that checks permission and launches request if needed.
     */
    @Composable
    fun rememberCameraPermissionLauncher(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit = {}
    ): Pair<Boolean, () -> Unit> {
        val context = LocalContext.current
        var isGranted by remember { mutableStateOf(hasCameraPermission(context)) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            isGranted = granted
            if (granted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }

        val launchRequest: () -> Unit = {
            if (hasCameraPermission(context)) {
                isGranted = true
                onPermissionGranted()
            } else {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }

        return Pair(isGranted, launchRequest)
    }
}

package com.example

import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.ParkingViewModel
import com.example.ui.screens.UtpalaParkingApp
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PosAudioHelper
import com.example.util.SunmiScanReceiver

class MainActivity : ComponentActivity() {

  private val viewModel: ParkingViewModel by viewModels()
  private var sunmiScanReceiver: SunmiScanReceiver? = null
  private val hardwareBarcodeBuffer = StringBuilder()
  private var lastKeyTimestamp = 0L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    sunmiScanReceiver = SunmiScanReceiver { scannedCode ->
      PosAudioHelper.playScanBeep()
      viewModel.handleScannedQrCode(scannedCode)
    }

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          UtpalaParkingApp(viewModel = viewModel)
        }
      }
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_DOWN) {
      val now = System.currentTimeMillis()
      if (now - lastKeyTimestamp > 500) {
        hardwareBarcodeBuffer.clear()
      }
      lastKeyTimestamp = now

      val char = event.unicodeChar.toChar()
      if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_TAB) {
        val scannedBarcode = hardwareBarcodeBuffer.toString().trim()
        if (scannedBarcode.length >= 3) {
          PosAudioHelper.playScanBeep()
          viewModel.handleScannedQrCode(scannedBarcode)
          hardwareBarcodeBuffer.clear()
          return true
        }
      } else if (char.isLetterOrDigit() || char == '-' || char == '_') {
        hardwareBarcodeBuffer.append(char)
      }
    }
    return super.dispatchKeyEvent(event)
  }

  override fun onResume() {
    super.onResume()
    sunmiScanReceiver?.let { receiver ->
      val filter = SunmiScanReceiver.createIntentFilter()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, filter, RECEIVER_EXPORTED)
      } else {
        registerReceiver(receiver, filter)
      }
    }
  }

  override fun onPause() {
    super.onPause()
    sunmiScanReceiver?.let { receiver ->
      try {
        unregisterReceiver(receiver)
      } catch (e: Exception) {
        // Ignored
      }
    }
  }
}



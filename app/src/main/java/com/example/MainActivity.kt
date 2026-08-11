package com.example

import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
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
import com.example.util.SunmiScanReceiver

class MainActivity : ComponentActivity() {

  private val viewModel: ParkingViewModel by viewModels()
  private var sunmiScanReceiver: SunmiScanReceiver? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    sunmiScanReceiver = SunmiScanReceiver { scannedCode ->
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


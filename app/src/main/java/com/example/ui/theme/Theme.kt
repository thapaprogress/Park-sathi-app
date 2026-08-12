package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


private val HighContrastOutdoorColorScheme =
  darkColorScheme(
    primary = Color(0xFFFFD600), // Vibrant High-Vis Yellow
    secondary = Color(0xFF00E5FF), // High-Vis Cyan
    tertiary = Color(0xFFFF3D00),
    background = Color(0xFF000000), // Pure Black for outdoor OLED contrast
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF262626)
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = VibrantIndigo,
    secondary = DeepNavy,
    tertiary = LightIndigoContainer,
    background = Color(0xFF121316),
    surface = Color(0xFF1E2024),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VibrantIndigo,
    secondary = DeepNavy,
    tertiary = LightIndigoContainer,
    background = LightGreyBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DeepNavy,
    onSurface = DeepNavy,
    surfaceVariant = LightGreyContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  isHighContrastOutdoor: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {

  val colorScheme =
    when {
      isHighContrastOutdoor -> HighContrastOutdoorColorScheme
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


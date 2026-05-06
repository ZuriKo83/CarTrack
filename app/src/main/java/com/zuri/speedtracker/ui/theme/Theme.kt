package com.zuri.cartrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.zuri.cartrack.ui.theme.BlackBg1
import com.zuri.cartrack.ui.theme.CardDark
import com.zuri.cartrack.ui.theme.NeonBlue
import com.zuri.cartrack.ui.theme.NeonCyan
import com.zuri.cartrack.ui.theme.NeonPurple
import com.zuri.cartrack.ui.theme.TextMain

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonCyan,
    tertiary = NeonBlue,
    background = BlackBg1,
    surface = CardDark,
    onPrimary = TextMain,
    onSecondary = TextMain,
    onTertiary = TextMain,
    onBackground = TextMain,
    onSurface = TextMain
)

private val LightColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonCyan,
    tertiary = NeonBlue,
    background = BlackBg1,
    surface = CardDark,
    onPrimary = TextMain,
    onSecondary = TextMain,
    onTertiary = TextMain,
    onBackground = TextMain,
    onSurface = TextMain
)

@Composable
fun CarTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
}
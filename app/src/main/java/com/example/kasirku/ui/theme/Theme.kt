package com.example.kasirku.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Skema Warna untuk Mode Terang (Light Theme)
private val LightColorScheme = lightColorScheme(
    primary = PrimaryRed,
    onPrimary = SurfaceWhite,
    secondary = TextGray,
    onSecondary = SurfaceWhite,
    background = BackgroundLight,
    onBackground = TextBlack,
    surface = SurfaceWhite,
    onSurface = TextBlack,
    error = StatusError,
    onError = SurfaceWhite,
    outline = BorderGray,
    // ================== TAMBAHKAN BARIS INI ==================
    surfaceContainer = SurfaceWhite // Samakan dengan warna surface
)

// Skema Warna untuk Mode Gelap (Dark Theme)
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryRedDark,
    onPrimary = TextBlack,
    secondary = BorderGray,
    onSecondary = TextBlack,
    background = DarkBackground,
    onBackground = SurfaceWhite,
    surface = DarkSurface,
    onSurface = SurfaceWhite,
    error = StatusError,
    onError = SurfaceWhite,
    outline = TextGray,
    // ================== TAMBAHKAN BARIS INI ==================
    surfaceContainer = DarkSurface // Samakan dengan warna surface
)

@Composable
fun CashierAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // PERBAIKAN: Status bar harusnya warna surface, bukan primary
            window.statusBarColor = colorScheme.surface.toArgb()
            // PERBAIKAN: Logikanya adalah !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
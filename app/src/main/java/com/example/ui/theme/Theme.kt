package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AldellaBluePrimary,
    onPrimary = AldellaWhite,
    primaryContainer = AldellaBlueLight,
    onPrimaryContainer = AldellaBlueBadgeText,
    secondary = AldellaNavySurface,
    onSecondary = AldellaWhite,
    background = AldellaNavyDark,
    onBackground = AldellaWhite,
    surface = AldellaCardBg,
    onSurface = AldellaTextDark,
    surfaceVariant = AldellaBlueLight,
    onSurfaceVariant = AldellaTextMuted,
    error = AldellaRed,
    onError = AldellaWhite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

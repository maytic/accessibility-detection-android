package com.maytic.objectdetection.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = OliveDark,
    onPrimary = OnOliveDark,
    secondary = OliveGreyDark,
    tertiary = MustardDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark,
)

private val LightColorScheme = lightColorScheme(
    primary = OliveLight,
    onPrimary = OnOliveLight,
    secondary = OliveGreyLight,
    tertiary = MustardLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
)

@Composable
fun AccessibilityObjectDetectionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color would override this custom palette on Android 12+, so it's off by default
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
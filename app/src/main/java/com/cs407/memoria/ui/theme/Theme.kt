package com.cs407.memoria.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 🌙 Dark, purple-pink focused palette
private val DarkColorScheme = darkColorScheme(
    // Buttons & main accents → purple / pink
    primary = Color(0xFFBB86FC),          // main button color (purple)
    secondary = Color(0xFF9C6BFF),        // secondary accents (purple)
    tertiary = Color(0xFFFF79C6),         // extra accent (pink)

    // Dark background & surfaces
    background = Color(0xFF060611),
    surface = Color(0xFF10101A),

    // Text colors on top of those
    onPrimary = Color(0xFF120024),       // text on purple buttons
    onSecondary = Color(0xFF120024),
    onTertiary = Color(0xFF220011),
    onBackground = Color(0xFFF5EFFF),
    onSurface = Color(0xFFF5EFFF),
)

// If you still want a “light” mode, we can keep it mostly the same vibe,
// but slightly brighter; or just mirror dark if you always want dark.
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8A5DFF),
    secondary = Color(0xFFB37AFF),
    tertiary = Color(0xFFFF85D1),

    background = Color(0xFF121222),
    surface = Color(0xFF1B1B2C),

    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF2B0015),
    onBackground = Color(0xFFF5EFFF),
    onSurface = Color(0xFFF5EFFF),
)

@Composable
fun MemoriaTheme(
    // If you want it ALWAYS dark, set this default to true and ignore system:
    darkTheme: Boolean = true,
    // Turn off dynamic color so your purple/pink palette is used
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package com.garageledger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFB34A1E),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFF8F4),
    secondary = androidx.compose.ui.graphics.Color(0xFF385A62),
    tertiary = androidx.compose.ui.graphics.Color(0xFF6E6A3D),
    background = androidx.compose.ui.graphics.Color(0xFFF6F1E8),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBF6),
    onSurface = androidx.compose.ui.graphics.Color(0xFF231C18),
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFFA56B),
    secondary = androidx.compose.ui.graphics.Color(0xFF97C7D1),
    tertiary = androidx.compose.ui.graphics.Color(0xFFD0CA8B),
    background = androidx.compose.ui.graphics.Color(0xFF181311),
    surface = androidx.compose.ui.graphics.Color(0xFF241D18),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF5EEE8),
)

@Composable
fun GarageLedgerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

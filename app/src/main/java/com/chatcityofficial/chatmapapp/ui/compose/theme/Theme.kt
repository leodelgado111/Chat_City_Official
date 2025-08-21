package com.chatcityofficial.chatmapapp.ui.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFB86BB),
    secondary = Color(0xFF8D93D0),
    tertiary = Color(0xFF97D4F0),
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF1A1A1A),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ChatCityTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
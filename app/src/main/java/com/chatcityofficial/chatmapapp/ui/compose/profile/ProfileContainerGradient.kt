package com.chatcityofficial.chatmapapp.ui.compose.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp

@Composable
fun ProfileContainerWithGradient(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 360.dp, height = 150.dp)
            .offset(x = 20.dp, y = 16.dp)
    ) {
        // Base white container with 30% opacity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White.copy(alpha = 0.3f))
        )
        
        // Sweep gradient overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(30.dp))
        ) {
            drawIntoCanvas { canvas ->
                val sweepGradient = SweepGradientShader(
                    center = Offset(0f, 0f), // Top-left corner
                    colors = listOf(
                        Color(0xB38800FF), // Purple with 70% intensity
                        Color(0xB3FF00AA), // Pink with 70% intensity
                        Color(0xB30088FF), // Blue with 70% intensity
                        Color(0xB38800FF)  // Back to purple for smooth transition
                    ),
                    colorStops = listOf(0f, 0.33f, 0.67f, 1f)
                )
                
                val paint = androidx.compose.ui.graphics.Paint().apply {
                    shader = sweepGradient
                }
                
                canvas.drawRect(
                    rect = androidx.compose.ui.geometry.Rect(
                        offset = Offset.Zero,
                        size = size
                    ),
                    paint = paint
                )
            }
        }
    }
}

// Alternative implementation using Brush
@Composable
fun ProfileContainerWithBrushGradient(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 360.dp, height = 150.dp)
            .offset(x = 20.dp, y = 16.dp)
            .clip(RoundedCornerShape(30.dp))
    ) {
        // Base white layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.3f))
        )
        
        // Gradient overlay with 70% color intensity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFB366D9).copy(alpha = 0.7f), // Purple
                            Color(0xFFFF66CC).copy(alpha = 0.7f), // Pink
                            Color(0xFF6699FF).copy(alpha = 0.7f), // Blue
                            Color(0xFFB366D9).copy(alpha = 0.7f)  // Back to purple
                        ),
                        center = Offset(0f, 0f) // Top-left positioning
                    )
                )
        )
    }
}
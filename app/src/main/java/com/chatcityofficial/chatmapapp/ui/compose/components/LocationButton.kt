package com.chatcityofficial.chatmapapp.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.chatcityofficial.chatmapapp.ui.compose.navigation.vectors.GpsWhiteLayer
import com.chatcityofficial.chatmapapp.ui.compose.navigation.vectors.GpsIconOverlay

@Composable
fun LocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(58.dp)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                clip = false
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: White background (bottom layer) with 80% opacity
        Image(
            painter = rememberVectorPainter(image = GpsWhiteLayer),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp) // Circle is 50dp (radius 25 * 2)
                .alpha(0.8f) // 80% opacity like navigation bar
        )
        
        // Layer 2: Gradient layer (middle layer) with 80% opacity
        // Using same conic gradient as navigation bar
        Box(
            modifier = Modifier
                .size(48.dp)
                .alpha(0.8f) // 80% opacity with additional 0.3 from SVG = 0.24 total
                .clip(CircleShape)
                .drawBehind {
                    // Conic gradient with center point above and to the left, same as nav bar
                    val center = Offset(-size.width * 0.5f, -size.height * 1.5f)
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colorStops = arrayOf(
                                0f to Color(217, 161, 206, (0.3725f * 255).toInt()),           // 0°
                                (140.4f/360f) to Color(152, 213, 241, (0.6f * 255).toInt()),   // 140.4°
                                (216f/360f) to Color(202, 173, 214, (0.6f * 255).toInt()),     // 216°
                                (284.4f/360f) to Color(252, 134, 188, (0.25f * 255).toInt()),  // 284.4°
                                1f to Color(217, 161, 206, (0.3725f * 255).toInt())            // 360°
                            ),
                            center = center
                        )
                    )
                }
        )
        
        // Layer 3: GPS Icon (top layer) - full opacity
        Image(
            painter = rememberVectorPainter(image = GpsIconOverlay),
            contentDescription = "Location",
            modifier = Modifier.size(26.dp)
        )
    }
}
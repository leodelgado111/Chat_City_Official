package com.chatcityofficial.chatmapapp.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Composable
fun ThemedLocationIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: White background circle with 80% opacity
        Box(
            modifier = Modifier
                .size(16.dp)
                .alpha(0.8f)
                .clip(CircleShape)
                .drawBehind {
                    drawCircle(
                        color = Color.White
                    )
                }
        )
        
        // Layer 2: Gradient layer with 80% opacity
        Box(
            modifier = Modifier
                .size(15.dp)
                .alpha(0.8f)
                .clip(CircleShape)
                .drawBehind {
                    // Conic gradient with center point above and to the left
                    val center = Offset(-size.width * 0.5f, -size.height * 1.5f)
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colorStops = arrayOf(
                                0f to Color(217, 161, 206, (0.3725f * 255).toInt()),
                                (140.4f/360f) to Color(152, 213, 241, (0.6f * 255).toInt()),
                                (216f/360f) to Color(202, 173, 214, (0.6f * 255).toInt()),
                                (284.4f/360f) to Color(252, 134, 188, (0.25f * 255).toInt()),
                                1f to Color(217, 161, 206, (0.3725f * 255).toInt())
                            ),
                            center = center
                        )
                    )
                }
        )
        
        // Layer 3: Location pin icon
        Image(
            painter = rememberVectorPainter(image = LocationPinIcon),
            contentDescription = "Location",
            modifier = Modifier.size(10.dp)
        )
    }
}

// Small location pin icon
private val LocationPinIcon: ImageVector
    get() = ImageVector.Builder(
        name = "locationPin",
        defaultWidth = 10.dp,
        defaultHeight = 10.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.Black)
        ) {
            // Location pin path
            moveTo(12f, 2f)
            curveTo(8.13f, 2f, 5f, 5.13f, 5f, 9f)
            curveTo(5f, 14.25f, 12f, 22f, 12f, 22f)
            reflectiveCurveToRelative(7f, -7.75f, 7f, -13f)
            curveTo(19f, 5.13f, 15.87f, 2f, 12f, 2f)
            close()
            moveTo(12f, 11.5f)
            curveTo(10.62f, 11.5f, 9.5f, 10.38f, 9.5f, 9f)
            reflectiveCurveToRelative(1.12f, -2.5f, 2.5f, -2.5f)
            reflectiveCurveToRelative(2.5f, 1.12f, 2.5f, 2.5f)
            reflectiveCurveToRelative(-1.12f, 2.5f, -2.5f, 2.5f)
            close()
        }
    }.build()
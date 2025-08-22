package com.chatcityofficial.chatmapapp.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThemedLocationContainer(
    locationText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Container without elevation or shadow
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Main content box
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            // Layer 1: White background with 80% opacity
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.8f)
                    .background(Color.White, RoundedCornerShape(12.dp))
            )
            
            // Layer 2: Gradient layer with 40% opacity (same as navigation bar)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.4f) // 40% opacity like navigation bar
                    .clip(RoundedCornerShape(12.dp))
                    .drawBehind {
                        // Two-layer gradient: purple from top-left + pink from bottom
                        
                        // First layer: Pink radial from bottom (scaled for smaller container, shifted 30dp right)
                        val bottomCenter = Offset(size.width / 2f + 30.dp.toPx(), size.height + 36.dp.toPx()) // Scaled down from 72dp
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(252, 134, 188, (0.4f * 255).toInt()),  // Pink center
                                    Color(202, 173, 214, (0.6f * 255).toInt()),  // Purple-blue blend
                                    Color(130, 190, 220, (0.7f * 255).toInt())   // Slightly darker blue
                                ),
                                center = bottomCenter,
                                radius = size.height * 1.5f
                            ),
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )
                        
                        // Second layer: Soft purple overlay (scaled for smaller container)
                        val topLeftCenter = Offset(size.width * 0.45f, 15.dp.toPx()) // Proportionally positioned
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(180, 150, 220, (0.5f * 255).toInt()),  // Soft purple center
                                    Color(180, 150, 220, (0.3f * 255).toInt()),  // Fading purple
                                    Color(180, 150, 220, 0)                      // Transparent edges
                                ),
                                center = topLeftCenter,
                                radius = size.width * 0.7f
                            ),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            blendMode = BlendMode.Plus
                        )
                    }
            )
            
            // White stroke layer - fully opaque
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .drawBehind {
                        drawRoundRect(
                            color = Color.White,
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.dp.toPx()
                            )
                        )
                    }
            )
            
            // Content that determines the size - with equal padding
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = rememberVectorPainter(image = MagnifyingGlassIcon),
                    contentDescription = "Search",
                    modifier = Modifier
                        .size(14.08.dp) // 10% smaller than 15.64dp (15.64 * 0.9 = 14.08)
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = locationText,
                    color = Color.Black,
                    fontSize = 14.3.sp, // 10% bigger than 13sp (13 * 1.1 = 14.3)
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis // Use ellipsis for long text
                )
            }
        }
    }
}

// Magnifying glass icon
private val MagnifyingGlassIcon: ImageVector
    get() = ImageVector.Builder(
        name = "magnifyingGlass",
        defaultWidth = 16.dp,
        defaultHeight = 16.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
            strokeLineWidth = 2f,
            fill = null
        ) {
            // Circle for magnifying glass
            moveTo(11f, 4f)
            arcTo(7f, 7f, 0f, true, true, 4f, 11f)
            arcTo(7f, 7f, 0f, false, true, 11f, 4f)
            close()
            
            // Handle
            moveTo(16.5f, 16.5f)
            lineTo(21f, 21f)
        }
    }.build()
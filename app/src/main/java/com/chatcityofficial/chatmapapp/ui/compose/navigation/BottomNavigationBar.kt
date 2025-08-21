package com.chatcityofficial.chatmapapp.ui.compose.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

enum class NavigationTab {
    SAVED, HOME, CREATE, CHATS, PROFILE
}

@Composable
fun BottomNavigationBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(334.dp)
            .height(68.dp)
    ) {
        // Layer 1: White background with 80% opacity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.8f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
        )
        
        // Layer 2: Gradient3 layer from navigation_bar_3 folder
        // Reduced to 40% opacity
        Box(
            modifier = Modifier
                .width(332.dp)
                .height(66.dp)
                .align(Alignment.Center)
                .alpha(0.4f) // Reduced to 40% opacity
                .clip(RoundedCornerShape(19.dp))
                .drawBehind {
                    // Two-layer gradient: purple from top-left + pink from bottom
                    
                    // First layer: Pink radial from bottom (moved up by 10% - from 80dp to 72dp below)
                    val bottomCenter = Offset(size.width / 2f, size.height + 72.dp.toPx())
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
                        cornerRadius = CornerRadius(19.dp.toPx())
                    )
                    
                    // Second layer: Soft purple overlay (shifted right by 150dp and down by 30dp)
                    val topLeftCenter = Offset(150.dp.toPx(), 30.dp.toPx())
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
                        cornerRadius = CornerRadius(19.dp.toPx()),
                        blendMode = BlendMode.Plus
                    )
                }
        )
        
        // Layer 3: White stroke outline on top
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp)
                )
        )
        
        // Layer 4: Outline that moves to selected tab (except CREATE)
        val outlineXPosition = when (selectedTab) {
            NavigationTab.SAVED -> 43.dp - 37.dp    // 43dp is center of saved icon
            NavigationTab.HOME -> 105.dp - 37.dp    // 105dp is center of home icon
            NavigationTab.CREATE -> 43.dp - 37.dp   // Default to saved position when create is selected
            NavigationTab.CHATS -> 229.dp - 37.dp   // 229dp is center of chats icon
            NavigationTab.PROFILE -> 291.dp - 37.dp // 291dp is center of profile icon
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Selection outline that moves with selected tab
            Box(
                modifier = Modifier
                    .size(74.dp, 56.dp)
                    .offset(x = outlineXPosition, y = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberVectorPainter(image = SelectionOutline),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(Color.Black)
                )
            }
        }
        
        // Layer 5: All icons positioned with consistent spacing
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Saved icon - 43dp from left
            Image(
                painter = rememberVectorPainter(image = SavedIcon),
                contentDescription = "Saved",
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = 43.dp - 12.dp, y = 34.dp - 12.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(NavigationTab.SAVED) },
                colorFilter = ColorFilter.tint(Color.Black)
            )
            
            // Home icon - 105dp from left
            Image(
                painter = rememberVectorPainter(image = HomeIcon),
                contentDescription = "Home",
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = 105.dp - 12.dp, y = 34.dp - 12.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(NavigationTab.HOME) },
                colorFilter = ColorFilter.tint(Color.Black)
            )
            
            // Chats icon - 229dp from left
            Image(
                painter = rememberVectorPainter(image = ChatsIcon),
                contentDescription = "Chats",
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = 229.dp - 12.dp, y = 34.dp - 12.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(NavigationTab.CHATS) },
                colorFilter = ColorFilter.tint(Color.Black)
            )
            
            // Profile icon - 291dp from left
            Image(
                painter = rememberVectorPainter(image = ProfileIcon),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = 291.dp - 12.dp, y = 34.dp - 12.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(NavigationTab.PROFILE) },
                colorFilter = ColorFilter.tint(Color.Black)
            )
        }
        
        // Layer 6: Single Create icon centered in container
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center // Centers both horizontally and vertically
        ) {
            Image(
                painter = rememberVectorPainter(image = CreateIcon),
                contentDescription = "Create",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(NavigationTab.CREATE) },
                colorFilter = ColorFilter.tint(Color.Black)
            )
        }
    }
}

// Saved icon vector
private val SavedIcon: ImageVector
    get() = ImageVector.Builder(
        name = "saved",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            moveTo(19f, 21f)
            lineTo(12f, 16.7778f)
            lineTo(5f, 21f)
            lineTo(5f, 4.11111f)
            arcTo(2.11111f, 2.11111f, 0f, false, true, 7f, 2f)
            lineTo(17f, 2f)
            arcTo(2.11111f, 2.11111f, 0f, false, true, 19f, 4.11111f)
            lineTo(19f, 21f)
            close()
        }
    }.build()

// Home icon vector
private val HomeIcon: ImageVector
    get() = ImageVector.Builder(
        name = "home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            // House outline
            moveTo(3f, 9f)
            lineTo(12f, 2f)
            lineTo(21f, 9f)
            lineTo(21f, 20f)
            arcTo(1f, 1f, 0f, false, true, 20f, 21f)
            lineTo(4f, 21f)
            arcTo(1f, 1f, 0f, false, true, 3f, 20f)
            lineTo(3f, 9f)
            close()
            // Door
            moveTo(9f, 21f)
            lineTo(9f, 13f)
            lineTo(15f, 13f)
            lineTo(15f, 21f)
        }
    }.build()

// Create icon vector (location pin with plus)
private val CreateIcon: ImageVector
    get() = ImageVector.Builder(
        name = "create",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            // Simple pin shape - circle at top tapering to point
            moveTo(12f, 3f)
            arcTo(8f, 8f, 0f, false, false, 4f, 11f)
            curveTo(4f, 16f, 11f, 21f, 12f, 22f)
            curveTo(13f, 21f, 20f, 16f, 20f, 11f)
            arcTo(8f, 8f, 0f, false, false, 12f, 3f)
            close()
            // Plus sign
            moveTo(12f, 8f)
            lineTo(12f, 14f)
            moveTo(9f, 11f)
            lineTo(15f, 11f)
        }
    }.build()

// Chats icon vector (two overlapping chat bubbles)
private val ChatsIcon: ImageVector
    get() = ImageVector.Builder(
        name = "chats",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            // First chat bubble
            moveTo(3f, 15f)
            lineTo(3f, 5f)
            arcTo(2f, 2f, 0f, false, true, 5f, 3f)
            lineTo(15f, 3f)
            arcTo(2f, 2f, 0f, false, true, 17f, 5f)
            lineTo(17f, 11f)
            arcTo(2f, 2f, 0f, false, true, 15f, 13f)
            lineTo(8f, 13f)
            lineTo(3f, 16f)
            lineTo(3f, 15f)
            close()
            // Second chat bubble
            moveTo(8f, 16f)
            lineTo(8f, 17f)
            arcTo(2f, 2f, 0f, false, false, 10f, 19f)
            lineTo(17f, 19f)
            lineTo(22f, 22f)
            lineTo(22f, 21f)
            lineTo(22f, 11f)
            arcTo(2f, 2f, 0f, false, false, 20f, 9f)
            lineTo(20f, 9f)
        }
    }.build()

// Profile icon vector
private val ProfileIcon: ImageVector
    get() = ImageVector.Builder(
        name = "profile",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            // Head circle
            moveTo(16f, 6f)
            arcTo(4f, 4f, 0f, true, true, 8f, 6f)
            arcTo(4f, 4f, 0f, true, true, 16f, 6f)
            close()
            // Body/shoulders
            moveTo(5f, 21f)
            lineTo(5f, 19f)
            arcTo(4f, 4f, 0f, false, true, 9f, 15f)
            lineTo(15f, 15f)
            arcTo(4f, 4f, 0f, false, true, 19f, 19f)
            lineTo(19f, 21f)
        }
    }.build()

// Selection outline vector
private val SelectionOutline: ImageVector
    get() = ImageVector.Builder(
        name = "outline",
        defaultWidth = 74.dp,
        defaultHeight = 56.dp,
        viewportWidth = 74f,
        viewportHeight = 56f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            fill = null
        ) {
            // Rectangle with rounded corners
            moveTo(15f, 1f)
            lineTo(59f, 1f)
            arcTo(14f, 14f, 0f, false, true, 73f, 15f)
            lineTo(73f, 41f)
            arcTo(14f, 14f, 0f, false, true, 59f, 55f)
            lineTo(15f, 55f)
            arcTo(14f, 14f, 0f, false, true, 1f, 41f)
            lineTo(1f, 15f)
            arcTo(14f, 14f, 0f, false, true, 15f, 1f)
            close()
        }
    }.build()
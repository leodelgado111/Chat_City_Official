package com.chatcityofficial.chatmapapp.ui.compose.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
        // Remember the last non-CREATE tab
        var lastNonCreateTab by remember { mutableStateOf(NavigationTab.SAVED) }
        
        // Update lastNonCreateTab immediately when a non-CREATE tab is selected
        if (selectedTab != NavigationTab.CREATE) {
            lastNonCreateTab = selectedTab
        }
        
        // Use either the current tab or the last non-CREATE tab for outline position
        val outlineTab = if (selectedTab == NavigationTab.CREATE) {
            lastNonCreateTab
        } else {
            selectedTab
        }
        
        val targetXPosition = when (outlineTab) {
            NavigationTab.SAVED -> 43.dp - 37.dp    // 43dp is center of saved icon
            NavigationTab.HOME -> 105.dp - 37.dp    // 105dp is center of home icon
            NavigationTab.CREATE -> 43.dp - 37.dp   // Should never happen, but default to saved
            NavigationTab.CHATS -> 229.dp - 37.dp   // 229dp is center of chats icon
            NavigationTab.PROFILE -> 291.dp - 37.dp // 291dp is center of profile icon
        }
        
        // Animate the X position with a smooth transition
        val animatedXPosition by animateDpAsState(
            targetValue = targetXPosition,
            animationSpec = tween(durationMillis = 100),
            label = "outline_animation"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Selection outline that moves with selected tab
            Box(
                modifier = Modifier
                    .size(74.dp, 56.dp)
                    .offset(x = animatedXPosition, y = 6.dp),
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

// Chats icon vector (two overlapping chat bubbles - updated version)
private val ChatsIcon: ImageVector
    get() = ImageVector.Builder(
        name = "chats",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // First chat bubble with tail
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            moveTo(17f, 11f)
            curveTo(17f, 11.5304f, 16.7893f, 12.0391f, 16.4142f, 12.4142f)
            curveTo(16.0391f, 12.7893f, 15.5304f, 13f, 15f, 13f)
            lineTo(7.828f, 13f)
            curveTo(7.29761f, 13.0001f, 6.78899f, 13.2109f, 6.414f, 13.586f)
            lineTo(4.212f, 15.788f)
            curveTo(4.1127f, 15.8873f, 3.9862f, 15.9549f, 3.84849f, 15.9823f)
            curveTo(3.71077f, 16.0097f, 3.56803f, 15.9956f, 3.43831f, 15.9419f)
            curveTo(3.30858f, 15.8881f, 3.1977f, 15.7971f, 3.11969f, 15.6804f)
            curveTo(3.04167f, 15.5637f, 3.00002f, 15.4264f, 3f, 15.286f)
            lineTo(3f, 5f)
            curveTo(3f, 4.46957f, 3.21071f, 3.96086f, 3.58579f, 3.58579f)
            curveTo(3.96086f, 3.21071f, 4.46957f, 3f, 5f, 3f)
            lineTo(15f, 3f)
            curveTo(15.5304f, 3f, 16.0391f, 3.21071f, 16.4142f, 3.58579f)
            curveTo(16.7893f, 3.96086f, 17f, 4.46957f, 17f, 5f)
            lineTo(17f, 11f)
            close()
        }
        // Second chat bubble with tail
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            moveTo(18f, 7f)
            curveTo(18.5304f, 7f, 19.0391f, 7.21071f, 19.4142f, 7.58579f)
            curveTo(19.7893f, 7.96086f, 20f, 8.46957f, 20f, 9f)
            lineTo(20f, 19.286f)
            curveTo(20f, 19.4264f, 19.9583f, 19.5637f, 19.8803f, 19.6804f)
            curveTo(19.8023f, 19.7971f, 19.6914f, 19.8881f, 19.5617f, 19.9419f)
            curveTo(19.432f, 19.9956f, 19.2892f, 20.0097f, 19.1515f, 19.9823f)
            curveTo(19.0138f, 19.9549f, 18.8873f, 19.8873f, 18.788f, 19.788f)
            lineTo(16.586f, 17.586f)
            curveTo(16.211f, 17.2109f, 15.7024f, 17.0001f, 15.172f, 17f)
            lineTo(8f, 17f)
            curveTo(7.46957f, 17f, 6.96086f, 16.7893f, 6.58579f, 16.4142f)
            curveTo(6.21071f, 16.0391f, 6f, 15.5304f, 6f, 15f)
            lineTo(6f, 14f)
        }
    }.build()

// Profile icon vector (updated with profile_icon_5)
private val ProfileIcon: ImageVector
    get() = ImageVector.Builder(
        name = "profile",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Note: Original SVG is 16x21, centering it in 24x24 viewport
        // Offset: x by 4 (to center 16 in 24), y by 1.5 (to center 21 in 24)
        
        // Head circle
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            moveTo(12f, 10.5f)  // 8 + 4 = 12, 9 + 1.5 = 10.5
            curveTo(14.2091f, 10.5f, 16f, 8.70914f, 16f, 6.5f)  // 5 + 1.5 = 6.5
            curveTo(16f, 4.29086f, 14.2091f, 2.5f, 12f, 2.5f)  // 1 + 1.5 = 2.5
            curveTo(9.79086f, 2.5f, 8f, 4.29086f, 8f, 6.5f)
            curveTo(8f, 8.70914f, 9.79086f, 10.5f, 12f, 10.5f)
            close()
        }
        // Body/shoulders path
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            moveTo(9f, 14.5f)  // 5 + 4 = 9, 13 + 1.5 = 14.5
            lineTo(15f, 14.5f)  // 11 + 4 = 15
            curveTo(17.2091f, 14.5f, 19f, 16.2909f, 19f, 18.5f)  // 15 + 4 = 19, 17 + 1.5 = 18.5
            lineTo(19f, 19.5f)  // 18 + 1.5 = 19.5
            curveTo(19f, 20.6046f, 18.1046f, 21.5f, 17f, 21.5f)  // 13 + 4 = 17, 20 + 1.5 = 21.5
            lineTo(7f, 21.5f)  // 3 + 4 = 7
            curveTo(5.89543f, 21.5f, 5f, 20.6046f, 5f, 19.5f)  // 1 + 4 = 5
            lineTo(5f, 18.5f)
            curveTo(5f, 16.2909f, 6.79086f, 14.5f, 9f, 14.5f)
            close()
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
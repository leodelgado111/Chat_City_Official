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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
        
        // Layer 3: Base layer with icon cutouts (transparent holes)
        // This layer will show the gradient through the cutouts
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Draw a semi-transparent overlay with icon cutouts
                    drawIntoCanvas { canvas ->
                        val paint = androidx.compose.ui.graphics.Paint().apply {
                            color = Color.Black.copy(alpha = 0.05f) // Very light overlay
                            blendMode = BlendMode.SrcOver
                        }
                        
                        // Draw base rectangle
                        canvas.drawRoundRect(
                            0f, 0f, size.width, size.height,
                            20.dp.toPx(), 20.dp.toPx(),
                            paint
                        )
                    }
                }
        )
        
        // Layer 4: White stroke outline on top
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
        
        // Layer 4: Black filled outline that moves with selected tab
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Selection outline (filled black)
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
        
        // Layer 5: Icons with gradient colors when selected
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Define colors for each icon based on selection state
            // When selected, use gradient colors; when not selected, use black
            
            // Saved icon - 43dp from left
            val savedIconColor = if (outlineTab == NavigationTab.SAVED) {
                Color(171, 191, 212)  // Reduced purple blend by 20%
            } else {
                Color.Black
            }
            
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
                colorFilter = ColorFilter.tint(savedIconColor)
            )
            
            // Home icon - 105dp from left
            val homeIconColor = if (outlineTab == NavigationTab.HOME) {
                Color(171, 191, 212)  // Reduced purple blend by 20%
            } else {
                Color.Black
            }
            
            Image(
                painter = rememberVectorPainter(image = HomeIcon),
                contentDescription = "Home",
                modifier = Modifier
                    .size(width = 20.dp, height = 21.dp)
                    .offset(x = 105.dp - 10.dp, y = 34.dp - 10.5.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(NavigationTab.HOME) },
                colorFilter = ColorFilter.tint(homeIconColor)
            )
            
            // Chats icon - 229dp from left
            val chatsIconColor = if (outlineTab == NavigationTab.CHATS) {
                Color(171, 191, 212)  // Reduced purple blend by 20%
            } else {
                Color.Black
            }
            
            Image(
                painter = rememberVectorPainter(image = ChatsIcon),
                contentDescription = "Chats",
                modifier = Modifier
                    .size(21.dp)
                    .offset(x = 229.dp - 10.5.dp, y = 34.dp - 10.5.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(NavigationTab.CHATS) },
                colorFilter = ColorFilter.tint(chatsIconColor)
            )
            
            // Profile icon - 291dp from left
            val profileIconColor = if (outlineTab == NavigationTab.PROFILE) {
                Color(171, 191, 212)  // Reduced purple blend by 20%
            } else {
                Color.Black
            }
            
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
                colorFilter = ColorFilter.tint(profileIconColor)
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

// Saved icon vector - wallet with dollar sign (from group325)
private val SavedIcon: ImageVector
    get() = ImageVector.Builder(
        name = "saved",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Wallet/Card shape with rounded corners
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            fill = null
        ) {
            // Top flap of wallet
            moveTo(16f, 6.8f)
            curveTo(16f, 5.0083f, 16f, 4.1134f, 15.414f, 3.5567f)
            curveTo(14.828f, 3f, 13.886f, 3f, 12f, 3f)
            curveTo(10.114f, 3f, 9.172f, 3f, 8.586f, 3.5567f)
            curveTo(8f, 4.1134f, 8f, 5.0083f, 8f, 6.8f)
            
            // Main wallet body
            moveTo(2f, 14.4f)
            curveTo(2f, 10.8175f, 2f, 9.02585f, 3.172f, 7.9134f)
            curveTo(4.344f, 6.80095f, 6.229f, 6.8f, 10f, 6.8f)
            lineTo(14f, 6.8f)
            curveTo(17.771f, 6.8f, 19.657f, 6.8f, 20.828f, 7.9134f)
            curveTo(21.999f, 9.0268f, 22f, 10.8175f, 22f, 14.4f)
            curveTo(22f, 17.9824f, 22f, 19.7741f, 20.828f, 20.8866f)
            curveTo(19.656f, 21.999f, 17.771f, 22f, 14f, 22f)
            lineTo(10f, 22f)
            curveTo(6.229f, 22f, 4.343f, 22f, 3.172f, 20.8866f)
            curveTo(2.001f, 19.7732f, 2f, 17.9824f, 2f, 14.4f)
            close()
        }
        
        // Dollar sign in the center
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            // S-curve of dollar sign
            moveTo(12f, 17.4164f)
            curveTo(13.105f, 17.4164f, 14f, 16.7636f, 14f, 15.9586f)
            curveTo(14f, 15.1536f, 13.105f, 14.5f, 12f, 14.5f)
            curveTo(10.895f, 14.5f, 10f, 13.8473f, 10f, 13.0414f)
            curveTo(10f, 12.2364f, 10.895f, 11.5836f, 12f, 11.5836f)
            
            // Bottom curve
            moveTo(12f, 17.4164f)
            curveTo(10.895f, 17.4164f, 10f, 16.7636f, 10f, 15.9586f)
            
            // Vertical lines
            moveTo(12f, 17.4164f)
            lineTo(12f, 18f)
            moveTo(12f, 11.5836f)
            lineTo(12f, 11f)
            
            // Top curve
            moveTo(12f, 11.5836f)
            curveTo(13.105f, 11.5836f, 14f, 12.2364f, 14f, 13.0414f)
        }
    }.build()

// Home icon vector - house with door (from group323)
private val HomeIcon: ImageVector
    get() = ImageVector.Builder(
        name = "home",
        defaultWidth = 20.dp,
        defaultHeight = 21.dp,
        viewportWidth = 20f,
        viewportHeight = 21f
    ).apply {
        // Door in the center
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            moveTo(13f, 19.9995f)
            lineTo(13f, 11.9995f)
            curveTo(13f, 11.7343f, 12.8946f, 11.4799f, 12.7071f, 11.2924f)
            curveTo(12.5196f, 11.1049f, 12.2652f, 10.9995f, 12f, 10.9995f)
            lineTo(8f, 10.9995f)
            curveTo(7.73478f, 10.9995f, 7.48043f, 11.1049f, 7.29289f, 11.2924f)
            curveTo(7.10536f, 11.4799f, 7f, 11.7343f, 7f, 11.9995f)
            lineTo(7f, 19.9995f)
        }
        
        // House outline with roof
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            moveTo(1f, 8.99948f)
            curveTo(0.99993f, 8.70855f, 1.06333f, 8.4211f, 1.18579f, 8.1572f)
            curveTo(1.30824f, 7.89329f, 1.4868f, 7.65928f, 1.709f, 7.47148f)
            lineTo(8.709f, 1.47248f)
            curveTo(9.06999f, 1.16739f, 9.52736f, 1f, 10f, 1f)
            curveTo(10.4726f, 1f, 10.93f, 1.16739f, 11.291f, 1.47248f)
            lineTo(18.291f, 7.47148f)
            curveTo(18.5132f, 7.65928f, 18.6918f, 7.89329f, 18.8142f, 8.1572f)
            curveTo(18.9367f, 8.4211f, 19.0001f, 8.70855f, 19f, 8.99948f)
            lineTo(19f, 17.9995f)
            curveTo(19f, 18.5299f, 18.7893f, 19.0386f, 18.4142f, 19.4137f)
            curveTo(18.0391f, 19.7888f, 17.5304f, 19.9995f, 17f, 19.9995f)
            lineTo(3f, 19.9995f)
            curveTo(2.46957f, 19.9995f, 1.96086f, 19.7888f, 1.58579f, 19.4137f)
            curveTo(1.21071f, 19.0386f, 1f, 18.5299f, 1f, 17.9995f)
            lineTo(1f, 8.99948f)
            close()
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

// Chats icon vector - dual overlapping chat bubbles (from group327)
private val ChatsIcon: ImageVector
    get() = ImageVector.Builder(
        name = "chats",
        defaultWidth = 21.dp,
        defaultHeight = 21.dp,
        viewportWidth = 21f,
        viewportHeight = 21f
    ).apply {
        // First chat bubble (front)
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            moveTo(15f, 8.38694f)
            curveTo(15f, 8.87672f, 14.7893f, 9.34645f, 14.4142f, 9.69278f)
            curveTo(14.0391f, 10.0391f, 13.5304f, 10.2337f, 13f, 10.2337f)
            lineTo(5.828f, 10.2337f)
            curveTo(5.29761f, 10.2338f, 4.78899f, 10.4284f, 4.414f, 10.7748f)
            lineTo(2.212f, 12.808f)
            curveTo(2.1127f, 12.8997f, 1.9862f, 12.9621f, 1.84849f, 12.9874f)
            curveTo(1.71077f, 13.0127f, 1.56803f, 12.9997f, 1.43831f, 12.9501f)
            curveTo(1.30858f, 12.9005f, 1.1977f, 12.8165f, 1.11969f, 12.7087f)
            curveTo(1.04167f, 12.6009f, 1.00002f, 12.4741f, 1f, 12.3445f)
            lineTo(1f, 2.84674f)
            curveTo(1f, 2.35695f, 1.21071f, 1.88723f, 1.58579f, 1.5409f)
            curveTo(1.96086f, 1.19457f, 2.46957f, 1f, 3f, 1f)
            lineTo(13f, 1f)
            curveTo(13.5304f, 1f, 14.0391f, 1.19457f, 14.4142f, 1.5409f)
            curveTo(14.7893f, 1.88723f, 15f, 2.35695f, 15f, 2.84674f)
            lineTo(15f, 8.38694f)
            close()
        }
        
        // Second chat bubble (back/offset) - modified from group327
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            moveTo(18f, 6f)
            curveTo(18.5304f, 6f, 19.0391f, 6.22699f, 19.4142f, 6.63105f)
            curveTo(19.7893f, 7.0351f, 20f, 7.58311f, 20f, 8.15452f)
            lineTo(20f, 19.2352f)
            curveTo(20f, 19.3865f, 19.9583f, 19.5344f, 19.8803f, 19.6601f)
            curveTo(19.8023f, 19.7859f, 19.6914f, 19.8839f, 19.5617f, 19.9418f)
            curveTo(19.432f, 19.9997f, 19.2892f, 20.0148f, 19.1515f, 19.9853f)
            curveTo(19.0138f, 19.9558f, 18.8873f, 19.883f, 18.788f, 19.776f)
            lineTo(16.586f, 17.4039f)
            curveTo(16.211f, 16.9998f, 15.7024f, 16.7727f, 15.172f, 16.7726f)
            lineTo(8f, 16.7726f)
            curveTo(7.46957f, 16.7726f, 6.96086f, 16.5456f, 6.58579f, 16.1416f)
            curveTo(6.21071f, 15.7375f, 6f, 15.1895f, 6f, 14.6181f)
            lineTo(6f, 13.5f)
        }
    }.build()

// Profile icon vector (original profile_icon_3)
private val ProfileIcon: ImageVector
    get() = ImageVector.Builder(
        name = "profile",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Head circle
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        ) {
            moveTo(12f, 10f)
            curveTo(14.2091f, 10f, 16f, 8.20914f, 16f, 6f)
            curveTo(16f, 3.79086f, 14.2091f, 2f, 12f, 2f)
            curveTo(9.79086f, 2f, 8f, 3.79086f, 8f, 6f)
            curveTo(8f, 8.20914f, 9.79086f, 10f, 12f, 10f)
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
            moveTo(19f, 21f)
            lineTo(19f, 18.6667f)
            curveTo(19f, 17.429f, 18.5786f, 16.242f, 17.8284f, 15.3668f)
            curveTo(17.0783f, 14.4917f, 16.0609f, 14f, 15f, 14f)
            lineTo(9f, 14f)
            curveTo(7.93913f, 14f, 6.92172f, 14.4917f, 6.17157f, 15.3668f)
            curveTo(5.42143f, 16.242f, 5f, 17.429f, 5f, 18.6667f)
            lineTo(5f, 21f)
        }
    }.build()

// Selection outline vector (filled with black)
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
            fill = SolidColor(Color.Black)  // Added black fill
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
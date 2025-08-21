package com.chatcityofficial.chatmapapp.ui.compose.navigation.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GpsWhiteLayer: ImageVector
    get() {
        if (_gpsWhiteLayer != null) return _gpsWhiteLayer!!
        
        _gpsWhiteLayer = ImageVector.Builder(
            name = "gpsWhiteLayer",
            defaultWidth = 58.dp,
            defaultHeight = 58.dp,
            viewportWidth = 58f,
            viewportHeight = 58f
        ).apply {
            // White circle with drop shadow (shadow will be handled by Compose modifier)
            path(
                fill = SolidColor(Color.White)
            ) {
                // Circle at center (29, 25) with radius 25
                moveTo(54f, 25f)
                arcTo(25f, 25f, 0f, true, true, 4f, 25f)
                arcTo(25f, 25f, 0f, true, true, 54f, 25f)
                close()
            }
        }.build()
        
        return _gpsWhiteLayer!!
    }

private var _gpsWhiteLayer: ImageVector? = null
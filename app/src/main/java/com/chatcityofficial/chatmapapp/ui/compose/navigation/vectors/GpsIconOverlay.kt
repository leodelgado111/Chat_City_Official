package com.chatcityofficial.chatmapapp.ui.compose.navigation.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GpsIconOverlay: ImageVector
    get() {
        if (_gpsIconOverlay != null) return _gpsIconOverlay!!
        
        _gpsIconOverlay = ImageVector.Builder(
            name = "gpsIconOverlay",
            defaultWidth = 26.dp,
            defaultHeight = 26.dp,
            viewportWidth = 26f,
            viewportHeight = 26f
        ).apply {
            // Arrow/navigation icon
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 12.3684f)
                lineTo(25f, 1f)
                lineTo(13.6316f, 25f)
                lineTo(11.1053f, 14.8947f)
                lineTo(1f, 12.3684f)
                close()
            }
        }.build()
        
        return _gpsIconOverlay!!
    }

private var _gpsIconOverlay: ImageVector? = null
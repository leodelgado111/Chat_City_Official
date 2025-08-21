package com.chatcityofficial.chatmapapp.ui.compose.navigation.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val NewBottomWhiteLayer: ImageVector
    get() {
        if (_newBottomWhiteLayer != null) return _newBottomWhiteLayer!!
        
        _newBottomWhiteLayer = ImageVector.Builder(
            name = "newBottomWhiteLayer",
            defaultWidth = 334.dp,
            defaultHeight = 68.dp,
            viewportWidth = 334f,
            viewportHeight = 68f
        ).apply {
            group {
                path(
                    fill = SolidColor(Color(0xFFFFFFFF))
                ) {
                    moveTo(20f, 0f)
                    horizontalLineTo(314f)
                    arcTo(20f, 20f, 0f, false, true, 334f, 20f)
                    verticalLineTo(48f)
                    arcTo(20f, 20f, 0f, false, true, 314f, 68f)
                    horizontalLineTo(20f)
                    arcTo(20f, 20f, 0f, false, true, 0f, 48f)
                    verticalLineTo(20f)
                    arcTo(20f, 20f, 0f, false, true, 20f, 0f)
                    close()
                }
            }
        }.build()
        
        return _newBottomWhiteLayer!!
    }

private var _newBottomWhiteLayer: ImageVector? = null
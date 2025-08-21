package com.chatcityofficial.chatmapapp.ui.compose.navigation.vectors

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val NewMiddleGradient: ImageVector
    get() {
        if (_newMiddleGradient != null) return _newMiddleGradient!!
        
        _newMiddleGradient = ImageVector.Builder(
            name = "newMiddleGradient",
            defaultWidth = 332.dp,
            defaultHeight = 66.dp,
            viewportWidth = 332f,
            viewportHeight = 66f
        ).apply {
            group {
                path {
                }
            }
        }.build()
        
        return _newMiddleGradient!!
    }

private var _newMiddleGradient: ImageVector? = null
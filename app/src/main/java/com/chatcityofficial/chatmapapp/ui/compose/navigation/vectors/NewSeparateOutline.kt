package com.chatcityofficial.chatmapapp.ui.compose.navigation.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val NewSeparateOutline: ImageVector
    get() {
        if (_newSeparateOutline != null) return _newSeparateOutline!!
        
        _newSeparateOutline = ImageVector.Builder(
            name = "newSeparateOutline",
            defaultWidth = 58.dp,
            defaultHeight = 52.dp,
            viewportWidth = 58f,
            viewportHeight = 52f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f
            ) {
                moveTo(15f, 1f)
                horizontalLineTo(43f)
                arcTo(14f, 14f, 0f, false, true, 57f, 15f)
                verticalLineTo(37f)
                arcTo(14f, 14f, 0f, false, true, 43f, 51f)
                horizontalLineTo(15f)
                arcTo(14f, 14f, 0f, false, true, 1f, 37f)
                verticalLineTo(15f)
                arcTo(14f, 14f, 0f, false, true, 15f, 1f)
                close()
            }
        }.build()
        
        return _newSeparateOutline!!
    }

private var _newSeparateOutline: ImageVector? = null
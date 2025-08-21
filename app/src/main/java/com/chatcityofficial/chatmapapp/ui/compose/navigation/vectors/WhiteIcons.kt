package com.chatcityofficial.chatmapapp.ui.compose.navigation.vectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Saved Icon (Bookmark) - White
val SavedIconWhite: ImageVector
    get() = ImageVector.Builder(
        name = "savedIconWhite",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            fill = null,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 3f)
            horizontalLineTo(19f)
            verticalLineTo(21f)
            lineTo(12f, 16f)
            lineTo(5f, 21f)
            verticalLineTo(3f)
            close()
        }
    }.build()

// Home Icon - White
val HomeIconWhite: ImageVector
    get() = ImageVector.Builder(
        name = "homeIconWhite",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            fill = null,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 9f)
            lineTo(12f, 2f)
            lineTo(21f, 9f)
            verticalLineTo(20f)
            arcTo(2f, 2f, 0f, false, true, 19f, 22f)
            horizontalLineTo(5f)
            arcTo(2f, 2f, 0f, false, true, 3f, 20f)
            verticalLineTo(9f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            fill = null,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9f, 22f)
            verticalLineTo(12f)
            horizontalLineTo(15f)
            verticalLineTo(22f)
        }
    }.build()

// Create Icon (Plus) - White
val CreateIconWhite: ImageVector
    get() = ImageVector.Builder(
        name = "createIconWhite",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            fill = null,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 12f)
            horizontalLineTo(19f)
            moveTo(12f, 5f)
            verticalLineTo(19f)
        }
    }.build()

// Chats Icon - White
val ChatsIconWhite: ImageVector
    get() = ImageVector.Builder(
        name = "chatsIconWhite",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            fill = null,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(21f, 11.5f)
            arcTo(8.38f, 8.38f, 0f, false, true, 20.9f, 12.5f)
            arcTo(8.5f, 8.5f, 0f, false, true, 12.5f, 21f)
            arcTo(8.38f, 8.38f, 0f, false, true, 11.5f, 20.9f)
            lineTo(3f, 21f)
            lineTo(3.1f, 12.5f)
            arcTo(8.5f, 8.5f, 0f, false, true, 11.5f, 4f)
            arcTo(8.38f, 8.38f, 0f, false, true, 12.5f, 4.1f)
            arcTo(8.5f, 8.5f, 0f, false, true, 21f, 11.5f)
            close()
        }
    }.build()

// Profile Icon - White
val ProfileIconWhite: ImageVector
    get() = ImageVector.Builder(
        name = "profileIconWhite",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Head circle
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            fill = null,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(16f, 7f)
            arcTo(4f, 4f, 0f, true, true, 8f, 7f)
            arcTo(4f, 4f, 0f, true, true, 16f, 7f)
            close()
        }
        // Body
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            fill = null,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 21f)
            verticalLineTo(19f)
            arcTo(4f, 4f, 0f, false, true, 8f, 15f)
            horizontalLineTo(16f)
            arcTo(4f, 4f, 0f, false, true, 20f, 19f)
            verticalLineTo(21f)
        }
    }.build()
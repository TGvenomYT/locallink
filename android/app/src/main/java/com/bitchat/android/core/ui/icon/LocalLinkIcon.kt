package com.bitchat.android.core.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * LocalLink's mark: a three-node mesh graph, representing local peer-to-peer relay.
 * Matches the shape used for the adaptive launcher icon (see res/drawable/ic_launcher_foreground.xml).
 */
val LocalLinkIcon: ImageVector
    get() {
        _LocalLinkIcon?.let { return it }

        return ImageVector.Builder(
            name = "LocalLinkIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 0.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 7.4f)
                lineTo(7.4f, 15.5f)
                moveTo(12f, 7.4f)
                lineTo(16.6f, 15.5f)
                moveTo(7.4f, 15.5f)
                lineTo(16.6f, 15.5f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 6f)
                lineTo(13.4f, 7.4f)
                lineTo(12f, 8.8f)
                lineTo(10.6f, 7.4f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(7.4f, 14.1f)
                lineTo(8.8f, 15.5f)
                lineTo(7.4f, 16.9f)
                lineTo(6f, 15.5f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(16.6f, 14.1f)
                lineTo(18f, 15.5f)
                lineTo(16.6f, 16.9f)
                lineTo(15.2f, 15.5f)
                close()
            }
        }.build().also { _LocalLinkIcon = it }
    }

private var _LocalLinkIcon: ImageVector? = null

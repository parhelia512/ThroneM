package io.throneproj.thronem.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

fun Modifier.tvFocusBorder(
    shape: Shape? = null,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f)
    val borderColor by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
    )
    val resolvedShape = shape ?: MaterialTheme.shapes.small
    this
        .onFocusChanged { isFocused = it.isFocused }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .border(2.dp, borderColor, resolvedShape)
}

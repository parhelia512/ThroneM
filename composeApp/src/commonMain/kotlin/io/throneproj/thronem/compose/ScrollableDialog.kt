package io.throneproj.thronem.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter

@Composable
fun ScrollableDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    textPadding: PaddingValues = PaddingValues(),
    text: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = {
            val scrollState = rememberScrollState()
            val density = LocalDensity.current
            val windowInfo = LocalWindowInfo.current
            val maxTextHeight =
                with(density) { windowInfo.containerSize.height.toDp() }.takeIf { it > 0.dp }
                    ?.times(0.8f)
                    ?: 480.dp
            var viewportHeight by remember { mutableStateOf(0) }

            Row(
                modifier = Modifier
                    .heightIn(max = maxTextHeight)
                    .onSizeChanged { viewportHeight = it.height },
            ) {
                Column(
                    modifier = modifier
                        .weight(1f)
                        .padding(textPadding)
                        .verticalScroll(scrollState),
                ) {
                    text()
                }

                if (viewportHeight > 0) {
                    BoxedVerticalScrollbar(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .height(with(density) { viewportHeight.toDp() }),
                        adapter = rememberScrollbarAdapter(scrollState = scrollState),
                        style = defaultMaterialScrollbarStyle().copy(
                            thickness = 12.dp,
                        ),
                    )
                }
            }
        },
    )
}

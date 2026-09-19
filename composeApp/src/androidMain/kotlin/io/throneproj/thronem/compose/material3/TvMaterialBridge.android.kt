package io.throneproj.thronem.compose.material3

import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import androidx.compose.material3.LocalTextStyle as MaterialLocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.LocalContentColor as TvLocalContentColor
import androidx.tv.material3.LocalTextStyle as TvLocalTextStyle

@Composable
internal fun ProvideTvMaterialBridge(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        MaterialLocalContentColor provides TvLocalContentColor.current,
        MaterialLocalTextStyle provides TvLocalTextStyle.current,
        content = content,
    )
}

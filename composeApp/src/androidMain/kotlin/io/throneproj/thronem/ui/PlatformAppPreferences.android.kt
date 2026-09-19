package io.throneproj.thronem.ui

import androidx.compose.runtime.Composable
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.Preference
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.apps
import io.throneproj.thronem.resources.apps_message
import io.throneproj.thronem.resources.legend_toggle
import io.throneproj.thronem.resources.not_set
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun AppSelectPreference(
    packages: Set<String>,
    onSelectApps: (Set<String>) -> Unit,
) {
    Preference(
        title = { Text(stringResource(Res.string.apps)) },
        icon = {
            MaskedIcon(
                resource = Res.drawable.legend_toggle,
                color = IconMaskColors.IconLavender,
            )
        },
        summary = {
            val text = when (val size = packages.size) {
                0 -> stringResource(Res.string.not_set)
                in 1..5 -> packages.joinToString("\n")
                else -> pluralStringResource(Res.plurals.apps_message, size, size)
            }
            Text(text)
        },
        onClick = {
            onSelectApps(packages)
        },
    )
}

package io.throneproj.thronem.ui.profile

import android.content.Intent
import androidx.compose.material3.DropdownMenuItem
import io.throneproj.thronem.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.throneproj.thronem.QuickToggleActivity
import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.lib.R
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.create_shortcut
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun platformSupportShortcut(): Boolean {
    val context = LocalContext.current
    return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
}

@Composable
internal actual fun ShortcutMenuItem(entity: ProxyEntity, postClick: () -> Unit) {
    val context = LocalContext.current
    DropdownMenuItem(
        text = { Text(stringResource(Res.string.create_shortcut)) },
        onClick = {
            val name = entity.displayName()
            val shortcut = ShortcutInfoCompat
                .Builder(context, QuickToggleActivity.shortcutId(entity.id))
                .setShortLabel(name)
                .setLongLabel(name)
                .setIcon(
                    IconCompat.createWithResource(
                        context,
                        R.drawable.ic_qu_shadowsocks_launcher,
                    ),
                )
                .setIntent(
                    Intent(context, QuickToggleActivity::class.java)
                        .setAction(Intent.ACTION_MAIN)
                        .putExtra(QuickToggleActivity.EXTRA_PROFILE_ID, entity.id),
                )
                .build()
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            postClick()
        },
    )
}

package io.throneproj.thronem.ui.configuration

import android.content.Intent
import androidx.compose.material3.DropdownMenuItem
import io.throneproj.thronem.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.add_profile_methods_scan_qr_code
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun ScannerDropdownMenuItem() {
    val context = LocalContext.current
    DropdownMenuItem(
        text = { Text(stringResource(Res.string.add_profile_methods_scan_qr_code)) },
        onClick = {
            context.startActivity(
                Intent(context, ScannerActivity::class.java),
            )
        },
    )
}
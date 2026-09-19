@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.throneproj.thronem.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.throneproj.thronem.compose.material3.Text

@Composable
fun DropdownMenuSectionHeader(text: String) {
    MenuDefaults.Label {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding),
    )
}

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.throneproj.thronem.compose

import androidx.compose.material3.AppBarMenuState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import io.throneproj.thronem.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import io.throneproj.thronem.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import io.throneproj.thronem.resources.*

@Composable
fun ExpandableDropdownMenuItem(
    text: String,
    shape: Shape = MenuDefaults.shape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        onClick = onClick,
        text = { Text(text) },
        shape = shape,
        modifier = modifier,
        trailingIcon = {
            Icon(
                imageVector = vectorResource(Res.drawable.navigate_next),
                contentDescription = stringResource(Res.string.expand),
            )
        },
    )
}

@Composable
fun MoreOverIcon(menuState: AppBarMenuState) {
    SimpleIconButton(
        imageVector = vectorResource(Res.drawable.more_vert),
        contentDescription = stringResource(Res.string.more),
        onClick = menuState::show,
    )
}
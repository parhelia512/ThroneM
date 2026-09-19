package io.throneproj.thronem.ui.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.CapsuleTopBar
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.material3.Button
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.withNavigation
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.ktx.runOnIoDispatcher
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.arrow_back
import io.throneproj.thronem.resources.back
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.warning
import io.throneproj.thronem.ui.LocalSnackbarEmitter
import io.throneproj.thronem.ui.StringOrRes
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun DebugScreen(
    modifier: Modifier = Modifier,
    onBackPress: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val snackbar = LocalSnackbarEmitter.current
    val windowInsets = WindowInsets.safeDrawing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showResetAlert by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CapsuleTopBar(
                navigationIcon = {
                    SimpleIconButton(
                        imageVector = vectorResource(Res.drawable.arrow_back),
                        contentDescription = stringResource(Res.string.back),
                        onClick = onBackPress,
                    )
                },
                title = { Text("DEBUG") },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val contentPadding = innerPadding.withNavigation()
        val layoutDirection = LocalLayoutDirection.current
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                    )
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding()))
                Spacer(modifier = Modifier.height(16.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = "Debug Actions",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { error("Test crash") },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Crash from Kotlin")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showResetAlert = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Reset settings")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
            }

            BoxedVerticalScrollbar(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = scrollState),
                style = defaultMaterialScrollbarStyle().copy(
                    thickness = 12.dp,
                ),
            )
        }
    }
    if (showResetAlert) AlertDialog(
        onDismissRequest = { showResetAlert = false },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                runOnIoDispatcher {
                    DataStore.configurationStore.reset()
                }
                snackbar.show(StringOrRes.Direct("Cleared"))
                showResetAlert = false
            }
        },
        dismissButton = {
            TextButton(stringResource(Res.string.cancel)) {
                showResetAlert = false
            }
        },
        icon = {
            Icon(
                imageVector = vectorResource(Res.drawable.warning),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("Dangerous action") },
        text = { Text("Are you sure to reset your settings?") },
    )
}

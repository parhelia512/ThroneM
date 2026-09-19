package io.throneproj.thronem.ui.tools

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.CapsuleTopBar
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.material3.Button
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.withNavigation
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.arrow_back
import io.throneproj.thronem.resources.back
import io.throneproj.thronem.resources.network_quality_test
import io.throneproj.thronem.resources.network_quality_test_summary
import io.throneproj.thronem.resources.start
import io.throneproj.thronem.resources.stun_test
import io.throneproj.thronem.resources.stun_test_summary
import io.throneproj.thronem.resources.tools_network
import io.throneproj.thronem.ui.NavRoutes
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun NetworkScreen(
    modifier: Modifier = Modifier,
    onBackPress: () -> Unit,
    onOpenTool: (NavRoutes.ToolsPage) -> Unit,
) {
    val scrollState = rememberScrollState()
    val windowInsets = WindowInsets.safeDrawing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
                title = { Text(stringResource(Res.string.tools_network)) },
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
                ActivityCard(
                    title = stringResource(Res.string.stun_test),
                    description = stringResource(Res.string.stun_test_summary),
                    launch = {
                        onOpenTool(NavRoutes.ToolsPage.Stun)
                    },
                )
                PlatformNetworkTools(onOpenTool)
                ActivityCard(
                    title = stringResource(Res.string.network_quality_test),
                    description = stringResource(Res.string.network_quality_test_summary),
                    launch = {
                        onOpenTool(NavRoutes.ToolsPage.NetworkQuality)
                    },
                )
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
}

@Composable
internal fun ActivityCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    launch: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = launch,
                ) {
                    Text(stringResource(Res.string.start))
                }
            }
        }
    }
}

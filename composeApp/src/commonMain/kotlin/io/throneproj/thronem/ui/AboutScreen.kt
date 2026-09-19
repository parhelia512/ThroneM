package io.throneproj.thronem.ui


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.nekohasekai.libbox.Libbox
import io.throneproj.thronem.BuildConfig
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.platformCombinedClickable
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.SimpleTopAppBar
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.setPlainText
import io.throneproj.thronem.ktx.isPreReleaseVersion
import io.throneproj.thronem.compose.withNavigation
import io.throneproj.thronem.core.LibboxCoreClient
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.android
import io.throneproj.thronem.resources.app_name
import io.throneproj.thronem.resources.battery_charging_full
import io.throneproj.thronem.resources.build
import io.throneproj.thronem.resources.build_environment
import io.throneproj.thronem.resources.arrow_back
import io.throneproj.thronem.resources.back
import io.throneproj.thronem.resources.code
import io.throneproj.thronem.resources.copy_success
import io.throneproj.thronem.resources.data_usage
import io.throneproj.thronem.resources.document
import io.throneproj.thronem.resources.g_translate
import io.throneproj.thronem.resources.gavel
import io.throneproj.thronem.resources.github
import io.throneproj.thronem.resources.ignore_battery_optimizations
import io.throneproj.thronem.resources.ignore_battery_optimizations_sum
import io.throneproj.thronem.resources.library_music
import io.throneproj.thronem.resources.menu_about
import io.throneproj.thronem.resources.oss_licenses
import io.throneproj.thronem.resources.project
import io.throneproj.thronem.resources.translate_platform
import io.throneproj.thronem.resources.version_x
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onBackPress: () -> Unit,
    onNavigateToLibraries: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val windowInsets = WindowInsets.safeDrawing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarEmitter.current
    val listState = rememberLazyListState()

    val displayVersion = remember(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) {
        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }
    val releaseLink = remember(BuildConfig.VERSION_NAME) {
        val isPreVersion = isPreReleaseVersion(BuildConfig.VERSION_NAME)
        if (isPreVersion) {
            "https://github.com/throneproj/thronem/releases"
        } else {
            "https://github.com/throneproj/thronem/releases/latest"
        }
    }
    val boxVersion = remember { Libbox.version() }
    val buildEnvironment = remember { LibboxCoreClient.buildEnvironment() }

    val shouldRequestBattery = rememberShouldRequestBatteryOptimizations()
    val requestIgnoreBatteryOptimizations = rememberRequestIgnoreBatteryOptimizations()

    fun putToClipboard(text: String) {
        scope.launch {
            clipboard.setPlainText(text)
            snackbar.show(StringOrRes.Res(Res.string.copy_success))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SimpleTopAppBar(
                title = { Text(stringResource(Res.string.menu_about)) },
                navigationIcon = {
                    SimpleIconButton(
                        imageVector = vectorResource(Res.drawable.arrow_back),
                        contentDescription = stringResource(Res.string.back),
                        onClick = onBackPress,
                    )
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val uriHandler = LocalUriHandler.current
        val contentPadding = innerPadding.withNavigation()

        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = contentPadding,
            ) {
                item("versions_card") {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CardItem(
                                icon = { Icon(vectorResource(Res.drawable.android), null) },
                                title = "ThroneM",
                                description = displayVersion,
                                onClick = { putToClipboard(displayVersion) },
                                onLongClick = { uriHandler.openUri(releaseLink) },
                            )
                            CardItem(
                                icon = {
                                    Icon(
                                        vectorResource(Res.drawable.library_music),
                                        null,
                                    )
                                },
                                title = stringResource(Res.string.version_x, "sing-box"),
                                description = boxVersion,
                                onClick = { putToClipboard(boxVersion) },
                                onLongClick = {
                                    uriHandler.openUri("https://github.com/SagerNet/sing-box")
                                },
                            )
                            CardItem(
                                icon = {
                                    Icon(
                                        vectorResource(Res.drawable.build),
                                        null,
                                    )
                                },
                                title = stringResource(Res.string.build_environment),
                                description = buildEnvironment,
                                onClick = { putToClipboard(buildEnvironment) },
                                onLongClick = {
                                    val isExpert = !DataStore.isExpert.getBlocking()
                                    DataStore.isExpert.setBlocking(isExpert)
                                    snackbar.show(StringOrRes.Direct("isExpert: $isExpert"))
                                },
                            )

                            if (shouldRequestBattery) {
                                CardItem(
                                    icon = {
                                        Icon(
                                            vectorResource(Res.drawable.battery_charging_full),
                                            null,
                                        )
                                    },
                                    title = stringResource(Res.string.ignore_battery_optimizations),
                                    description = stringResource(Res.string.ignore_battery_optimizations_sum),
                                    onClick = { requestIgnoreBatteryOptimizations() },
                                )
                            }
                        }
                    }
                }

                item("project_card") {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.project),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium,
                            )

                            CardItem(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                icon = { Icon(vectorResource(Res.drawable.code), null) },
                                title = stringResource(Res.string.github),
                                onClick = { uriHandler.openUri("https://github.com/throneproj/thronem") },
                            )
                            CardItem(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                icon = {
                                    Icon(
                                        vectorResource(Res.drawable.data_usage),
                                        null,
                                    )
                                },
                                title = stringResource(Res.string.document),
                                onClick = {
                                    uriHandler.openUri("https://github.com/throneproj/thronem/wiki")
                                },
                            )
                            CardItem(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                icon = {
                                    Icon(
                                        vectorResource(Res.drawable.g_translate),
                                        null,
                                    )
                                },
                                title = stringResource(Res.string.translate_platform),
                                onClick = { uriHandler.openUri("https://hosted.weblate.org/projects/thronem/thronem/") },
                            )
                            CardItem(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                icon = {
                                    Icon(
                                        vectorResource(Res.drawable.gavel),
                                        null,
                                    )
                                },
                                title = stringResource(Res.string.oss_licenses),
                                onClick = onNavigateToLibraries,
                            )
                        }
                    }
                }
            }

            BoxedVerticalScrollbar(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = listState),
                style = defaultMaterialScrollbarStyle().copy(
                    thickness = 12.dp,
                ),
            )
        }
    }

}


@Composable
private fun CardItem(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {
        Spacer(
            Modifier.size(24.dp),
        )
    },
    title: String,
    description: String? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .platformCombinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.size(16.dp))
        Column(
            modifier = modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (description != null) {
                Spacer(Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewAboutScreen() {
    PreviewContainer {
        AboutScreen(
            onBackPress = {},
            onNavigateToLibraries = {},
        )
    }
}

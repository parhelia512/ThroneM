package io.throneproj.thronem.ui.dashboard

import io.nekohasekai.libbox.Libbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ElevatedCard
import io.throneproj.thronem.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import io.throneproj.thronem.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.fadingEdge
import io.throneproj.thronem.compose.rememberSwipeToDismissBoxStateUnsaveable
import io.throneproj.thronem.compose.theme.LogColors
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.connection_speed
import io.throneproj.thronem.resources.connection_status_active
import io.throneproj.thronem.resources.connection_status_closed
import io.throneproj.thronem.resources.delete_forever
import io.throneproj.thronem.resources.traffic
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun DashboardConnectionsScreen(
    modifier: Modifier = Modifier,
    uiState: DashboardState,
    bottomPadding: Dp,
    resolveProcessInfo: suspend (String?, Int) -> ProcessInfo?,
    closeConnection: (uuid: String) -> Unit,
    onConnectionClick: (uuid: String) -> Unit,
) {
    val itemSpacing = 12.dp
    val listState = rememberLazyListState()

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .fadingEdge(listState),
                state = listState,
                contentPadding = PaddingValues(bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                items(
                    items = uiState.connections,
                    key = { it.uuid },
                    contentType = { 0 },
                ) { connection ->
                    val swipState = rememberSwipeToDismissBoxStateUnsaveable(connection.uuid)
                    SwipeToDismissBox(
                        state = swipState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.delete_forever),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onError,
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        modifier = Modifier.fillMaxWidth(),
                        onDismiss = { swipeToDismissBoxValue ->
                            if (swipeToDismissBoxValue == SwipeToDismissBoxValue.EndToStart) {
                                closeConnection(connection.uuid)
                            }
                        },
                    ) {
                        ConnectionCard(
                            connection = connection,
                            resolveProcessInfo = resolveProcessInfo,
                            onConnectionClick = onConnectionClick,
                        )
                    }
                }
            }

            BoxedVerticalScrollbar(
                modifier = Modifier.fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = listState),
                style = defaultMaterialScrollbarStyle().copy(
                    thickness = 12.dp,
                ),
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    modifier: Modifier = Modifier,
    connection: ConnectionDetailState,
    resolveProcessInfo: suspend (String?, Int) -> ProcessInfo?,
    onConnectionClick: (uuid: String) -> Unit,
) {
    val process = connection.processes?.firstOrNull()
    val uid = connection.uid
    var processInfo by remember { mutableStateOf<ProcessInfo?>(null) }
    // No keys because LazyColumn's item keys handle it
    LaunchedEffect(Unit) {
        processInfo = resolveProcessInfo(process, uid)
    }

    ElevatedCard(
        onClick = { onConnectionClick(connection.uuid) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (connection.protocol == null) {
                        connection.network
                    } else {
                        "${connection.network}/${connection.protocol}"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LogColors.green,
                )
                Text(
                    text = connection.dst,
                    fontSize = 16.sp,
                    color = Color(0xFFFB7299), // Pink
                )
                val host = connection.host
                val showHost = host.isNotBlank() && !connection.dst.startsWith(host)
                if (showHost) Text(
                    text = host,
                    fontSize = 16.sp,
                    color = LogColors.redLight,
                )
                Text(
                    text = connection.inbound,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondaryFixed,
                )

                Text(
                    text = connection.chain,
                    fontSize = 14.sp,
                    color = LogColors.blue,
                )

                Text(
                    text = stringResource(
                        Res.string.traffic,
                        Libbox.formatBytes(connection.uploadTotal),
                        Libbox.formatBytes(connection.downloadTotal),
                    ),
                    fontSize = 14.sp,
                )

                Text(
                    text = stringResource(
                        Res.string.connection_speed,
                        Libbox.formatBytes(connection.uploadSpeed),
                        Libbox.formatBytes(connection.downloadSpeed),
                    ),
                    fontSize = 14.sp,
                )

                Text(
                    text = stringResource(
                        if (connection.isClosed) {
                            Res.string.connection_status_closed
                        } else {
                            Res.string.connection_status_active
                        },
                    ),
                    fontSize = 14.sp,
                    color = if (connection.isClosed) {
                        Color.Red
                    } else {
                        Color.Green
                    },
                )
            }
            processInfo?.icon?.let { icon ->
                ProcessIcon(
                    icon = icon,
                    contentDescription = processInfo?.label,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
}

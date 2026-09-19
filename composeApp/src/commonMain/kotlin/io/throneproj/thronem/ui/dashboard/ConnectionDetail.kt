package io.throneproj.thronem.ui.dashboard

import io.nekohasekai.libbox.Libbox

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CardDefaults
import io.throneproj.thronem.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.platformCombinedClickable
import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.emptyAsNull
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.add_road
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.chain
import io.throneproj.thronem.resources.close
import io.throneproj.thronem.resources.closed_time
import io.throneproj.thronem.resources.connection_status
import io.throneproj.thronem.resources.connection_status_active
import io.throneproj.thronem.resources.connection_status_closed
import io.throneproj.thronem.resources.create_rule
import io.throneproj.thronem.resources.delete_forever
import io.throneproj.thronem.resources.destination_address
import io.throneproj.thronem.resources.done
import io.throneproj.thronem.resources.download
import io.throneproj.thronem.resources.download_speed
import io.throneproj.thronem.resources.http_host
import io.throneproj.thronem.resources.inbound
import io.throneproj.thronem.resources.ip_version
import io.throneproj.thronem.resources.network
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.outbound
import io.throneproj.thronem.resources.outbound_rule
import io.throneproj.thronem.resources.process
import io.throneproj.thronem.resources.protocol
import io.throneproj.thronem.resources.source_address
import io.throneproj.thronem.resources.speed
import io.throneproj.thronem.resources.start_time
import io.throneproj.thronem.resources.upload
import io.throneproj.thronem.resources.upload_speed
import io.throneproj.thronem.ui.RouteSettingsUiState
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private enum class ConnectionFields {
    // STATUS,
    // INBOUND,
    // IP_VERSION,
    NETWORK,

    // UPLOAD_TOTAL,
    // DOWNLOAD_TOTAL,
    // START,
    SOURCE,
    DESTINATION,
    HOST,

    // MATCHED_RULE,
    // OUTBOUND,
    // CHAIN,
    PROTOCOL,
    PROCESS,
}

@Composable
internal fun ConnectionDetailSheet(
    modifier: Modifier = Modifier,
    connection: ConnectionDetailState,
    resolveProcessInfo: suspend (String?, Int) -> ProcessInfo?,
    closeConnection: (uuid: String) -> Unit,
    onDismiss: () -> Unit,
    openRouteSettings: (RouteSettingsUiState) -> Unit,
) {
    val uuid = connection.uuid
    var isSelecting by remember(uuid) { mutableStateOf(false) }
    var selectedField by remember(uuid) { mutableStateOf(emptySet<ConnectionFields>()) }
    val selectField = { field: ConnectionFields, checked: Boolean ->
        selectedField = if (checked) selectedField + field else selectedField - field
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = uuid,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isSelecting) {
                SimpleIconButton(
                    imageVector = vectorResource(Res.drawable.close),
                    contentDescription = stringResource(Res.string.cancel),
                    onClick = {
                        selectedField = emptySet()
                        isSelecting = false
                    },
                )
                SimpleIconButton(
                    imageVector = vectorResource(Res.drawable.done),
                    contentDescription = stringResource(Res.string.ok),
                    onClick = {
                        openRouteSettings(createRouteDraft(selectedField, connection))
                    },
                )
            } else {
                SimpleIconButton(
                    imageVector = vectorResource(Res.drawable.add_road),
                    contentDescription = stringResource(Res.string.create_rule),
                    onClick = { isSelecting = true },
                )
                SimpleIconButton(
                    imageVector = vectorResource(Res.drawable.delete_forever),
                    contentDescription = stringResource(Res.string.close),
                    onClick = {
                        closeConnection(uuid)
                        onDismiss()
                    },
                )
            }
        }

        val listState = rememberLazyListState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item("connection") {
                    ConnectionGroupCard(isShaking = isSelecting) {
                        ConnectionField(
                            field = Res.string.connection_status,
                            isSelecting = isSelecting,
                        ) {
                            Text(
                                text = if (connection.isClosed) {
                                    stringResource(Res.string.connection_status_closed)
                                } else {
                                    stringResource(Res.string.connection_status_active)
                                },
                                color = if (connection.isClosed) {
                                    Color.Red
                                } else {
                                    Color.Green
                                },
                            )
                        }
                        ConnectionField(
                            field = Res.string.inbound,
                            isSelecting = isSelecting,
                        ) {
                            Text(connection.inbound)
                        }
                        connection.ipVersion?.let { ipVersion ->
                            ConnectionField(
                                field = Res.string.ip_version,
                                isSelecting = isSelecting,
                            ) {
                                Text(ipVersion.toString())
                            }
                        }
                        ConnectionField(
                            field = Res.string.network,
                            isSelecting = isSelecting,
                            isSelectable = true,
                            isSelected = ConnectionFields.NETWORK in selectedField,
                            onSelectedChange = { checked ->
                                selectField(ConnectionFields.NETWORK, checked)
                            },
                        ) {
                            Text(connection.network)
                        }
                        connection.protocol?.let { protocol ->
                            ConnectionField(
                                field = Res.string.protocol,
                                isSelecting = isSelecting,
                                isSelectable = true,
                                isSelected = ConnectionFields.PROTOCOL in selectedField,
                                onSelectedChange = { checked ->
                                    selectField(ConnectionFields.PROTOCOL, checked)
                                },
                            ) {
                                Text(protocol)
                            }
                        }
                    }
                }

                item("traffic") {
                    ConnectionGroupCard {
                        ConnectionField(
                            field = Res.string.upload,
                            isSelecting = isSelecting,
                        ) {
                            Text(Libbox.formatBytes(connection.uploadTotal))
                        }
                        ConnectionField(
                            field = Res.string.download,
                            isSelecting = isSelecting,
                        ) {
                            Text(Libbox.formatBytes(connection.downloadTotal))
                        }
                        ConnectionField(
                            field = Res.string.upload_speed,
                            isSelecting = isSelecting,
                        ) {
                            Text(
                                stringResource(
                                    Res.string.speed,
                                    Libbox.formatBytes(connection.uploadSpeed),
                                )
                            )
                        }
                        ConnectionField(
                            field = Res.string.download_speed,
                            isSelecting = isSelecting,
                        ) {
                            Text(
                                stringResource(
                                    Res.string.speed,
                                    Libbox.formatBytes(connection.downloadSpeed),
                                )
                            )
                        }
                        ConnectionField(
                            field = Res.string.start_time,
                            isSelecting = isSelecting,
                        ) {
                            Text(connection.startedAt)
                        }
                        connection.closedAt.emptyAsNull()?.let { closedAt ->
                            ConnectionField(
                                field = Res.string.closed_time,
                                isSelecting = isSelecting,
                            ) {
                                Text(closedAt)
                            }
                        }
                    }
                }

                item("address") {
                    ConnectionGroupCard(isShaking = isSelecting) {
                        ConnectionField(
                            field = Res.string.source_address,
                            isSelecting = isSelecting,
                            isSelectable = true,
                            isSelected = ConnectionFields.SOURCE in selectedField,
                            onSelectedChange = { checked ->
                                selectField(ConnectionFields.SOURCE, checked)
                            },
                        ) {
                            Text(connection.src)
                        }
                        ConnectionField(
                            field = Res.string.destination_address,
                            isSelecting = isSelecting,
                            isSelectable = true,
                            isSelected = ConnectionFields.DESTINATION in selectedField,
                            onSelectedChange = { checked ->
                                selectField(ConnectionFields.DESTINATION, checked)
                            },
                        ) {
                            Text(connection.dst)
                        }
                        if (connection.host.isNotBlank()) {
                            ConnectionField(
                                field = Res.string.http_host,
                                isSelecting = isSelecting,
                                isSelectable = true,
                                isSelected = ConnectionFields.HOST in selectedField,
                                onSelectedChange = { checked ->
                                    selectField(ConnectionFields.HOST, checked)
                                },
                            ) {
                                Text(connection.host)
                            }
                        }
                    }
                }

                item("route") {
                    ConnectionGroupCard {
                        ConnectionField(
                            field = Res.string.outbound_rule,
                            isSelecting = isSelecting,
                        ) {
                            Text(connection.matchedRule)
                        }
                        ConnectionField(
                            field = Res.string.outbound,
                            isSelecting = isSelecting,
                        ) {
                            Text(connection.outbound)
                        }
                        ConnectionField(
                            field = Res.string.chain,
                            isSelecting = isSelecting,
                        ) {
                            Text(connection.chain)
                        }
                    }
                }

                val process = connection.processes?.firstOrNull()
                val processText = buildProcessText(connection.uid, process)
                if (processText.isNotEmpty()) item("process") {
                    val uid = connection.uid
                    var processInfo by remember { mutableStateOf<ProcessInfo?>(null) }
                    LaunchedEffect(Unit) {
                        processInfo = resolveProcessInfo(process, uid)
                    }
                    val processLabel = processInfo?.label.blankAsNull()
                    val openProcessAppInfo = rememberOpenProcessAppInfo(process)
                    ConnectionGroupCard(isShaking = isSelecting) {
                        ConnectionField(
                            field = Res.string.process,
                            isSelecting = isSelecting,
                            isSelectable = true,
                            isSelected = ConnectionFields.PROCESS in selectedField,
                            onLongClick = openProcessAppInfo,
                            onSelectedChange = { checked ->
                                selectField(ConnectionFields.PROCESS, checked)
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (processLabel != null) {
                                        Text(processLabel)
                                    }
                                    Text(
                                        text = processText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                processInfo?.icon?.let { icon ->
                                    ProcessIcon(
                                        icon = icon,
                                        contentDescription = processLabel
                                            ?: processInfo?.packageName,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }
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

private fun buildProcessText(uid: Int, process: String?): String {
    var text = process.orEmpty()
    if (uid >= 0) {
        text = "[$uid] $text"
    }
    return text
}

@Composable
private fun ConnectionGroupCard(
    modifier: Modifier = Modifier,
    isShaking: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(80),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rotation",
    )
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationZ = if (isShaking) rotation else 0f
            },
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content,
        )
    }
}

private val FIELD_CHECKBOX_SIZE = 20.dp

@Composable
private fun ConnectionField(
    field: StringResource,
    modifier: Modifier = Modifier,
    isSelecting: Boolean = false,
    isSelectable: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onSelectedChange: (Boolean) -> Unit = {},
    value: @Composable () -> Unit,
) {
    val onClick = {
        if (isSelecting && isSelectable) {
            onSelectedChange(!isSelected)
        }
    }
    val clickModifier = when {
        onLongClick != null -> Modifier.platformCombinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        )

        isSelecting && isSelectable -> Modifier.clickable(onClick = onClick)

        else -> Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelecting) {
            if (isSelectable) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectedChange,
                    modifier = Modifier.size(FIELD_CHECKBOX_SIZE),
                )
            } else {
                Spacer(Modifier.size(FIELD_CHECKBOX_SIZE))
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = stringResource(field),
            modifier = Modifier.width(104.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        SelectionContainer(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                value()
            }
        }
    }
}

private fun createRouteDraft(
    fields: Set<ConnectionFields>,
    connection: ConnectionDetailState,
): RouteSettingsUiState {
    var domains = ""
    var ip = ""
    var port = ""
    var source = ""
    var sourcePort = ""
    var network = emptySet<String>()
    var protocol = emptySet<String>()
    var packages = emptySet<String>()

    for (field in fields) {
        when (field) {
            ConnectionFields.HOST -> {
                if (connection.host.isNotBlank()) {
                    domains = "full:${connection.host}"
                }
            }

            ConnectionFields.DESTINATION -> {
                val (dstIp, dstPort) = parseAddress(connection.dst)
                if (dstIp.isNotBlank()) ip = dstIp
                if (dstPort.isNotBlank()) port = dstPort
            }

            ConnectionFields.SOURCE -> {
                val (srcIp, srcPort) = parseAddress(connection.src)
                if (srcIp.isNotBlank()) source = srcIp
                if (srcPort.isNotBlank()) sourcePort = srcPort
            }

            ConnectionFields.NETWORK -> {
                if (connection.network.isNotBlank()) {
                    network = setOf(connection.network)
                }
            }

            ConnectionFields.PROTOCOL -> {
                if (connection.protocol != null) {
                    protocol = setOf(connection.protocol)
                }
            }

            ConnectionFields.PROCESS -> {
                if (connection.processes?.isNotEmpty() == true) {
                    packages = connection.processes.toSet()
                }
            }

        }
    }

    return RouteSettingsUiState(
        name = connection.uuid,
        action = SingBoxOptions.ACTION_ROUTE,
        domains = domains,
        ip = ip,
        port = port,
        source = source,
        sourcePort = sourcePort,
        network = network,
        protocol = protocol,
        packages = packages,
    )
}

private fun parseAddress(address: String): Pair<String, String> {
    if (address.isBlank()) return "" to ""
    return if (address.startsWith("[")) {
        // IPv6
        val closeBracket = address.indexOf(']')
        if (closeBracket == -1) return address to ""
        val ip = address.substring(1, closeBracket)
        val port = if (closeBracket + 2 < address.length) {
            address.substring(closeBracket + 2)
        } else ""
        ip to port
    } else {
        // IPv4
        val lastColon = address.lastIndexOf(':')
        if (lastColon == -1) return address to ""
        address.take(lastColon) to address.substring(lastColon + 1)
    }
}

@Preview
@Composable
private fun PreviewConnectionDetailSheet() {
    val connection = remember {
        ConnectionDetailState(
            uuid = "6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8",
            inbound = "mixed-in",
            ipVersion = 0,
            network = "tcp",
            uploadTotal = 114514,
            downloadTotal = 1919810,
            uploadSpeed = 1024,
            downloadSpeed = 40960,
            startedAt = "2026-12-31 23:59:59",
            src = "127.0.0.1:54321",
            dst = "example.com:443",
            host = "example.com",
            matchedRule = "final",
            outbound = "selector",
            chain = "selector => 🇭🇰",
            protocol = "tls",
            processes = listOf("io.throneproj.thronem"),
            uid = 8888,
        )
    }
    ConnectionDetailSheet(
        connection = connection,
        resolveProcessInfo = { _, _ -> null },
        closeConnection = {},
        onDismiss = {},
        openRouteSettings = {},
    )
}

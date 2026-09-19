package io.throneproj.thronem.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.throneproj.thronem.compose.DropDownSelector
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.core.CoreClient
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.outbound
import io.throneproj.thronem.resources.outbound_default
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

private const val DEFAULT_OUTBOUND_TAG = ""

@Composable
internal fun OutboundSelector(
    selectedTag: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coreClient = remember { GlobalContext.get().get<CoreClient>() }
    var tags by remember { mutableStateOf(listOf(DEFAULT_OUTBOUND_TAG)) }
    LaunchedEffect(coreClient) {
        tags = listOf(DEFAULT_OUTBOUND_TAG)
        try {
            coreClient.subscribeOutbounds().collect { list ->
                tags = listOf(DEFAULT_OUTBOUND_TAG) + list.map { it.tag }
            }
        } catch (e: Exception) {
            Logs.w("subscribe outbounds", e)
        }
    }

    val defaultLabel = stringResource(Res.string.outbound_default)
    DropDownSelector(
        modifier = modifier,
        label = { Text(stringResource(Res.string.outbound)) },
        value = selectedTag,
        values = tags,
        onValueChange = onSelect,
        displayValue = { tag -> tag.ifEmpty { defaultLabel } },
    )
}

@file:OptIn(ExperimentalLayoutApi::class)

package io.throneproj.thronem.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import io.throneproj.thronem.compose.material3.Surface
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.throneproj.thronem.compose.theme.AppTheme
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ProxySet
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.permission.LocalPermissionPlatform
import io.throneproj.thronem.permission.rememberAndroidPermissionPlatform
import io.throneproj.thronem.repository.resolveRepository
import kotlinx.coroutines.flow.first

class SwitchActivity : ComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val platformPermission = rememberAndroidPermissionPlatform()
            CompositionLocalProvider(
                LocalPermissionPlatform provides platformPermission,
            ) {
                AppTheme {
                    val dismissInteractionSource = remember { MutableInteractionSource() }
                    val selectedSetId = DataStore.selectedProxySet.getBlocking()
                    val sets = kotlinx.coroutines.runBlocking {
                        ThroneDatabase.proxySetDao.allSets().first()
                    }
                    val bottomPadding = WindowInsets.navigationBarsIgnoringVisibility
                        .asPaddingValues()
                        .calculateBottomPadding()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = dismissInteractionSource,
                                    indication = null,
                                    onClick = ::finish,
                                ),
                        )

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(0.75f),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    top = 16.dp,
                                    bottom = bottomPadding + 16.dp,
                                ),
                            ) {
                                items(sets, key = { it.id }) { set ->
                                    ProxySetPickerCard(
                                        set = set,
                                        isSelected = set.id == selectedSetId,
                                        onSelect = {
                                            returnProxySet(set.id)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun returnProxySet(setId: Long) {
        DataStore.selectedProxySet.setBlocking(setId)
        resolveRepository().reloadService()
        finish()
    }
}

@Composable
private fun ProxySetPickerCard(
    set: ProxySet,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    OutlinedCard(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.elevatedCardElevation(),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary,
            )
        } else {
            CardDefaults.outlinedCardBorder()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = set.displayName(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = set.displayType(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

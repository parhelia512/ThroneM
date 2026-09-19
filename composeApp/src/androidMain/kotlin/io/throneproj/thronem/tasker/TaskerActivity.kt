/******************************************************************************
 * Copyright (C) 2022 by nekohasekai <contact-git@sekai.icu>                  *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                       *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                            *
 ******************************************************************************/

package io.throneproj.thronem.tasker

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.AlertDialog
import io.throneproj.thronem.compose.material3.Icon
import androidx.compose.material3.Scaffold
import io.throneproj.thronem.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import io.throneproj.thronem.compose.CapsuleActionButton
import io.throneproj.thronem.compose.CapsuleTopBar
import io.throneproj.thronem.compose.ListPreference
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.paddingExceptBottom
import io.throneproj.thronem.compose.theme.AppTheme
import io.throneproj.thronem.database.ProfileManager
import io.throneproj.thronem.ktx.intListN
import io.throneproj.thronem.permission.LocalPermissionPlatform
import io.throneproj.thronem.permission.rememberAndroidPermissionPlatform
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.apply
import io.throneproj.thronem.resources.close
import io.throneproj.thronem.resources.done
import io.throneproj.thronem.resources.layers
import io.throneproj.thronem.resources.menu_configuration
import io.throneproj.thronem.resources.no
import io.throneproj.thronem.resources.not_set
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.question_mark
import io.throneproj.thronem.resources.route_profile
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.tasker_action
import io.throneproj.thronem.resources.tasker_action_start_service
import io.throneproj.thronem.resources.tasker_action_stop_service
import io.throneproj.thronem.resources.tasker_blurb_start_profile
import io.throneproj.thronem.resources.tasker_settings
import io.throneproj.thronem.resources.tasker_start_current_profile
import io.throneproj.thronem.resources.unsaved_changes_prompt
import io.throneproj.thronem.ui.ComposeActivity
import io.throneproj.thronem.ui.configuration.ProfileSelectSheet
import kotlinx.coroutines.runBlocking
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

class TaskerActivity : ComposeActivity() {

    private val viewModel by viewModels<TaskerActivityViewModel>()
    private lateinit var settings: TaskerBundle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            reloadIntent(intent)
        }

        setContent {
            val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
            var showBackAlert by remember { mutableStateOf(false) }
            BackHandler(enabled = isDirty) {
                showBackAlert = true
            }
            var profileSelectSession by remember { mutableStateOf<ProfileSelectSession?>(null) }

            val windowInsets = WindowInsets.safeDrawing
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

            val platformPermission = rememberAndroidPermissionPlatform()

            CompositionLocalProvider(
                LocalPermissionPlatform provides platformPermission,
            ) {
                AppTheme {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            CapsuleTopBar(
                                title = { Text(stringResource(Res.string.tasker_settings)) },
                                navigationIcon = {
                                    SimpleIconButton(
                                        imageVector = vectorResource(Res.drawable.close),
                                        contentDescription = stringResource(Res.string.close),
                                        onClick = {
                                            onBackPressedDispatcher.onBackPressed()
                                        },
                                    )
                                },
                                actions = {
                                    CapsuleActionButton {
                                        SimpleIconButton(
                                            imageVector = vectorResource(Res.drawable.done),
                                            contentDescription = stringResource(Res.string.apply),
                                            onClick = {
                                                saveAndExit()
                                            },
                                        )
                                    }
                                },
                                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                                scrollBehavior = scrollBehavior,
                            )
                        },
                    ) { innerPadding ->
                        Column(modifier = Modifier.paddingExceptBottom(innerPadding)) {
                            TaskerPreference(
                                onOpenProfileSelect = { preSelected, onSelected ->
                                    profileSelectSession =
                                        ProfileSelectSession(preSelected, onSelected)
                                },
                            )

                            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                        }
                    }

                    if (showBackAlert) AlertDialog(
                        onDismissRequest = { showBackAlert = false },
                        confirmButton = {
                            TextButton(stringResource(Res.string.ok)) {
                                saveAndExit()
                            }
                        },
                        dismissButton = {
                            TextButton(stringResource(Res.string.no)) {
                                finish()
                            }
                        },
                        icon = { Icon(vectorResource(Res.drawable.question_mark), null) },
                        title = { Text(stringResource(Res.string.unsaved_changes_prompt)) },
                    )

                    val session = profileSelectSession
                    if (session != null) {
                        ProfileSelectSheet(
                            preSelected = session.preSelected,
                            onDismiss = { profileSelectSession = null },
                            onSelected = { id ->
                                session.onSelected(id)
                                profileSelectSession = null
                            },
                        )
                    }
                }
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        reloadIntent(intent)
    }

    private fun reloadIntent(intent: Intent) {
        settings = TaskerBundle.fromIntent(intent)
        viewModel.loadFromSetting(settings.action, settings.profileId)
    }

    private fun saveAndExit() {
        setResult(RESULT_OK, buildIntent())
        finish()
    }

    private fun buildIntent(): Intent {
        val uiState = viewModel.uiState.value
        val action = uiState.action
        val profileId = uiState.profileID
        settings.action = action
        settings.profileId = profileId
        val blurb = runBlocking {
            when (action) {
                TaskerBundle.ACTION_START -> {
                    val entity = if (profileId > 0) {
                        ProfileManager.getProfile(profileId)
                    } else {
                        null
                    }
                    if (entity != null) {
                        resolveRepository().getString(
                            Res.string.tasker_blurb_start_profile, entity.displayName(),
                        )
                    } else {
                        resolveRepository().getString(Res.string.tasker_action_start_service)
                    }
                }

                TaskerBundle.ACTION_STOP -> {
                    resolveRepository().getString(Res.string.tasker_action_stop_service)
                }

                else -> ""
            }
        }
        return Intent().apply {
            putExtra(TaskerBundle.EXTRA_BUNDLE, settings.bundle)
            putExtra(TaskerBundle.EXTRA_STRING_BLURB, blurb)
        }
    }

    @Composable
    private fun TaskerPreference(
        onOpenProfileSelect: (preSelected: Long?, onSelected: (Long) -> Unit) -> Unit,
    ) {
        ProvidePreferenceLocals {
            val uiState by viewModel.uiState.collectAsState()

            fun actionText(action: Int) = when (action) {
                TaskerBundle.ACTION_START -> Res.string.tasker_action_start_service
                TaskerBundle.ACTION_STOP -> Res.string.tasker_action_stop_service
                else -> error("impossible")
            }
            ListPreference(
                value = uiState.action,
                onValueChange = { viewModel.setAction(it) },
                values = intListN(2),
                title = { Text(stringResource(Res.string.tasker_action)) },
                icon = { Icon(vectorResource(Res.drawable.layers), null) },
                summary = { Text(stringResource(actionText(uiState.action))) },
                type = ListPreferenceType.DROPDOWN_MENU,
                valueToText = {
                    val text = runBlocking { resolveRepository().getString(actionText(it)) }
                    AnnotatedString(text)
                },
            )

            ListPreference(
                value = uiState.profileID,
                onValueChange = {
                    if (it == -1L) {
                        viewModel.setProfileID(it)
                    } else {
                        onOpenProfileSelect(uiState.profileID.takeIf { id -> id > 0 }) { id ->
                            viewModel.setProfileID(id)
                        }
                    }
                },
                values = listOf(-1L, 0L),
                title = { Text(stringResource(Res.string.menu_configuration)) },
                enabled = uiState.action == TaskerBundle.ACTION_START,
                icon = { Icon(vectorResource(Res.drawable.router), null) },
                summary = {
                    val notSet = stringResource(Res.string.not_set)
                    val summary by produceState(notSet, uiState.profileID) {
                        value = ProfileManager.getProfile(uiState.profileID)?.displayName()
                            ?: notSet
                    }
                    Text(summary)
                },
                type = ListPreferenceType.DROPDOWN_MENU,
                valueToText = {
                    val id = if (it == -1L) {
                        Res.string.tasker_start_current_profile
                    } else {
                        Res.string.route_profile
                    }
                    val text = runBlocking { resolveRepository().getString(id) }
                    AnnotatedString(text)
                },
            )
        }
    }

}

private data class ProfileSelectSession(
    val preSelected: Long?,
    val onSelected: (Long) -> Unit,
)

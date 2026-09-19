package io.throneproj.thronem.ui

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.throneproj.thronem.GroupType
import io.throneproj.thronem.Key
import io.throneproj.thronem.bg.DeepLinkDispatcher
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ProxyGroup
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.fmt.AbstractBean
import io.throneproj.thronem.group.GroupUpdateResult
import io.throneproj.thronem.group.GroupUpdateWarning
import io.throneproj.thronem.group.GroupUpdater
import io.throneproj.thronem.group.RawUpdater
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.SubscriptionFoundException
import io.throneproj.thronem.ktx.readableMessage
import io.throneproj.thronem.repository.Repository
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class AlertButton(
    val label: StringOrRes,
    val onClick: () -> Unit,
)

@Immutable
class MainAlertDialogEvent(
    val title: StringOrRes,
    val message: StringOrRes,
    val confirmButton: AlertButton,
    val dismissButton: AlertButton? = null,
    val onDismiss: (() -> Unit)? = null,
)

@Stable
class MainViewModel(
    private val repository: Repository = resolveRepository(),
    private val importLinkInteractor: ImportLinkInteractor = ImportLinkInteractor(),
    private val snackbar: SnackbarEmitter = SnackbarEmitter(),
) : AutoCloseable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val dialogEvent: SharedFlow<MainAlertDialogEvent>
        field = MutableSharedFlow<MainAlertDialogEvent>()

    private fun alertDialog(
        message: StringOrRes,
        title: StringOrRes = StringOrRes.Res(Res.string.error_title),
    ) = MainAlertDialogEvent(
        title = title,
        message = message,
        confirmButton = AlertButton(StringOrRes.Res(Res.string.ok)) {},
    )

    init {
        scope.launch {
            DeepLinkDispatcher.flow.collect { link ->
                importFromUri(link)
            }
        }

        scope.launch {
            DataStore.configurationStore.keysFlow(
                Key.PROXY_APPS,
                Key.BYPASS_MODE,
                Key.PACKAGES,
            ).collectLatest {
                if (DataStore.serviceState.canStop) {
                    snackbar.show(
                        StringOrRes.Res(Res.string.need_reload),
                        StringOrRes.Res(Res.string.apply),
                    ) { result ->
                        if (result == SnackbarResult.ActionPerformed) {
                            repository.reloadService()
                        }
                    }
                }
            }
        }
    }

    fun importFromUri(uri: String) = scope.launch {
        val preview = try {
            importLinkInteractor.parseUri(uri)
        } catch (e: Exception) {
            dialogEvent.emit(alertDialog(StringOrRes.Direct(e.readableMessage)))
            return@launch
        }
        when (preview) {
            ImportLinkPreview.Ignore -> Unit
            is ImportLinkPreview.Subscription -> showImportSubscriptionDialog(preview.group)
            is ImportLinkPreview.Profiles -> showImportProfileDialog(preview.proxies)
        }
    }

    fun importSubscription(uri: String) = scope.launch {
        val group = try {
            importLinkInteractor.parseSubscription(uri)
        } catch (e: Exception) {
            dialogEvent.emit(alertDialog(StringOrRes.Direct(e.readableMessage)))
            return@launch
        } ?: return@launch
        showImportSubscriptionDialog(group)
    }

    private suspend fun showImportSubscriptionDialog(group: ProxyGroup) {
        val detail = group.name + "\n" + group.subscription?.link + "\n" + group.subscription?.token
        dialogEvent.emit(
            MainAlertDialogEvent(
                title = StringOrRes.Res(Res.string.subscription_import),
                message = StringOrRes.ResWithParams(Res.string.subscription_import_message, detail),
                confirmButton = AlertButton(StringOrRes.Res(Res.string.ok)) {
                    scope.launch(Dispatchers.Default) {
                        val createdGroup = withContext(Dispatchers.IO) {
                            importLinkInteractor.createSubscriptionGroup(group)
                        }
                        DataStore.selectedGroup.set(createdGroup.id)
                        performGroupUpdate(createdGroup, true)
                    }
                },
                dismissButton = AlertButton(StringOrRes.Res(Res.string.cancel)) {},
            ),
        )
    }

    private suspend fun showImportProfileDialog(profiles: List<AbstractBean>) {
        if (profiles.isEmpty()) {
            dialogEvent.emit(alertDialog(StringOrRes.Res(Res.string.no_proxies_found)))
            return
        }
        dialogEvent.emit(
            MainAlertDialogEvent(
                title = StringOrRes.Res(Res.string.profile_import),
                message = StringOrRes.ResWithParams(
                    Res.string.profile_import_message,
                    profiles.joinToString("\n") { it.displayName() },
                ),
                confirmButton = AlertButton(StringOrRes.Res(Res.string.ok)) {
                    scope.launch(Dispatchers.IO) {
                        importProfile(profiles)
                    }
                },
                dismissButton = AlertButton(StringOrRes.Res(Res.string.cancel)) {},
            ),
        )
    }

    fun parseProxy(text: String?) = scope.launch {
        if (text.isNullOrBlank()) {
            snackbar.show(StringOrRes.Res(Res.string.clipboard_empty))
            return@launch
        }
        when (text.substringBefore("://", "").lowercase()) {
            "http", "https" -> dialogEvent.emit(
                MainAlertDialogEvent(
                    title = StringOrRes.Res(Res.string.import_url),
                    message = StringOrRes.Res(Res.string.import_http_url),
                    confirmButton = AlertButton(StringOrRes.Res(Res.string.subscription_import)) {
                        importSubscription(text)
                    },
                    dismissButton = AlertButton(StringOrRes.Res(Res.string.profile_import)) {
                        scope.launch {
                            parseSubscription(text)
                        }
                    },
                ),
            )

            else -> parseSubscription(text)
        }
    }

    private suspend fun parseSubscription(text: String) {
        try {
            val proxies = RawUpdater.parseRaw(text)
            if (proxies.isNullOrEmpty()) {
                snackbar.show(StringOrRes.Res(Res.string.no_proxies_found_in_clipboard))
            } else {
                importProfile(proxies)
            }
        } catch (e: SubscriptionFoundException) {
            importSubscription(e.link)
        } catch (e: Exception) {
            Logs.w(e)
            snackbar.show(StringOrRes.Direct(e.readableMessage))
        }
    }

    suspend fun importProfile(proxies: List<AbstractBean>) {
        val importedCount = importLinkInteractor.importProfiles(proxies)
        snackbar.show(
            StringOrRes.PluralsRes(
                Res.plurals.added,
                importedCount,
                importedCount,
            ),
        )
    }

    fun updateSubscriptionGroup(group: ProxyGroup) = scope.launch(Dispatchers.Default) {
        performGroupUpdate(group, true)
    }

    fun updateAllSubscriptionGroups() = scope.launch(Dispatchers.Default) {
        val groups = withContext(Dispatchers.IO) {
            ThroneDatabase.groupDao.allGroups().first()
                .filter { it.type == GroupType.SUBSCRIPTION }
        }
        for (group in groups) {
            performGroupUpdate(group, true)
        }
    }

    suspend fun confirm(message: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        dialogEvent.emit(
            MainAlertDialogEvent(
                title = StringOrRes.Res(Res.string.confirm),
                message = StringOrRes.Direct(message),
                confirmButton = AlertButton(StringOrRes.Res(Res.string.ok)) {
                    deferred.complete(true)
                },
                dismissButton = AlertButton(StringOrRes.Res(Res.string.cancel)) {
                    deferred.complete(false)
                },
                onDismiss = {
                    deferred.complete(false)
                },
            ),
        )
        return deferred.await()
    }

    private suspend fun performGroupUpdate(group: ProxyGroup, byUser: Boolean) {
        var allowDisconnectedUpdate = false
        while (true) {
            when (val result = GroupUpdater.executeUpdate(group, byUser, allowDisconnectedUpdate)) {
                is GroupUpdateResult.AlreadyUpdating -> return

                is GroupUpdateResult.ConfirmRequired -> {
                    if (!confirm(result.message)) return
                    allowDisconnectedUpdate = true
                }

                else -> {
                    presentGroupUpdateResult(result)
                    return
                }
            }
        }
    }

    private suspend fun presentGroupUpdateResult(result: GroupUpdateResult) {
        presentGroupUpdateWarnings(result.warningsOrEmpty())
        when (result) {
            is GroupUpdateResult.Success -> presentGroupUpdateSuccess(result)
            is GroupUpdateResult.Failure -> {
                dialogEvent.emit(alertDialog(StringOrRes.Direct("${result.group.name}: ${result.message}")))
            }

            else -> Unit
        }
    }

    private suspend fun presentGroupUpdateSuccess(result: GroupUpdateResult.Success) {
        val changed = result.diff.changed
        if (changed == 0) {
            snackbar.show(
                StringOrRes.ResWithParams(Res.string.group_no_difference, result.group.displayName()),
            )
            return
        }
        if (!result.byUser) {
            snackbar.show(
                StringOrRes.PluralsRes(
                    Res.plurals.group_updated,
                    changed,
                    result.group.displayName(),
                    changed,
                ),
            )
            return
        }

        val parts = buildList {
            add(
                StringOrRes.PluralsRes(
                    Res.plurals.group_updated,
                    changed,
                    result.group.displayName(),
                    changed,
                ),
            )
            if (result.diff.added.isNotEmpty()) {
                add(StringOrRes.ResWithParams(Res.string.group_added, result.diff.added.joinToString("\n")))
            }
            if (result.diff.updated.isNotEmpty()) {
                add(
                    StringOrRes.ResWithParams(
                        Res.string.group_changed,
                        result.diff.updated.entries.joinToString("\n") { "${it.key} -> ${it.value}" },
                    ),
                )
            }
            if (result.diff.deleted.isNotEmpty()) {
                add(
                    StringOrRes.ResWithParams(
                        Res.string.group_deleted,
                        result.diff.deleted.joinToString("\n"),
                    ),
                )
            }
            if (result.diff.duplicate.isNotEmpty()) {
                add(
                    StringOrRes.ResWithParams(
                        Res.string.group_duplicate,
                        result.diff.duplicate.joinToString("\n"),
                    ),
                )
            }
        }
        dialogEvent.emit(
            alertDialog(
                message = StringOrRes.Compound(parts),
                title = StringOrRes.ResWithParams(Res.string.group_diff, result.group.displayName()),
            ),
        )
    }

    private fun presentGroupUpdateWarnings(warnings: List<GroupUpdateWarning>) {
        for ((group, message) in warnings) {
            snackbar.show(
                StringOrRes.Compound(
                    parts = listOf(
                        StringOrRes.Direct(group),
                        StringOrRes.Direct(message),
                    ),
                    separator = ": ",
                ),
            )
        }
    }

    override fun close() {
        scope.cancel()
    }
}

private fun GroupUpdateResult.warningsOrEmpty(): List<GroupUpdateWarning> = when (this) {
    is GroupUpdateResult.Success -> warnings
    is GroupUpdateResult.Failure -> warnings
    else -> emptyList()
}

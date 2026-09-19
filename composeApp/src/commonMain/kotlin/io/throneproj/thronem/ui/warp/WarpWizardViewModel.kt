package io.throneproj.thronem.ui.warp

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ProfileManager
import io.throneproj.thronem.fmt.AbstractBean
import io.throneproj.thronem.fmt.masque.MasqueBean
import io.throneproj.thronem.fmt.wireguard.WireGuardBean
import io.throneproj.thronem.fmt.wireguard.ensureCidr
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.applyDefaultValues
import io.throneproj.thronem.warp.MasqueAccount
import io.throneproj.thronem.warp.WarpAccount
import io.throneproj.thronem.warp.WarpClient
import io.throneproj.thronem.warp.WarpEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

const val WARP_TRANSPORT_WIREGUARD = "wireguard"
const val WARP_TRANSPORT_MASQUE = "masque"

@Immutable
internal data class WarpWizardUiState(
    val license: String = "",
    val endpoint: String = "",
    val obfuscate: Boolean = false,
    val masqueradeProtocol: String = "quic",
    val masqueradeDomain: String = "",
    val masqueradeBrowser: String = "chrome",
    val junkCount: Int = 4,
    val junkMin: Int = 40,
    val junkMax: Int = 70,
    val includeReserved: Boolean = true,
    val keepalive: Int = 25,
    val forceNew: Boolean = false,
    val busy: Boolean = false,
    /** null = no result yet; true/false drives the message styling on screen. */
    val success: Boolean? = null,
    val message: String = "",
    // §130 — MASQUE transport ("wireguard" | "masque").
    val transport: String = WARP_TRANSPORT_WIREGUARD,
    /** §393 — HTTP version of the generated node: auto | h3 | h2. */
    val masqueNetwork: String = "auto",
    val masqueSni: String = "",
    /** §305 — manual endpoint override; blank = server from the registration. */
    val masqueEndpointIp: String = "",
    val masquePort: String = "",
    /** Client-side tuning; 0 = core default. */
    val masqueIdleMinutes: Int = 0,
    val masqueKeepAliveSeconds: Int = 0,
)

@Stable
internal class WarpWizardViewModel : ViewModel() {

    val uiState: StateFlow<WarpWizardUiState>
        field = MutableStateFlow(
            WarpWizardUiState(
                masqueradeDomain = WarpEndpoints.randomSni(),
                masqueSni = WarpEndpoints.randomMasqueSni(),
            ),
        )

    fun setLicense(license: String) = update { it.copy(license = license) }

    fun setEndpoint(endpoint: String) = update { it.copy(endpoint = endpoint) }

    fun setObfuscate(obfuscate: Boolean) = update {
        it.copy(
            obfuscate = obfuscate,
            // Default by the toggle; the user can still override afterwards.
            includeReserved = !obfuscate,
            masqueradeDomain = if (it.masqueradeDomain.isBlank()) {
                WarpEndpoints.randomSni()
            } else {
                it.masqueradeDomain
            },
        )
    }

    fun setMasqueradeProtocol(protocol: String) = update { it.copy(masqueradeProtocol = protocol) }

    fun setMasqueradeDomain(domain: String) = update { it.copy(masqueradeDomain = domain) }

    fun setMasqueradeBrowser(browser: String) = update { it.copy(masqueradeBrowser = browser) }

    fun rerollMasqueradeDomain() = update { it.copy(masqueradeDomain = WarpEndpoints.randomSni()) }

    fun rerollEndpoint() = update { it.copy(endpoint = WarpEndpoints.randomEndpoint()) }

    fun setJunkCount(count: Int) = update { it.copy(junkCount = count) }

    fun setJunkMin(min: Int) = update { it.copy(junkMin = min) }

    fun setJunkMax(max: Int) = update { it.copy(junkMax = max) }

    fun setIncludeReserved(include: Boolean) = update { it.copy(includeReserved = include) }

    fun setKeepalive(keepalive: Int) = update { it.copy(keepalive = keepalive) }

    fun setForceNew(forceNew: Boolean) = update { it.copy(forceNew = forceNew) }

    fun setTransport(transport: String) = update { it.copy(transport = transport) }

    fun setMasqueNetwork(network: String) = update {
        it.copy(
            masqueNetwork = network,
            // §305 — the h3 and h2 port sets differ in LxBox; here they are the
            // same, but a stale out-of-pool port still resets to the first one.
            masquePort = it.masquePort.takeIf { port ->
                port.toIntOrNull() in WarpEndpoints.MASQUE_PORTS
            } ?: WarpEndpoints.MASQUE_PORTS.first().toString(),
        )
    }

    fun setMasqueSni(sni: String) = update { it.copy(masqueSni = sni) }

    fun rerollMasqueSni() = update { it.copy(masqueSni = WarpEndpoints.randomMasqueSni()) }

    fun setMasqueEndpointIp(ip: String) = update { it.copy(masqueEndpointIp = ip) }

    fun rerollMasqueEndpoint() = update {
        it.copy(
            masqueEndpointIp = WarpEndpoints.randomMasqueIp(it.masqueNetwork).orEmpty(),
            masquePort = WarpEndpoints.randomMasquePort().toString(),
        )
    }

    fun setMasquePort(port: String) = update { it.copy(masquePort = port) }

    fun setMasqueIdleMinutes(minutes: Int) = update { it.copy(masqueIdleMinutes = minutes) }

    fun setMasqueKeepAliveSeconds(seconds: Int) = update { it.copy(masqueKeepAliveSeconds = seconds) }

    /**
     * Registers (or reuses the cached) WARP account — WireGuard or MASQUE —
     * and adds the generated profile to the current group. [onSuccess] pops
     * the screen.
     */
    fun register(onSuccess: () -> Unit) {
        if (uiState.value.busy) return
        update { it.copy(busy = true, success = null, message = "") }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = uiState.value
                if (state.transport == WARP_TRANSPORT_MASQUE) {
                    registerMasque(state, onSuccess)
                } else {
                    registerWireGuard(state, onSuccess)
                }
            } catch (e: Exception) {
                Logs.w("WARP registration failed", e)
                withContext(Dispatchers.Main) {
                    update {
                        it.copy(busy = false, success = false, message = e.message ?: "$e")
                    }
                }
            }
        }
    }

    private suspend fun registerWireGuard(state: WarpWizardUiState, onSuccess: () -> Unit) {
        val cached = if (state.forceNew) {
            null
        } else {
            DataStore.warpAccount.getOrNull()?.let { WarpAccount.fromJson(it) }
        }
        val account = cached ?: run {
            val endpoint = state.endpoint.trim().ifBlank { WarpEndpoints.DEFAULT_ENDPOINT }
            val client = WarpClient()
            client.register(
                nowIso8601 = Clock.System.now().toString(),
                licenseKey = state.license.trim().ifBlank { null },
                endpoint = endpoint,
                obfuscate = state.obfuscate,
                randomEndpoint = if (state.obfuscate &&
                    endpoint == WarpEndpoints.DEFAULT_ENDPOINT
                ) {
                    WarpEndpoints.randomEndpoint()
                } else {
                    null
                },
            )
        }
        // Cache for the next run ("force new" clears it by skipping the read).
        DataStore.warpAccount.set(account.toJson())

        val bean = account.toWireGuardBean(state)
        finishRegistration(bean, if (account.warpPlus) "WARP+" else "WARP", onSuccess)
    }

    /** §130 — MASQUE registration path: cached account or ECDSA enroll. */
    private suspend fun registerMasque(state: WarpWizardUiState, onSuccess: () -> Unit) {
        val sni = state.masqueSni.trim()
        val idleTimeout = state.masqueIdleMinutes.takeIf { it > 0 }?.let { "${it}m" }.orEmpty()
        val keepAlive = state.masqueKeepAliveSeconds.takeIf { it > 0 }?.let { "${it}s" }.orEmpty()

        val cached = if (state.forceNew) {
            null
        } else {
            DataStore.masqueAccount.getOrNull()?.let { MasqueAccount.fromJson(it) }
        }
        val account = (cached ?: WarpClient().registerMasque(
            nowIso8601 = Clock.System.now().toString(),
            sni = sni,
            idleTimeout = idleTimeout,
            keepAlive = keepAlive,
        )).copyWith(sni = sni, idleTimeout = idleTimeout, keepAlive = keepAlive)
        // Cache for the next run; the endpoint override below must NOT leak
        // into the cache — it keeps the canonical registration server.
        DataStore.masqueAccount.set(account.toJson())

        val bean = account.toMasqueBean(state)
        finishRegistration(bean, "WARP (MASQUE)", onSuccess)
    }

    private suspend fun finishRegistration(bean: AbstractBean, name: String, onSuccess: () -> Unit) {
        val groupId = DataStore.selectedGroupForImport()
        DataStore.selectedGroup.set(groupId)
        ProfileManager.createProfile(groupId, bean)
        withContext(Dispatchers.Main) {
            update {
                it.copy(busy = false, success = true, message = name)
            }
            onSuccess()
        }
    }

    private fun update(reducer: (WarpWizardUiState) -> WarpWizardUiState) {
        uiState.update(reducer)
    }
}

/** Builds the WireGuard profile from the registration result plus the wizard options. */
internal fun WarpAccount.toWireGuardBean(state: WarpWizardUiState): WireGuardBean =
    WireGuardBean().applyDefaultValues().apply {
        name = when {
            warpPlus && state.obfuscate -> "WARP+ (AWG)"
            warpPlus -> "WARP+"
            state.obfuscate -> "WARP (AWG)"
            else -> "WARP"
        }
        localAddress = buildString {
            append(clientV4.ensureCidr())
            if (clientV6.isNotBlank()) {
                append('\n').append(clientV6.ensureCidr())
            }
        }
        privateKey = privKey
        publicKey = peerPub
        // Peer endpoint (host:port) from the registration response or user
        // input; without it the bean stays at AbstractBean's 127.0.0.1 default.
        serverAddress = endpoint.substringBeforeLast(":").removeSurrounding("[", "]")
        serverPort = endpoint.substringAfterLast(":").toIntOrNull()
            ?: WarpEndpoints.DEFAULT_PEER_PORT
        mtu = WarpEndpoints.WARP_MTU
        if (state.includeReserved && clientId.isNotBlank()) {
            reserved = clientId
        }
        persistentKeepaliveInterval = state.keepalive
        if (state.obfuscate) {
            // s1/s2 stay 0 and h1..h4 stay at the standard WG message types so
            // the handshake is bit-for-bit plain; junk (jc + masquerade i1)
            // is what breaks the DPI signature.
            awgJc = state.junkCount
            awgJmin = state.junkMin
            awgJmax = state.junkMax
            awgH1 = "1"
            awgH2 = "2"
            awgH3 = "3"
            awgH4 = "4"
            awgId = state.masqueradeDomain.trim().ifBlank { "www.google.com" }
            awgIp = state.masqueradeProtocol.trim().ifBlank { "quic" }
            if (awgIp == "quic") {
                awgIb = state.masqueradeBrowser.trim().ifBlank { "chrome" }
            }
        }
    }

/**
 * Builds the MASQUE profile from the cached/registered account. The manual
 * endpoint override (§305) applies only to the node — the cached account keeps
 * the canonical registration server.
 */
internal fun MasqueAccount.toMasqueBean(state: WarpWizardUiState): MasqueBean =
    MasqueBean().applyDefaultValues().apply {
        name = "WARP (MASQUE)"
        privateKey = privKeyDer
        publicKey = serverPubDer
        localIp = clientV4.ensureCidr()
        localIpv6 = clientV6.ensureCidr()
        vhttp = state.masqueNetwork
        sni = state.masqueSni.trim()
        mtu = WarpEndpoints.WARP_MTU
        idleTimeout = state.masqueIdleMinutes.takeIf { it > 0 }?.let { "${it}m" }.orEmpty()
        keepAlive = state.masqueKeepAliveSeconds.takeIf { it > 0 }?.let { "${it}s" }.orEmpty()
        val ipOverride = state.masqueEndpointIp.trim()
        val portOverride = state.masquePort.trim().toIntOrNull() ?: 0
        serverAddress = ipOverride.ifBlank { server }
        serverPort = if (portOverride > 0) portOverride else port
    }

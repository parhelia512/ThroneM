package io.throneproj.thronem.ui.profile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.throneproj.thronem.fmt.wireguard.WireGuardBean
import io.throneproj.thronem.ktx.applyDefaultValues
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Immutable
internal data class WireGuardUiState(
    override val customConfig: String = "",
    override val customOutbound: String = "",
    val name: String = "",
    val address: String = "127.0.0.1",
    val port: Int = 51820,
    val localAddress: String = "",
    val privateKey: String = "",
    val publicKey: String = "",
    val preSharedKey: String = "",
    val mtu: Int = 1420,
    val reserved: String = "",
    val listenPort: Int = 0,
    val persistentKeepaliveInterval: Int = 0,
    // AmneziaWG obfuscation (empty/zero = unset)
    val awgJc: Int = 0,
    val awgJmin: Int = 0,
    val awgJmax: Int = 0,
    val awgS1: Int = 0,
    val awgS2: Int = 0,
    val awgH1: String = "",
    val awgH2: String = "",
    val awgH3: String = "",
    val awgH4: String = "",
    val awgId: String = "",
    val awgIp: String = "",
    val awgIb: String = "",
) : ProfileEditorUiState

@Stable
internal class WireGuardSettingsViewModel : ProfileEditorViewModel<WireGuardBean>() {
    override fun createBean() = WireGuardBean().applyDefaultValues()

    override val uiState: StateFlow<WireGuardUiState>
        field = MutableStateFlow(WireGuardUiState())

    override suspend fun WireGuardBean.writeToUiState() {
        uiState.update {
            it.copy(
                customConfig = customConfigJson,
                customOutbound = customOutboundJson,
                name = name,
                address = serverAddress,
                port = serverPort,
                localAddress = localAddress,
                privateKey = privateKey,
                publicKey = publicKey,
                preSharedKey = preSharedKey,
                mtu = mtu,
                reserved = reserved,
                listenPort = listenPort,
                persistentKeepaliveInterval = persistentKeepaliveInterval,
                awgJc = awgJc,
                awgJmin = awgJmin,
                awgJmax = awgJmax,
                awgS1 = awgS1,
                awgS2 = awgS2,
                awgH1 = awgH1,
                awgH2 = awgH2,
                awgH3 = awgH3,
                awgH4 = awgH4,
                awgId = awgId,
                awgIp = awgIp,
                awgIb = awgIb,
            )
        }
    }

    override fun WireGuardBean.loadFromUiState() {
        val state = uiState.value

        customConfigJson = state.customConfig
        customOutboundJson = state.customOutbound
        name = state.name
        serverAddress = state.address
        serverPort = state.port
        localAddress = state.localAddress
        privateKey = state.privateKey
        publicKey = state.publicKey
        preSharedKey = state.preSharedKey
        mtu = state.mtu
        reserved = state.reserved
        listenPort = state.listenPort
        persistentKeepaliveInterval = state.persistentKeepaliveInterval
        awgJc = state.awgJc
        awgJmin = state.awgJmin
        awgJmax = state.awgJmax
        awgS1 = state.awgS1
        awgS2 = state.awgS2
        awgH1 = state.awgH1
        awgH2 = state.awgH2
        awgH3 = state.awgH3
        awgH4 = state.awgH4
        awgId = state.awgId
        awgIp = state.awgIp
        awgIb = state.awgIb
    }

    override fun setCustomConfig(config: String) {
        uiState.update { it.copy(customConfig = config) }
    }

    override fun setCustomOutbound(outbound: String) {
        uiState.update { it.copy(customOutbound = outbound) }
    }

    fun setName(name: String) {
        uiState.update { it.copy(name = name) }
    }

    fun setAddress(address: String) {
        uiState.update { it.copy(address = address) }
    }

    fun setPort(port: Int) {
        uiState.update { it.copy(port = port) }
    }

    fun setLocalAddress(address: String) {
        uiState.update { it.copy(localAddress = address) }
    }

    fun setPrivateKey(key: String) {
        uiState.update { it.copy(privateKey = key) }
    }

    fun setPublicKey(key: String) {
        uiState.update { it.copy(publicKey = key) }
    }

    fun setPreSharedKey(key: String) {
        uiState.update { it.copy(preSharedKey = key) }
    }

    fun setMtu(mtu: Int) {
        uiState.update { it.copy(mtu = mtu) }
    }

    fun setReserved(reserved: String) {
        uiState.update { it.copy(reserved = reserved) }
    }

    fun setListenPort(port: Int) {
        uiState.update { it.copy(listenPort = port) }
    }

    fun setPersistentKeepaliveInterval(interval: Int) {
        uiState.update { it.copy(persistentKeepaliveInterval = interval) }
    }

    fun setAwgJc(value: Int) {
        uiState.update { it.copy(awgJc = value) }
    }

    fun setAwgJmin(value: Int) {
        uiState.update { it.copy(awgJmin = value) }
    }

    fun setAwgJmax(value: Int) {
        uiState.update { it.copy(awgJmax = value) }
    }

    fun setAwgS1(value: Int) {
        uiState.update { it.copy(awgS1 = value) }
    }

    fun setAwgS2(value: Int) {
        uiState.update { it.copy(awgS2 = value) }
    }

    fun setAwgH1(value: String) {
        uiState.update { it.copy(awgH1 = value) }
    }

    fun setAwgH2(value: String) {
        uiState.update { it.copy(awgH2 = value) }
    }

    fun setAwgH3(value: String) {
        uiState.update { it.copy(awgH3 = value) }
    }

    fun setAwgH4(value: String) {
        uiState.update { it.copy(awgH4 = value) }
    }

    fun setAwgId(value: String) {
        uiState.update { it.copy(awgId = value) }
    }

    fun setAwgIp(value: String) {
        uiState.update { it.copy(awgIp = value) }
    }

    fun setAwgIb(value: String) {
        uiState.update { it.copy(awgIb = value) }
    }
}

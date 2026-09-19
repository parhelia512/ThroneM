package io.throneproj.thronem.ui.profile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.throneproj.thronem.fmt.masque.MasqueBean
import io.throneproj.thronem.ktx.applyDefaultValues
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Immutable
internal data class MasqueUiState(
    override val customConfig: String = "",
    override val customOutbound: String = "",
    val name: String = "",
    val address: String = "162.159.198.1",
    val port: Int = 443,
    val privateKey: String = "",
    val publicKey: String = "",
    val localIp: String = "",
    val localIpv6: String = "",
    val vhttp: String = "auto",
    val sni: String = "",
    val mtu: Int = 1280,
    val idleTimeout: String = "",
    val keepAlive: String = "",
) : ProfileEditorUiState

@Stable
internal class MasqueSettingsViewModel : ProfileEditorViewModel<MasqueBean>() {
    override fun createBean() = MasqueBean().applyDefaultValues()

    override val uiState: StateFlow<MasqueUiState>
        field = MutableStateFlow(MasqueUiState())

    override suspend fun MasqueBean.writeToUiState() {
        uiState.update {
            it.copy(
                customConfig = customConfigJson,
                customOutbound = customOutboundJson,
                name = name,
                address = serverAddress,
                port = serverPort,
                privateKey = privateKey,
                publicKey = publicKey,
                localIp = localIp,
                localIpv6 = localIpv6,
                vhttp = vhttp,
                sni = sni,
                mtu = mtu,
                idleTimeout = idleTimeout,
                keepAlive = keepAlive,
            )
        }
    }

    override fun MasqueBean.loadFromUiState() {
        val state = uiState.value

        customConfigJson = state.customConfig
        customOutboundJson = state.customOutbound
        name = state.name
        serverAddress = state.address
        serverPort = state.port
        privateKey = state.privateKey
        publicKey = state.publicKey
        localIp = state.localIp
        localIpv6 = state.localIpv6
        vhttp = state.vhttp
        sni = state.sni
        mtu = state.mtu
        idleTimeout = state.idleTimeout
        keepAlive = state.keepAlive
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

    fun setPrivateKey(key: String) {
        uiState.update { it.copy(privateKey = key) }
    }

    fun setPublicKey(key: String) {
        uiState.update { it.copy(publicKey = key) }
    }

    fun setLocalIp(address: String) {
        uiState.update { it.copy(localIp = address) }
    }

    fun setLocalIpv6(address: String) {
        uiState.update { it.copy(localIpv6 = address) }
    }

    fun setVhttp(vhttp: String) {
        uiState.update { it.copy(vhttp = vhttp) }
    }

    fun setSni(sni: String) {
        uiState.update { it.copy(sni = sni) }
    }

    fun setMtu(mtu: Int) {
        uiState.update { it.copy(mtu = mtu) }
    }

    fun setIdleTimeout(timeout: String) {
        uiState.update { it.copy(idleTimeout = timeout) }
    }

    fun setKeepAlive(keepAlive: String) {
        uiState.update { it.copy(keepAlive = keepAlive) }
    }
}

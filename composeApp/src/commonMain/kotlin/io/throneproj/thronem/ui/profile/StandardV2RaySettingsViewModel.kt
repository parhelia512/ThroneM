package io.throneproj.thronem.ui.profile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.throneproj.thronem.fmt.v2ray.StandardV2RayBean

/**
 * XHTTP transport parameters (lx fork). Host/path/headers live on the shared
 * UI state; everything else mirrors `option.V2RayXHTTPOptions`. Empty string /
 * -1 / false mean "unset" and are omitted from the generated config.
 */
@Immutable
internal data class XhttpUiState(
    /** The core default is `auto`; there is no "unset" mode. */
    val mode: String = "auto",
    val paddingBytes: String = "",
    val noGrpcHeader: Boolean = false,
    val sessionPlacement: String = "",
    val sessionKey: String = "",
    val seqPlacement: String = "",
    val seqKey: String = "",
    val sessionTable: String = "",
    val sessionLength: String = "",
    val uplinkDataPlacement: String = "",
    val uplinkDataKey: String = "",
    val uplinkChunkSize: String = "",
    val uplinkHttpMethod: String = "",
    val paddingObfsMode: Boolean = false,
    val paddingKey: String = "",
    val paddingHeader: String = "",
    val paddingPlacement: String = "",
    val paddingMethod: String = "",
    val scMaxEachPostBytes: String = "",
    val scMinPostsIntervalMs: String = "",
    val scStreamUpServerSecs: String = "",
    val scMaxBufferedPosts: Int = -1,
    val noSseHeader: Boolean = false,
    val maxConcurrency: String = "",
    val maxConnections: String = "",
    val cMaxReuseTimes: String = "",
    val hMaxRequestTimes: String = "",
    val hMaxReusableSecs: String = "",
    val hKeepAlivePeriod: Int = -1,
)

internal fun StandardV2RayBean.readXhttp(): XhttpUiState = XhttpUiState(
    mode = xhttpMode,
    paddingBytes = xhttpPaddingBytes,
    noGrpcHeader = xhttpNoGrpcHeader,
    sessionPlacement = xhttpSessionPlacement,
    sessionKey = xhttpSessionKey,
    seqPlacement = xhttpSeqPlacement,
    seqKey = xhttpSeqKey,
    sessionTable = xhttpSessionTable,
    sessionLength = xhttpSessionLength,
    uplinkDataPlacement = xhttpUplinkDataPlacement,
    uplinkDataKey = xhttpUplinkDataKey,
    uplinkChunkSize = xhttpUplinkChunkSize,
    uplinkHttpMethod = xhttpUplinkHttpMethod,
    paddingObfsMode = xhttpPaddingObfsMode,
    paddingKey = xhttpPaddingKey,
    paddingHeader = xhttpPaddingHeader,
    paddingPlacement = xhttpPaddingPlacement,
    paddingMethod = xhttpPaddingMethod,
    scMaxEachPostBytes = xhttpScMaxEachPostBytes,
    scMinPostsIntervalMs = xhttpScMinPostsIntervalMs,
    scStreamUpServerSecs = xhttpScStreamUpServerSecs,
    scMaxBufferedPosts = xhttpScMaxBufferedPosts,
    noSseHeader = xhttpNoSseHeader,
    maxConcurrency = xhttpMaxConcurrency,
    maxConnections = xhttpMaxConnections,
    cMaxReuseTimes = xhttpCMaxReuseTimes,
    hMaxRequestTimes = xhttpHMaxRequestTimes,
    hMaxReusableSecs = xhttpHMaxReusableSecs,
    hKeepAlivePeriod = xhttpHKeepAlivePeriod,
)

internal fun StandardV2RayBean.applyXhttp(state: XhttpUiState) {
    xhttpMode = state.mode
    xhttpPaddingBytes = state.paddingBytes
    xhttpNoGrpcHeader = state.noGrpcHeader
    xhttpSessionPlacement = state.sessionPlacement
    xhttpSessionKey = state.sessionKey
    xhttpSeqPlacement = state.seqPlacement
    xhttpSeqKey = state.seqKey
    xhttpSessionTable = state.sessionTable
    xhttpSessionLength = state.sessionLength
    xhttpUplinkDataPlacement = state.uplinkDataPlacement
    xhttpUplinkDataKey = state.uplinkDataKey
    xhttpUplinkChunkSize = state.uplinkChunkSize
    xhttpUplinkHttpMethod = state.uplinkHttpMethod
    xhttpPaddingObfsMode = state.paddingObfsMode
    xhttpPaddingKey = state.paddingKey
    xhttpPaddingHeader = state.paddingHeader
    xhttpPaddingPlacement = state.paddingPlacement
    xhttpPaddingMethod = state.paddingMethod
    xhttpScMaxEachPostBytes = state.scMaxEachPostBytes
    xhttpScMinPostsIntervalMs = state.scMinPostsIntervalMs
    xhttpScStreamUpServerSecs = state.scStreamUpServerSecs
    xhttpScMaxBufferedPosts = state.scMaxBufferedPosts
    xhttpNoSseHeader = state.noSseHeader
    xhttpMaxConcurrency = state.maxConcurrency
    xhttpMaxConnections = state.maxConnections
    xhttpCMaxReuseTimes = state.cMaxReuseTimes
    xhttpHMaxRequestTimes = state.hMaxRequestTimes
    xhttpHMaxReusableSecs = state.hMaxReusableSecs
    xhttpHKeepAlivePeriod = state.hKeepAlivePeriod
}

@Immutable
internal sealed interface StandardV2RayUiState : ProfileEditorUiState {
    val name: String
    val address: String
    val port: Int

    val v2rayTransport: String
    val host: String
    val path: String
    val headers: String
    val wsMaxEarlyData: Int
    val wsEarlyDataHeaderName: String
    val xhttp: XhttpUiState

    val security: String
    val sni: String
    val alpn: String
    val certificate: String
    val certPublicKeySha256: String
    val allowInsecure: Boolean
    val disableSNI: Boolean
    val tlsFragment: Boolean
    val tlsFragmentFallbackDelay: String
    val tlsRecordFragment: Boolean
    val tlsSpoof: String
    val tlsSpoofMethod: String
    val utlsFingerprint: String
    val realityPublicKey: String
    val realityShortID: String
    val ech: Boolean
    val echConfig: String
    val echQueryServerName: String
    val clientCert: String
    val clientKey: String

    val enableMux: Boolean
    val brutal: Boolean
    val muxType: Int
    val muxStrategy: Int
    val muxNumber: Int
    val muxPadding: Boolean
}

@Stable
internal abstract class StandardV2RaySettingsViewModel<T : StandardV2RayBean> :
    ProfileEditorViewModel<T>() {

    abstract fun setName(name: String)
    abstract fun setAddress(address: String)
    abstract fun setPort(port: Int)
    abstract fun setTransport(transport: String)
    abstract fun setHost(host: String)
    abstract fun setPath(path: String)
    abstract fun setHeaders(headers: String)
    abstract fun setWsMaxEarlyData(maxEarlyData: Int)
    abstract fun setWsEarlyDataHeaderName(headerName: String)
    abstract fun setXhttp(state: XhttpUiState)
    abstract fun setSecurity(security: String)
    abstract fun setSni(sni: String)
    abstract fun setAlpn(alpn: String)
    abstract fun setCertificate(certificate: String)
    abstract fun setCertPublicKeySha256(sha256: String)
    abstract fun setAllowInsecure(allow: Boolean)
    abstract fun setDisableSNI(disable: Boolean)
    abstract fun setTlsFragment(enable: Boolean)
    abstract fun setTlsFragmentFallbackDelay(delay: String)
    abstract fun setTlsRecordFragment(enable: Boolean)
    abstract fun setTlsSpoof(value: String)
    abstract fun setTlsSpoofMethod(value: String)
    abstract fun setUtlsFingerprint(fingerprint: String)
    abstract fun setRealityPublicKey(publicKey: String)
    abstract fun setRealityShortID(shortID: String)
    abstract fun setEch(enable: Boolean)
    abstract fun setEchConfig(config: String)
    abstract fun setEchQueryServerName(queryServerName: String)
    abstract fun setClientCert(cert: String)
    abstract fun setClientKey(key: String)
    abstract fun setEnableMux(enable: Boolean)
    abstract fun setBrutal(enable: Boolean)
    abstract fun setMuxType(type: Int)
    abstract fun setMuxStrategy(strategy: Int)
    abstract fun setMuxNumber(number: Int)
    abstract fun setMuxPadding(enable: Boolean)
}
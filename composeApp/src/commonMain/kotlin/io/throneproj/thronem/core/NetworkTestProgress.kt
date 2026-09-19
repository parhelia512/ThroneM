package io.throneproj.thronem.core

import io.nekohasekai.libbox.Libbox

enum class StunPhase {
    Binding,
    NatMapping,
    NatFiltering,
    Done,
    ;

    companion object {
        fun ofLibbox(value: Int): StunPhase = when (value) {
            Libbox.STUNPhaseNATMapping -> NatMapping
            Libbox.STUNPhaseNATFiltering -> NatFiltering
            Libbox.STUNPhaseDone -> Done
            else -> Binding
        }
    }
}

enum class NatBehaviour {
    Unknown,
    EndpointIndependent,
    AddressDependent,
    AddressAndPortDependent,
    ;

    companion object {
        fun ofMapping(value: Int): NatBehaviour = when (value) {
            Libbox.NATMappingEndpointIndependent -> EndpointIndependent
            Libbox.NATMappingAddressDependent -> AddressDependent
            Libbox.NATMappingAddressAndPortDependent -> AddressAndPortDependent
            else -> Unknown
        }

        fun ofFiltering(value: Int): NatBehaviour = when (value) {
            Libbox.NATFilteringEndpointIndependent -> EndpointIndependent
            Libbox.NATFilteringAddressDependent -> AddressDependent
            Libbox.NATFilteringAddressAndPortDependent -> AddressAndPortDependent
            else -> Unknown
        }
    }
}

enum class NetworkQualityPhase {
    Idle,
    Download,
    Upload,
    Done,
    ;

    companion object {
        fun ofLibbox(value: Int): NetworkQualityPhase = when (value) {
            Libbox.NetworkQualityPhaseDownload -> Download
            Libbox.NetworkQualityPhaseUpload -> Upload
            Libbox.NetworkQualityPhaseDone -> Done
            else -> Idle
        }
    }
}

/**
 * App-native STUN test progress. libbox carries errors through the handler's
 * onError callback rather than the progress message, so both paths land here.
 */
data class STUNTestProgress(
    val phase: StunPhase = StunPhase.Binding,
    val externalAddr: String = "",
    val latencyMs: Int = 0,
    val natMapping: NatBehaviour = NatBehaviour.Unknown,
    val natFiltering: NatBehaviour = NatBehaviour.Unknown,
    val natTypeSupported: Boolean = false,
    val error: String? = null,
    val isFinal: Boolean = false,
)

data class NetworkQualityTestProgress(
    val phase: NetworkQualityPhase = NetworkQualityPhase.Idle,
    val downloadCapacity: Long = 0L,
    val uploadCapacity: Long = 0L,
    val downloadRPM: Int = 0,
    val uploadRPM: Int = 0,
    val idleLatencyMs: Int = 0,
    val elapsedMs: Long = 0L,
    val error: String? = null,
    val isFinal: Boolean = false,
)

val STUNTestProgress.failure: String?
    get() = error.takeIf { isFinal && !it.isNullOrEmpty() }

val NetworkQualityTestProgress.failure: String?
    get() = error.takeIf { isFinal && !it.isNullOrEmpty() }

internal fun STUNTestProgress(
    progress: io.nekohasekai.libbox.STUNTestProgress,
): STUNTestProgress = STUNTestProgress(
    phase = StunPhase.ofLibbox(progress.phase),
    externalAddr = progress.externalAddr,
    latencyMs = progress.latencyMs,
    natMapping = NatBehaviour.ofMapping(progress.natMapping),
    natFiltering = NatBehaviour.ofFiltering(progress.natFiltering),
)

internal fun STUNTestProgress(
    result: io.nekohasekai.libbox.STUNTestResult,
): STUNTestProgress = STUNTestProgress(
    phase = StunPhase.Done,
    externalAddr = result.externalAddr,
    latencyMs = result.latencyMs,
    natMapping = NatBehaviour.ofMapping(result.natMapping),
    natFiltering = NatBehaviour.ofFiltering(result.natFiltering),
    natTypeSupported = result.natTypeSupported,
    isFinal = true,
)

internal fun NetworkQualityTestProgress(
    progress: io.nekohasekai.libbox.NetworkQualityProgress,
): NetworkQualityTestProgress = NetworkQualityTestProgress(
    phase = NetworkQualityPhase.ofLibbox(progress.phase),
    downloadCapacity = progress.downloadCapacity,
    uploadCapacity = progress.uploadCapacity,
    downloadRPM = progress.downloadRPM,
    uploadRPM = progress.uploadRPM,
    idleLatencyMs = progress.idleLatencyMs,
    elapsedMs = progress.elapsedMs,
)

internal fun NetworkQualityTestProgress(
    result: io.nekohasekai.libbox.NetworkQualityResult,
): NetworkQualityTestProgress = NetworkQualityTestProgress(
    phase = NetworkQualityPhase.Done,
    downloadCapacity = result.downloadCapacity,
    uploadCapacity = result.uploadCapacity,
    downloadRPM = result.downloadRPM,
    uploadRPM = result.uploadRPM,
    idleLatencyMs = result.idleLatencyMs,
    isFinal = true,
)

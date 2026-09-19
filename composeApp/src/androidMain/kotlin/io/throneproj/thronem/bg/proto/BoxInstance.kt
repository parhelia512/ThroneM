package io.throneproj.thronem.bg.proto

import io.throneproj.thronem.bg.AbstractInstance
import io.throneproj.thronem.bg.CoreBox
import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.fmt.ConfigBuildResult
import io.throneproj.thronem.fmt.ConfigMetadata
import io.throneproj.thronem.fmt.buildConfig
import io.throneproj.thronem.ktx.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BoxInstance(
    val profile: ProxyEntity,
) : AbstractInstance {

    lateinit var metadata: ConfigMetadata

    private var pendingConfigJson: String? = null

    /**
     * Config has been built. The box instance itself is only started at
     * [launch] via [CoreBox.startService], so call sites that only need the
     * config must not wait for [isInitialized].
     */
    fun isInitialized(): Boolean {
        return ::metadata.isInitialized
    }

    protected open suspend fun buildConfig(): ConfigBuildResult {
        return buildConfig(profile)
    }

    open suspend fun init(isVPN: Boolean) {
        val result = buildConfig()
        metadata = result.metadata
        pendingConfigJson = result.configJson
    }

    override fun launch() {
        val configJson = checkNotNull(pendingConfigJson) { "instance already launched" }
        pendingConfigJson = null
        // startOrReloadService blocks until the core is up.
        kotlinx.coroutines.runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    CoreBox.startService(configJson)
                } catch (e: Exception) {
                    Logs.w(e)
                    throw e
                }
            }
        }
    }

    override fun close() {
        runCatching {
            CoreBox.stopService()
        }.onFailure {
            Logs.w(it)
        }
    }

}

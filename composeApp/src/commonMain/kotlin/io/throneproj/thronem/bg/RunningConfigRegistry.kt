package io.throneproj.thronem.bg

import io.throneproj.thronem.fmt.ConfigMetadata

/**
 * Metadata of the configuration most recently loaded into the core. Set when
 * a proxy instance starts, cleared when it closes. Lets UI code map profile
 * ids to the outbound tags of the running config (delay tests, traffic
 * attribution fallbacks).
 */
object RunningConfigRegistry {

    @Volatile
    var metadata: ConfigMetadata? = null
}

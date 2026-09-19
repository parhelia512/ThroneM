package io.throneproj.thronem.bg

import java.net.InetAddress

actual suspend fun resolveByDefaultNetwork(host: String): List<InetAddress> =
    DefaultNetworkMonitor.withDefaultNetwork { network ->
        network.getAllByName(host).filterNotNull()
    }

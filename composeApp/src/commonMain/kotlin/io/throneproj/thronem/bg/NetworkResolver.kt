package io.throneproj.thronem.bg

import java.net.InetAddress

expect suspend fun resolveByDefaultNetwork(host: String): List<InetAddress>

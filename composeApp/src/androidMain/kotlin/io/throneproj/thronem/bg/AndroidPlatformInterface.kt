package io.throneproj.thronem.bg

import android.content.pm.PackageManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.system.OsConstants
import androidx.annotation.RequiresApi
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.toPrefix
import io.throneproj.thronem.ktx.toStringIterator
import io.throneproj.thronem.repository.resolveAndroidRepository
import io.throneproj.thronem.utils.PackageCache
import java.net.InetSocketAddress
import java.net.NetworkInterface

class AndroidPlatformInterface : PlatformInterface {

    override fun autoDetectInterfaceControl(fd: Int) {
        if (ServiceRegistry.vpnService?.protect(fd) != true) {
            throw NullPointerException("protect() failed")
        }
    }

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        DefaultNetworkMonitor.setListener(listener)
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        DefaultNetworkMonitor.setListener(null)
    }

    override fun openTun(tunOptions: TunOptions?): Int {
        if (ServiceRegistry.vpnService == null) throw NullPointerException("no vpnService")
        return ServiceRegistry.vpnService!!.startVpn()
    }

    override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int,
    ): ConnectionOwner {
        try {
            val uid = resolveAndroidRepository().connectivity.getConnectionOwnerUid(
                ipProtocol,
                InetSocketAddress(sourceAddress, sourcePort),
                InetSocketAddress(destinationAddress, destinationPort),
            )
            if (uid == Process.INVALID_UID) error("android: connection owner not found")
            PackageCache.awaitLoadSync()
            val packages = PackageCache.uidMap[uid]
            val owner = ConnectionOwner()
            owner.setUserId(uid)
            if (packages != null) {
                owner.setAndroidPackageNames(packages.toStringIterator(packages.size))
            }
            return owner
        } catch (e: Exception) {
            Logs.e(e)
            throw e
        }
    }

    override fun lookupUser(username: String?): PlatformUser? = null

    override fun readWIFIState(): WIFIState? {
        if (!resolveAndroidRepository().packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) return null
        // TODO API 34
        @Suppress("DEPRECATION") val wifiInfo = resolveAndroidRepository().wifi.connectionInfo ?: return null
        var ssid = wifiInfo.ssid
        if (ssid == "<unknown ssid>") return Libbox.newWIFIState("", "")
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length - 1)
        }
        return Libbox.newWIFIState(ssid, wifiInfo.bssid)
    }

    override fun getInterfaces(): NetworkInterfaceIterator {
        @Suppress("DEPRECATION") val networks = resolveAndroidRepository().connectivity.allNetworks
        val networkInterfaces = NetworkInterface.getNetworkInterfaces().toList()
        val interfaces = mutableListOf<io.nekohasekai.libbox.NetworkInterface>()
        for (network in networks) {
            val boxInterface = io.nekohasekai.libbox.NetworkInterface()
            val linkProperties = resolveAndroidRepository().connectivity.getLinkProperties(network) ?: continue
            val networkCapabilities = resolveAndroidRepository().connectivity.getNetworkCapabilities(network) ?: continue
            boxInterface.name = linkProperties.interfaceName
            val networkInterface =
                networkInterfaces.find { it.name == boxInterface.name } ?: continue
            boxInterface.dnsServer = linkProperties.dnsServers.mapNotNull { it.hostAddress }
                .toStringIterator()
            boxInterface.type = when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                else -> Libbox.InterfaceTypeOther
            }
            boxInterface.index = networkInterface.index
            runCatching {
                boxInterface.mtu = networkInterface.mtu
            }.onFailure { e ->
                Logs.e("failed to get mtu for interface ${boxInterface.name}", e)
            }
            boxInterface.addresses = networkInterface.interfaceAddresses.map {
                it.toPrefix()
            }.toStringIterator()
            var dumpFlags = 0
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                dumpFlags = OsConstants.IFF_UP or OsConstants.IFF_RUNNING
            }
            if (networkInterface.isLoopback) {
                dumpFlags = dumpFlags or OsConstants.IFF_LOOPBACK
            }
            if (networkInterface.isPointToPoint) {
                dumpFlags = dumpFlags or OsConstants.IFF_POINTOPOINT
            }
            if (networkInterface.supportsMulticast()) {
                dumpFlags = dumpFlags or OsConstants.IFF_MULTICAST
            }
            boxInterface.flags = dumpFlags
            boxInterface.metered =
                !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            interfaces.add(boxInterface)
        }
        return InterfaceArray(interfaces.iterator())
    }

    override fun localDNSTransport(): LocalDNSTransport = LocalResolver

    override fun includeAllNetworks(): Boolean = false

    override fun underNetworkExtension(): Boolean = false

    override fun clearDNSCache() {
        // The app has no DNS cache of its own to clear.
    }

    override fun registerMyInterface(name: String?) {
        // Nothing to reserve; the TUN interface is owned by this process.
    }

    override fun sendNotification(notification: io.nekohasekai.libbox.Notification?) {
        // App notifications are managed by ServiceNotification.
    }

    override fun cancelNotification(identifier: String?, typeID: Int) {
    }

    override fun checkPlatformShell() {
        throw UnsupportedOperationException("platform shell is not supported on Android")
    }

    override fun usePlatformShell(): Boolean = false

    override fun openShellSession(
        user: io.nekohasekai.libbox.PlatformUser?,
        shell: String?,
        env: io.nekohasekai.libbox.StringIterator?,
        workingDir: String?,
        columns: Int,
        rows: Int,
    ): io.nekohasekai.libbox.ShellSession {
        throw UnsupportedOperationException("platform shell is not supported on Android")
    }

    override fun lookupSFTPServer(): String {
        throw UnsupportedOperationException("SFTP is not supported on Android")
    }

    override fun readSystemSSHHostKey(): String {
        throw UnsupportedOperationException("SSH host keys are not supported on Android")
    }

    override fun tailscaleHostname(): String = ""

    override fun startNeighborMonitor(listener: io.nekohasekai.libbox.NeighborUpdateListener?) {
        // Neighbor table monitoring is not supported on Android.
    }

    override fun closeNeighborMonitor(listener: io.nekohasekai.libbox.NeighborUpdateListener?) {
    }

    override fun usePlatformBridge(): Boolean = false

    override fun createBridge(options: io.nekohasekai.libbox.BridgeOptions?): io.nekohasekai.libbox.BridgeSession {
        throw UnsupportedOperationException("bridge is not supported on Android")
    }

    private class InterfaceArray(
        private val iterator: Iterator<io.nekohasekai.libbox.NetworkInterface>,
    ) : NetworkInterfaceIterator {

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }

        override fun next(): io.nekohasekai.libbox.NetworkInterface {
            return iterator.next()
        }
    }
}

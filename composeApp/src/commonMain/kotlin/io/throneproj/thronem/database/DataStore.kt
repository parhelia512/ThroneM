package io.throneproj.thronem.database

import io.throneproj.thronem.CONNECTION_TEST_URL
import io.throneproj.thronem.CertProvider
import io.throneproj.thronem.DEFAULT_HTTP_BYPASS
import io.throneproj.thronem.DOMAIN_STRATEGY_AUTO
import io.throneproj.thronem.GroupType
import io.throneproj.thronem.Key
import io.throneproj.thronem.NETWORK_QUALITY_CONFIG_URL
import io.throneproj.thronem.NetworkInterfaceStrategy
import io.throneproj.thronem.TrafficSortMode
import io.throneproj.thronem.TunIpStack
import io.throneproj.thronem.bg.ServiceState
import io.throneproj.thronem.compose.theme.DEFAULT
import io.throneproj.thronem.database.preference.DataStorePreferenceDataStore
import io.throneproj.thronem.database.preference.boolean
import io.throneproj.thronem.database.preference.int
import io.throneproj.thronem.database.preference.long
import io.throneproj.thronem.database.preference.port
import io.throneproj.thronem.database.preference.preferenceStoreScope
import io.throneproj.thronem.database.preference.string
import io.throneproj.thronem.database.preference.stringSet
import io.throneproj.thronem.platform.PlatformInfo
import io.throneproj.thronem.repository.resolveRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

object DataStore {

    // In-memory copy of the current service state, fed by BaseService and
    // mirrored to observers through BackendState / CoreEventBus.
    @Volatile
    var serviceState = ServiceState.Idle

    val configurationStore = DataStorePreferenceDataStore.create(
        resolveRepository().createConfigurationDataStore(preferenceStoreScope()),
    )

    init {
        // Migration
        val keyIndividual = "individual"
        val oldPackages = configurationStore.getString(keyIndividual)?.split("\n")
        if (oldPackages?.isNotEmpty() == true && configurationStore.getStringSet(Key.PACKAGES) == null) {
            configurationStore.putStringSet(Key.PACKAGES, oldPackages.toMutableSet())
            // remove old key
            configurationStore.remove(keyIndividual)
        }

        val keyTCPKeepAliveInterval = "tcpKeepAliveInterval"
        configurationStore.getString(keyTCPKeepAliveInterval)?.let { oldTCPKeepAlive ->
            if (oldTCPKeepAlive.lastOrNull()?.isLetter() == true) {
                configurationStore.putString(Key.TCP_KEEP_ALIVE_INTERVAL_0, oldTCPKeepAlive)
            } else {
                val seconds = oldTCPKeepAlive.toIntOrNull() ?: 75
                configurationStore.putString(Key.TCP_KEEP_ALIVE_INTERVAL_0, "${seconds}s")
            }
            configurationStore.remove(keyTCPKeepAliveInterval)
        }
    }

    // last used, but may not be running
    val currentProfile = configurationStore.long(Key.PROFILE_CURRENT)

    /** The proxy set picked on the proxy sets page; the service runs with it. */
    val selectedProxySet = configurationStore.long(Key.PROXY_SET_ID)

    /** No group use this ID */
    const val GROUP_NOPE = -1L

    /**
     * The stored value is [GROUP_NOPE] until a group is picked. Resolving that to a
     * real group can create the default group and is a database round trip, so it
     * lives in [currentGroupId] / [currentGroup].
     */
    val selectedGroup = configurationStore.long(Key.PROFILE_GROUP)

    suspend fun currentGroupId(): Long {
        val currentSelected = selectedGroup.getOrNull()
        if (currentSelected != null && currentSelected > GROUP_NOPE) return currentSelected
        val groupId = ProfileManager.ensureDefaultGroupId()
        selectedGroup.set(groupId)
        return groupId
    }

    suspend fun currentGroup(): ProxyGroup {
        val currentSelected = selectedGroup.getOrNull()
        if (currentSelected != null && currentSelected > GROUP_NOPE) {
            val group = ThroneDatabase.groupDao.getById(currentSelected).firstOrNull()
            if (group != null) return group
        }
        val groupId = ProfileManager.ensureDefaultGroupId()
        val group = ThroneDatabase.groupDao.getById(groupId).firstOrNull()
            ?: ThroneDatabase.groupDao.allGroups().first().first()
        selectedGroup.set(group.id)
        return group
    }

    suspend fun selectedGroupForImport(): Long {
        val current = currentGroup()
        if (current.type == GroupType.BASIC) return current.id
        val groups = ThroneDatabase.groupDao.allGroups().first()
        return groups.find { it.type == GroupType.BASIC }!!.id
    }

    val isExpert = configurationStore.boolean(Key.APP_EXPERT)

    /** Cached Cloudflare WARP account (JSON [io.throneproj.thronem.warp.WarpAccount]). */
    val warpAccount = configurationStore.string(Key.WARP_ACCOUNT)

    /** Cached MASQUE-WARP account (JSON [io.throneproj.thronem.warp.MasqueAccount]). */
    val masqueAccount = configurationStore.string(Key.MASQUE_ACCOUNT)
    val appTheme = configurationStore.int(Key.APP_THEME) { DEFAULT }
    val nightTheme = configurationStore.int(Key.NIGHT_THEME)
    val appLanguage = configurationStore.string(Key.APP_LANGUAGE)
    val serviceMode = configurationStore.string(Key.SERVICE_MODE) { Key.MODE_VPN }
    val debugListen = configurationStore.string(Key.DEBUG_LISTEN)
    val networkStrategy = configurationStore.string(Key.NETWORK_STRATEGY)

    val networkInterfaceType = configurationStore.int(Key.NETWORK_INTERFACE_STRATEGY) {
        NetworkInterfaceStrategy.DEFAULT
    }
    val networkPreferredInterfaces = configurationStore.stringSet(Key.NETWORK_PREFERRED_INTERFACES)
    val forcedSearchProcess = configurationStore.boolean(Key.FORCED_SEARCH_PROCESS) { false }

    val disableTcpKeepAlive = configurationStore.boolean(Key.DISABLE_TCP_KEEP_ALIVE) { PlatformInfo.isAndroid }
    val tcpKeepAliveIdle = configurationStore.string(Key.TCP_KEEP_ALIVE_IDLE) { "5m" }
    val tcpKeepAliveInterval = configurationStore.string(Key.TCP_KEEP_ALIVE_INTERVAL_0) { "75s" }
    val mtu = configurationStore.int(Key.MTU) { 9000 }
    val vpnSessionName = configurationStore.string(Key.VPN_SESSION_NAME) { "" }
    val tunInterfaceName = configurationStore.string(Key.TUN_INTERFACE_NAME) { "" }
    val tunStrictRoute = configurationStore.boolean(Key.TUN_STRICT_ROUTE) { true }
    val tunAutoRedirect = configurationStore.boolean(Key.TUN_AUTO_REDIRECT) { true }
    val allowAppsBypassVpn = configurationStore.boolean(Key.ALLOW_APPS_BYPASS_VPN) { false }

    val bypassLan = configurationStore.boolean(Key.BYPASS_LAN) { true }
    val inboundUsername = configurationStore.string(Key.INBOUND_USERNAME) { "" }
    val inboundPassword = configurationStore.string(Key.INBOUND_PASSWORD) { "" }

    val allowAccess = configurationStore.boolean(Key.ALLOW_ACCESS)
    val speedInterval = configurationStore.int(Key.SPEED_INTERVAL) { 1000 }
    val showGroupInNotification = configurationStore.boolean(Key.SHOW_GROUP_IN_NOTIFICATION)

    val remoteDns = configurationStore.string(Key.REMOTE_DNS) { "tcp://dns.google" }
    val directDns = configurationStore.string(Key.DIRECT_DNS) { "local" }
    val mDNS = configurationStore.string(Key.MDNS) { "" }

    // Consumers strip "auto" back to an empty strategy, so it is the neutral default.
    val domainStrategyForDirect = configurationStore.string(Key.DOMAIN_STRATEGY_FOR_DIRECT) {
        DOMAIN_STRATEGY_AUTO
    }
    val domainStrategyForServer = configurationStore.string(Key.DOMAIN_STRATEGY_FOR_SERVER) {
        DOMAIN_STRATEGY_AUTO
    }

    // Using different outbound is a normal situation for proxy set.
    // And the application may not refresh their DNS cache in time.
    // So fake DNS is the best resolution. (With long-term practice by Clash and Surge.)
    val enableFakeDns = configurationStore.boolean(Key.ENABLE_FAKE_DNS) { true }
    val fakeDNSForAll = configurationStore.boolean(Key.FAKE_DNS_FOR_ALL) { false }

    // https://developer.chrome.com/blog/local-network-access
    // Use the address belongs to these "local" networks
    // (https://wicg.github.io/local-network-access/#non-public-ip-address-blocks)
    // will make permission warning in Chrome.
    // To avoid user agreeing plenty of permissions, we decide to use these new address.
    // The pre-defined IPv4 range is limited, change to whatever user like.
    val fakeDNSRange4 = configurationStore.string(Key.FAKE_DNS_RANGE_4) { "198.51.100.0/24" }
    val fakeDNSRange6 = configurationStore.string(Key.FAKE_DNS_RANGE_6) { "2001:2::/48" }
    val dnsHosts = configurationStore.string(Key.DNS_HOSTS)
    val dnsOptimisticCache = configurationStore.string(Key.DNS_OPTIMISTIC_CACHE) { "" }

    val securityAdvisory = configurationStore.boolean(Key.SECURITY_ADVISORY) { true }
    val rulesProvider = configurationStore.int(Key.RULES_PROVIDER)
    val customRuleProvider = configurationStore.string(Key.CUSTOM_RULE_PROVIDER)
    val routeAssetsAutoUpdateDelay = configurationStore.int(Key.ROUTE_ASSETS_AUTO_UPDATE_DELAY) { 0 }
    val routeAssetsLastUpdated = configurationStore.long(Key.ROUTE_ASSETS_LAST_UPDATED) { 0L }
    val logLevel = configurationStore.int(Key.LOG_LEVEL) { 3 /* WARN */ }
    val logMaxLine = configurationStore.int(Key.LOG_MAX_LINE) { 1024 }
    val acquireWakeLock = configurationStore.boolean(Key.ACQUIRE_WAKE_LOCK)

    val mixedPort = configurationStore.port(Key.MIXED_PORT, 2080)
    val localDNSPort = configurationStore.port(Key.LOCAL_DNS_PORT, 0)

    suspend fun initGlobal() {
        if (mixedPort.getOrNull() == null) {
            mixedPort.set(mixedPort.get())
        }
        if (localDNSPort.getOrNull() == null) {
            localDNSPort.set(localDNSPort.get())
        }
    }

    val meteredNetwork = configurationStore.boolean(Key.METERED_NETWORK)
    val proxyApps = configurationStore.boolean(Key.PROXY_APPS)
    val updateProxyAppsWhenInstall = configurationStore.boolean(Key.UPDATE_PROXY_APPS_WHEN_INSTALL)
    val bypassMode = configurationStore.boolean(Key.BYPASS_MODE) { true } // VPN bypass mode

    val packages = configurationStore.stringSet(Key.PACKAGES)
    val showDirectSpeed = configurationStore.boolean(Key.SHOW_DIRECT_SPEED) { true }

    val persistAcrossReboot = configurationStore.boolean(Key.PERSIST_ACROSS_REBOOT) { false }

    val appendHttpProxy = configurationStore.boolean(Key.APPEND_HTTP_PROXY)
    val httpProxyBypass = configurationStore.string(Key.HTTP_PROXY_BYPASS) { DEFAULT_HTTP_BYPASS }

    val connectionTestURL = configurationStore.string(Key.CONNECTION_TEST_URL) { CONNECTION_TEST_URL }
    val connectionTestConcurrent = configurationStore.int(Key.CONNECTION_TEST_CONCURRENT) { 5 }
    val connectionTestTimeout = configurationStore.int(Key.CONNECTION_TEST_TIMEOUT) { 3000 }
    val connectionTestUnifiedDelay = configurationStore.boolean(Key.CONNECTION_TEST_UNIFIED_DELAY) { false }
    val connectionTestIgnoreHandshakeTime =
        configurationStore.boolean(Key.CONNECTION_TEST_IGNORE_HANDSHAKE_TIME) { false }

    val alwaysShowAddress = configurationStore.boolean(Key.ALWAYS_SHOW_ADDRESS)
    val blurredAddress = configurationStore.boolean(Key.BLURRED_ADDRESS)
    val privacyMode = configurationStore.boolean(Key.PRIVACY_MODE) { false }

    val tunIpStack = configurationStore.int(Key.TUN_IP_STACK) { TunIpStack.MIXED }
    val profileTrafficStatistics = configurationStore.boolean(Key.PROFILE_TRAFFIC_STATISTICS) { true }
    val certProvider = configurationStore.int(Key.CERT_PROVIDER) { CertProvider.MOZILLA }
    val disableProcessText = configurationStore.boolean(Key.DISABLE_PROCESS_TEXT)
    val hideLauncherIcon = configurationStore.boolean(Key.HIDE_LAUNCHER_ICON)
    val enableTasker = configurationStore.boolean(Key.ENABLE_TASKER) { false }

    val trafficDescending = configurationStore.boolean(Key.TRAFFIC_DESCENDING) { false }
    val trafficSortMode = configurationStore.int(Key.TRAFFIC_SORT_MODE) { TrafficSortMode.START }
    val trafficConnectionQuery = configurationStore.int(Key.TRAFFIC_CONNECTION_QUERY) { 1 shl 0 }
    val proxySetOrder = configurationStore.int(Key.PROXY_SET_ORDER)
    val dashboardWidgets = configurationStore.string(Key.DASHBOARD_WIDGETS)

    val networkQualityConfigUrl =
        configurationStore.string(Key.NETWORK_QUALITY_CONFIG_URL) { NETWORK_QUALITY_CONFIG_URL }
    val networkQualitySerial = configurationStore.boolean(Key.NETWORK_QUALITY_SERIAL) { false }
    val networkQualityMaxRuntime = configurationStore.int(Key.NETWORK_QUALITY_MAX_RUNTIME) { 30 }
    val networkQualityHttp3 = configurationStore.boolean(Key.NETWORK_QUALITY_HTTP3) { false }

    // ntp
    val ntpEnable = configurationStore.boolean(Key.ENABLE_NTP) { false }
    val ntpAddress = configurationStore.string(Key.NTP_SERVER) { "time.apple.com" }
    val ntpPort = configurationStore.int(Key.NTP_PORT) { 123 }
    val ntpInterval = configurationStore.string(Key.NTP_INTERVAL) { "30m" }

    // protocol

    val uploadSpeed = configurationStore.int(Key.UPLOAD_SPEED) { 0 }
    val downloadSpeed = configurationStore.int(Key.DOWNLOAD_SPEED) { 0 }

    val rulesFirstCreate = configurationStore.boolean(Key.RULES_FIRST_CREATE)

    val desktopNavRailWidth = configurationStore.int(Key.DESKTOP_NAV_RAIL_WIDTH) { 220 }

}

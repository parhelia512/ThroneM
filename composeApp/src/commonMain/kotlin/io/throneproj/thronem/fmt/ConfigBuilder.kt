package io.throneproj.thronem.fmt

import io.throneproj.thronem.DOMAIN_STRATEGY_AUTO
import io.throneproj.thronem.Key
import io.throneproj.thronem.NetworkInterfaceStrategy
import io.throneproj.thronem.RuleProvider
import io.throneproj.thronem.TunIpStack
import io.throneproj.thronem.bg.VpnConstants
import io.throneproj.thronem.bg.routeGeoDir
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ProfileManager
import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_CONFIG
import io.throneproj.thronem.database.RuleEntity
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.fmt.SingBoxOptions.CacheFileOptions
import io.throneproj.thronem.fmt.SingBoxOptions.DNSRule_Default
import io.throneproj.thronem.fmt.SingBoxOptions.DomainResolveOptions
import io.throneproj.thronem.fmt.SingBoxOptions.ExperimentalOptions
import io.throneproj.thronem.fmt.SingBoxOptions.Inbound_DirectOptions
import io.throneproj.thronem.fmt.SingBoxOptions.Inbound_HTTPMixedOptions
import io.throneproj.thronem.fmt.SingBoxOptions.Inbound_TunOptions
import io.throneproj.thronem.fmt.SingBoxOptions.LogOptions
import io.throneproj.thronem.fmt.SingBoxOptions.MyDNSOptions
import io.throneproj.thronem.fmt.SingBoxOptions.MyOptions
import io.throneproj.thronem.fmt.SingBoxOptions.MyRouteOptions
import io.throneproj.thronem.fmt.SingBoxOptions.NTPOptions
import io.throneproj.thronem.fmt.SingBoxOptions.NewDNSServerOptions_FakeIPDNSServerOptions
import io.throneproj.thronem.fmt.SingBoxOptions.NewDNSServerOptions_HostsDNSServerOptions
import io.throneproj.thronem.fmt.SingBoxOptions.NewDNSServerOptions_LocalDNSServerOptions
import io.throneproj.thronem.fmt.SingBoxOptions.NewDNSServerOptions_MDNSDNSServerOptions
import io.throneproj.thronem.fmt.SingBoxOptions.OptimisticDNSOptions
import io.throneproj.thronem.fmt.SingBoxOptions.Outbound
import io.throneproj.thronem.fmt.SingBoxOptions.Outbound_DirectOptions
import io.throneproj.thronem.fmt.SingBoxOptions.Rule_Default
import io.throneproj.thronem.fmt.SingBoxOptions.User
import io.throneproj.thronem.fmt.anytls.AnyTLSBean
import io.throneproj.thronem.fmt.anytls.buildSingBoxOutboundAnyTLSBean
import io.throneproj.thronem.fmt.config.ConfigBean
import io.throneproj.thronem.fmt.direct.DirectBean
import io.throneproj.thronem.fmt.direct.buildSingBoxOutboundDirectBean
import io.throneproj.thronem.fmt.hysteria.HysteriaBean
import io.throneproj.thronem.fmt.hysteria.buildSingBoxOutboundHysteriaBean
import io.throneproj.thronem.fmt.internal.ChainBean
import io.throneproj.thronem.fmt.internal.ProxySetBean
import io.throneproj.thronem.fmt.internal.buildSingBoxOutboundProxySetBean
import io.throneproj.thronem.fmt.internal.resolveMembers
import io.throneproj.thronem.fmt.juicity.JuicityBean
import io.throneproj.thronem.fmt.juicity.buildSingBoxOutboundJuicityBean
import io.throneproj.thronem.fmt.naive.NaiveBean
import io.throneproj.thronem.fmt.naive.buildSingBoxOutboundNaiveBean
import io.throneproj.thronem.fmt.openconnect.OpenConnectBean
import io.throneproj.thronem.fmt.openconnect.buildSingBoxEndpointOpenConnectBean
import io.throneproj.thronem.fmt.openvpn.OpenVPNBean
import io.throneproj.thronem.fmt.openvpn.buildSingBoxEndpointOpenVPNBean
import io.throneproj.thronem.fmt.masque.MasqueBean
import io.throneproj.thronem.fmt.masque.buildSingBoxOutboundMasqueBean
import io.throneproj.thronem.fmt.shadowsocks.ShadowsocksBean
import io.throneproj.thronem.fmt.shadowsocks.buildSingBoxOutboundShadowsocksBean
import io.throneproj.thronem.fmt.shadowtls.ShadowTLSBean
import io.throneproj.thronem.fmt.shadowtls.buildSingBoxOutboundShadowTLSBean
import io.throneproj.thronem.fmt.snell.SnellBean
import io.throneproj.thronem.fmt.snell.buildSingBoxOutboundSnellBean
import io.throneproj.thronem.fmt.socks.SOCKSBean
import io.throneproj.thronem.fmt.socks.buildSingBoxOutboundSocksBean
import io.throneproj.thronem.fmt.ssh.SSHBean
import io.throneproj.thronem.fmt.ssh.buildSingBoxOutboundSSHBean
import io.throneproj.thronem.fmt.tuic.TuicBean
import io.throneproj.thronem.fmt.tuic.buildSingBoxOutboundTuicBean
import io.throneproj.thronem.fmt.v2ray.StandardV2RayBean
import io.throneproj.thronem.fmt.v2ray.buildSingBoxOutboundStandardV2RayBean
import io.throneproj.thronem.fmt.wireguard.WireGuardBean
import io.throneproj.thronem.fmt.wireguard.buildSingBoxEndpointWireGuardBean
import io.throneproj.thronem.ktx.JSONMap
import io.throneproj.thronem.ktx.asKxsMap
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.defaultOr
import io.throneproj.thronem.ktx.invariantPathString
import io.throneproj.thronem.ktx.isIpAddress
import io.throneproj.thronem.ktx.kxs
import io.throneproj.thronem.ktx.listByLineIgnoringComments
import io.throneproj.thronem.ktx.listByLineOrComma
import io.throneproj.thronem.ktx.mergeJson
import io.throneproj.thronem.ktx.serverAddressDomainStrategy
import io.throneproj.thronem.ktx.showToast
import io.throneproj.thronem.ktx.toJsonElementKxs
import io.throneproj.thronem.ktx.toJsonMapKxs
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL
import io.throneproj.thronem.logLevelString
import io.throneproj.thronem.platform.PlatformInfo
import io.throneproj.thronem.repository.resolveRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonPrimitive

// Inbound
const val TAG_MIXED = "mixed-in"
const val TAG_TUN = "tun-in"
const val TAG_DNS_IN = "dns-in" // strategic

// Outbound
const val TAG_DIRECT = "direct"
const val TAG_BLOCK = "block"
const val TAG_BRIDGE = "bridge"

// DNS
const val TAG_DNS_REMOTE = "dns-remote"
const val TAG_DNS_DIRECT = "dns-direct"
const val TAG_DNS_LOCAL = "dns-local"
const val TAG_DNS_FAKE = "dns-fake"
const val TAG_DNS_HOSTS = "dns-hosts"
const val TAG_DNS_MDNS = "dns-mdns"

// Service
// HTTP client
const val TAG_HTTP_CLIENT_DEFAULT = "http-default"

const val LOCALHOST4 = "127.0.0.1"
const val LOCALHOST_NAME = "localhost"

// For a certain version schema, maybe we should use [typebox](https://github.com/jiang-zhexin/typebox) ?
const val CONFIG_SCHEMA_URL = "https://sing-box.sagernet.org/schema.json"

val DNS_QUERY_TYPE_ADDRESS get() = listOf("A", "AAAA")

private class DNSServerGroup(val primaryTag: String, serverCount: Int) {
    val serverTags = List(serverCount) { index ->
        if (index == 0) {
            primaryTag
        } else {
            "$primaryTag-$index"
        }
    }
    val races get() = serverTags.size > 1

    fun routeRules(buildBasicRules: DNSRule_Default.() -> Unit): List<JSONMap> {
        if (!races) {
            return listOf(
                DNSRule_Default().apply {
                    buildBasicRules()
                    server = primaryTag
                }.asKxsMap(),
            )
        }
        return serverTags.map { serverTag ->
            DNSRule_Default().apply {
                buildBasicRules()
                action = SingBoxOptions.ACTION_EVALUATE
                server = serverTag
                tag = serverTag
            }.asKxsMap()
        } + serverTags.map { serverTag ->
            DNSRule_Default().apply {
                match_response = JsonPrimitive(serverTag)
                action = SingBoxOptions.ACTION_RESPOND
                race = true
            }.asKxsMap()
        }
    }
}

class ConfigBuildResult(
    val configJson: String,
    val metadata: ConfigMetadata,
)

class ConfigMetadata(
    val mainTag: String,
    val trafficProfiles: List<ProxyEntity>,
    val tagToID: Map<String, Long>,
    val trafficGraph: Map<String, TrafficNode> = emptyMap(),
)

data class TrafficNode(
    /** Sub profiles */
    val profileIDs: Set<Long>,
    val detour: String? = null,
    /** Tags this outbound can resolve to when it is a selector, empty otherwise. */
    val memberTags: List<String> = emptyList(),
)

suspend fun buildConfig(
    proxy: ProxyEntity, forTest: Boolean = false, forExport: Boolean = false,
): ConfigBuildResult {
    val repository = resolveRepository()

    if (proxy.type == TYPE_CONFIG) {
        val bean = proxy.configBean!!
        if (bean.type == ConfigBean.TYPE_CONFIG) {
            val tagProxy = bean.displayName()
            return ConfigBuildResult(
                bean.config,
                ConfigMetadata(
                    mainTag = tagProxy,
                    trafficProfiles = listOf(proxy),
                    tagToID = mapOf(tagProxy to proxy.id),
                ),
            )
        }
    }

    val trafficProfiles = LinkedHashMap<Long, ProxyEntity>()
    // Rules target the root tag returned by each independently built profile or chain.
    val rootTagMap = HashMap<Long, String>()
    val tagToID = HashMap<String, Long>()
    val trafficGraph = HashMap<String, TrafficNode>()
    val optionsToMerge = proxy.requireBean().customConfigJson

    data class ChainEntryKey(val entityId: Long, val referencePath: List<Long>)

    data class ChainEntry(
        val entity: ProxyEntity,
        val referencePath: List<Long> = emptyList(),
    ) {
        val key get() = ChainEntryKey(entity.id, referencePath)

        fun copyForBuild() = copy(
            entity = entity.copy().putBean(entity.requireBean().clone()),
        )
    }

    data class ChainLink(val from: ChainEntryKey, val to: ChainEntryKey)

    data class ResolvedChain(
        val entries: List<ChainEntry> = emptyList(),
        val root: ChainEntry? = null,
        val exits: List<ChainEntry> = emptyList(),
        val links: List<ChainLink> = emptyList(),
        val proxySetMembers: Map<ChainEntryKey, List<ChainEntry>> = emptyMap(),
        /**
         * Profiles that own hops but emit no outbound of their own, such as chains.
         * They carry the traffic of the hops they expand to.
         */
        val containers: List<ChainEntry> = emptyList(),
    )

    fun ChainEntryKey.describe(): String = buildString {
        append(entityId)
        if (referencePath.isNotEmpty()) {
            append(" @ ")
            append(referencePath.joinToString(" -> "))
        }
    }

    fun mergeResolvedChains(chains: List<ResolvedChain>, connect: Boolean): ResolvedChain {
        val entries = LinkedHashMap<ChainEntryKey, ChainEntry>()
        val containers = LinkedHashMap<ChainEntryKey, ChainEntry>()
        val links = LinkedHashSet<ChainLink>()
        val continuationByFrom = HashMap<ChainEntryKey, ChainEntryKey>()
        val proxySetMembers = LinkedHashMap<ChainEntryKey, MutableList<ChainEntry>>()
        var root: ChainEntry? = null
        var exits = emptyList<ChainEntry>()

        fun addLink(link: ChainLink) {
            if (link.from == link.to) return
            val previousTarget = continuationByFrom.putIfAbsent(link.from, link.to)
            if (previousTarget != null && previousTarget != link.to) {
                error(
                    "Conflicting proxy continuation: ${link.from.describe()} -> " +
                        "${previousTarget.describe()} and ${link.to.describe()}",
                )
            }
            links.add(link)
        }

        for (chain in chains) {
            val chainRoot = chain.root ?: continue
            val duplicateEntry = chain.entries.firstOrNull { entries.containsKey(it.key) }
            if (duplicateEntry != null) {
                error("Duplicate proxy reference: ${duplicateEntry.key.describe()}")
            }

            for (entry in chain.entries) {
                entries.putIfAbsent(entry.key, entry)
            }
            for (container in chain.containers) {
                containers.putIfAbsent(container.key, container)
            }
            chain.links.forEach(::addLink)
            for ((proxySet, members) in chain.proxySetMembers) {
                val mergedMembers = proxySetMembers.getOrPut(proxySet) { mutableListOf() }
                for (member in members) {
                    if (mergedMembers.none { it.key == member.key }) {
                        mergedMembers.add(member)
                    }
                }
            }

            if (root == null) {
                root = chainRoot
            }
            if (connect) {
                for (exit in exits) {
                    if (exit.key != chainRoot.key) {
                        addLink(ChainLink(exit.key, chainRoot.key))
                    }
                }
                exits = chain.exits
            } else {
                exits = (exits + chain.exits).distinctBy { it.key }
            }
        }

        return ResolvedChain(
            entries = entries.values.toList(),
            root = root,
            exits = exits.distinctBy { it.key },
            links = links.toList(),
            proxySetMembers = proxySetMembers,
            containers = containers.values.toList(),
        )
    }

    fun ResolvedChain.alwaysReferencedKeys(): Set<ChainEntryKey> {
        val continuations = links.groupBy({ it.from }, { it.to })

        fun reachableFrom(start: ChainEntryKey): Set<ChainEntryKey> {
            val reached = LinkedHashSet<ChainEntryKey>()
            val pending = ArrayDeque(listOf(start))
            while (pending.isNotEmpty()) {
                val key = pending.removeFirst()
                if (!reached.add(key)) continue
                pending += continuations[key].orEmpty()
                pending += proxySetMembers[key].orEmpty().map { it.key }
            }
            return reached
        }

        val referenced = LinkedHashSet<ChainEntryKey>()
        val pending = ArrayDeque(listOfNotNull(root?.key))
        while (pending.isNotEmpty()) {
            val key = pending.removeFirst()
            if (!referenced.add(key)) continue
            pending += continuations[key].orEmpty()
            val members = proxySetMembers[key].orEmpty()
            if (members.isNotEmpty()) {
                pending += members
                    .map { reachableFrom(it.key) }
                    .reduce { shared, memberKeys -> shared intersect memberKeys }
            }
        }
        return referenced
    }

    val resolvingReferences = LinkedHashSet<Long>()

    fun List<Long>.requireNoDuplicateReferences(container: String) {
        val seen = HashSet<Long>()
        val duplicates = filterNot(seen::add).distinct()
        if (duplicates.isNotEmpty()) {
            error("Duplicate proxy reference in $container: ${duplicates.joinToString(", ")}")
        }
    }

    suspend fun ProxyEntity.resolveChain(
        referencePath: List<Long> = emptyList(),
        register: Boolean = true,
    ): ResolvedChain {
        if (register && !resolvingReferences.add(id)) {
            val cycle = (resolvingReferences.dropWhile { it != id } + id).joinToString(" -> ")
            error("Circular proxy reference: $cycle")
        }

        return try {
            when (val bean = requireBean()) {
                is ChainBean -> {
                    bean.proxies.requireNoDuplicateReferences("chain $id")
                    val requestedProxyIds = bean.proxies.asReversed()
                    val beans = ThroneDatabase.proxyDao.getEntities(requestedProxyIds)
                    val beansMap = beans.associateBy { it.id }
                    val missingProxyIds = requestedProxyIds.filterNot(beansMap::containsKey)
                    if (missingProxyIds.isNotEmpty()) {
                        error(
                            "Missing proxy reference in chain $id: " +
                                missingProxyIds.joinToString(", "),
                        )
                    }
                    val resolved = mergeResolvedChains(
                        requestedProxyIds.map { proxyId ->
                            beansMap.getValue(proxyId).resolveChain(referencePath + id)
                        },
                        connect = true,
                    )
                    resolved.root ?: error("Proxy chain $id has no members")
                    // The chain emits no outbound of its own, but it earns the traffic
                    // its hops carry, so keep it as a container of this occurrence.
                    resolved.copy(
                        containers = resolved.containers + ChainEntry(this, referencePath),
                    )
                }

                is ProxySetBean -> {
                    val memberProfiles = bean.resolveMembers(id, failOnMissing = true)

                    val memberChains = mutableListOf<ResolvedChain>()
                    for (member in memberProfiles) {
                        memberChains.add(
                            member.resolveChain(referencePath + id),
                        )
                    }

                    if (memberChains.isEmpty()) {
                        error("Proxy set $id has no usable members")
                    }

                    val members = mergeResolvedChains(memberChains, connect = false)
                    val proxySetEntry = ChainEntry(this, referencePath)
                    ResolvedChain(
                        entries = members.entries + proxySetEntry,
                        root = proxySetEntry,
                        exits = members.exits,
                        links = members.links,
                        proxySetMembers = members.proxySetMembers + mapOf(
                            proxySetEntry.key to memberChains.mapNotNull { it.root }
                                .distinctBy { it.key },
                        ),
                        containers = members.containers,
                    )
                }

                else -> {
                    val entry = ChainEntry(this, referencePath)
                    ResolvedChain(
                        entries = listOf(entry),
                        root = entry,
                        exits = listOf(entry),
                    )
                }
            }
        } finally {
            if (register) resolvingReferences.remove(id)
        }
    }

    val logLevel = DataStore.logLevel.get()
    val extraRules = if (forTest) {
        emptyList()
    } else {
        ProfileManager.enabledRules().first()
    }
    val extraProxies =
        if (forTest) {
            mapOf()
        } else {
            ThroneDatabase.proxyDao.getEntities(
                extraRules.mapNotNull { rule ->
                    rule.outbound.takeIf { it > 0 && it != proxy.id }
                }.toHashSet().toList(),
            ).associateBy { it.id }
        }
    val userDNSRuleList = mutableListOf<JSONMap>()
    val domainListDNSDirectForce = mutableSetOf<String>()
    val bypassDNSBeans = hashSetOf<AbstractBean>()
    val isVPN = DataStore.serviceMode.get() == Key.MODE_VPN
    val allowAccess = DataStore.allowAccess.get()
    val bind = if (!forTest && allowAccess) {
        "0.0.0.0"
    } else {
        LOCALHOST4
    }
    val remoteDns = DataStore.remoteDns.get().listByLineIgnoringComments()
    val directDNS = DataStore.directDns.get().listByLineIgnoringComments()
    if (remoteDns.isEmpty()) error("missing remote DNS")
    if (directDNS.isEmpty()) error("missing direct DNS")
    val remoteDNSGroup = DNSServerGroup(TAG_DNS_REMOTE, remoteDns.size)
    val directDNSGroup = DNSServerGroup(TAG_DNS_DIRECT, directDNS.size)
    val fakeDNSGroup = DNSServerGroup(TAG_DNS_FAKE, 1)
    val mDNSInterfaces = DataStore.mDNS.get()
        .blankAsNull()
        ?.listByLineOrComma()
        ?.takeIf { it.isNotEmpty() }
    val localDNSPort = DataStore.localDNSPort.get().takeIf { it > 0 }
    val useFakeDns = !forTest && DataStore.enableFakeDns.get()
    val fakeDNSForAll = useFakeDns && DataStore.fakeDNSForAll.get()
    val dnsHosts = DataStore.dnsHosts.get()
        .listByLineIgnoringComments()
        .mapNotNull { line ->
            val tokens = line.split("\\s+".toRegex()) // Handle direct copy from host file.
            if (tokens.size < 2) return@mapNotNull null
            val host = tokens[0]
            val ips = tokens.drop(1).toMutableList()
            host to ips
        }
        .toMap()
        .takeIf { it.isNotEmpty() }
    val networkStrategy = DataStore.networkStrategy.get()
    val networkInterfaceStrategy = DataStore.networkInterfaceType.get()
    val networkPreferredInterfaces = DataStore.networkPreferredInterfaces.get().toList()
    val defaultStrategy = networkStrategy.blankAsNull()
    val serverDomainStrategy = serverAddressDomainStrategy()
    val disableTcpKeepAlive = DataStore.disableTcpKeepAlive.get()
    val tcpKeepAliveIdle = DataStore.tcpKeepAliveIdle.get().blankAsNull()
    val tcpKeepAliveInterval = DataStore.tcpKeepAliveInterval.get().blankAsNull()
    val mixedPort = DataStore.mixedPort.get()
    lateinit var mainTag: String

    val readableNames = mutableSetOf(TAG_DIRECT, TAG_BLOCK)
    val vpnWithPushDNS = mutableMapOf<String, String>() // endpointTag:dnsType

    return MyOptions().apply {
        `$schema` = CONFIG_SCHEMA_URL
        if (!forTest) experimental = ExperimentalOptions().apply {
            if (!forExport) {
                if (DataStore.isExpert.get()) {
                    DataStore.debugListen.get().blankAsNull()?.let {
                        debug = SingBoxOptions.DebugOptions().apply {
                            listen = it
                        }
                    }
                }
            }
            cache_file = CacheFileOptions().apply {
                enabled = true
                store_fakeip = true
                path = "../cache/cache.db"
            }
        }

        log = LogOptions().apply {
            level = logLevelString(logLevel)
        }

        if (DataStore.ntpEnable.get()) ntp = NTPOptions().apply {
            enabled = true
            server = DataStore.ntpAddress.get()
            server_port = DataStore.ntpPort.get()
            interval = DataStore.ntpInterval.get()

            if (!server!!.isIpAddress()) {
                domainListDNSDirectForce.add(server!!)
            }
        }

        dns = MyDNSOptions().apply {
            servers = mutableListOf()
            rules = mutableListOf()
            if (!forTest) {
                DataStore.dnsOptimisticCache.get().blankAsNull()?.let { it ->
                    optimistic = OptimisticDNSOptions().apply {
                        enabled = true
                        timeout = it
                    }
                }
            }
        }

        inbounds = mutableListOf()

        if (!forTest) {
            if (isVPN) inbounds!!.add(
                Inbound_TunOptions().apply {
                    type = SingBoxOptions.TYPE_TUN
                    tag = TAG_TUN
                    stack = when (DataStore.tunIpStack.get()) {
                        TunIpStack.GVISOR -> "gvisor"
                        TunIpStack.SYSTEM -> "system"
                        else -> "mixed"
                    }
                    mtu = DataStore.mtu.get()
                    // Hijack intercepts port 53 at the TUN layer and calls
                    // router.HijackDNSPacket (with searchProcessInfo) directly,
                    // bypassing route rule matching.
                    // Fixed upstream: SagerNet/sing-box@339b1934 (codeberg thronem#87)
                    dns_mode = SingBoxOptions.TUN_DNS_MODE_HIJACK
                    applyPlatformConfig()
                    when (networkStrategy) {
                        SingBoxOptions.STRATEGY_IPV4_ONLY -> {
                            address = mutableListOf(VpnConstants.PRIVATE_VLAN4_CLIENT + "/28")
                        }

                        SingBoxOptions.STRATEGY_IPV6_ONLY -> {
                            address = mutableListOf(VpnConstants.PRIVATE_VLAN6_CLIENT + "/126")
                        }

                        else -> {
                            address = mutableListOf(
                                VpnConstants.PRIVATE_VLAN4_CLIENT + "/28",
                                VpnConstants.PRIVATE_VLAN6_CLIENT + "/126",
                            )
                        }
                    }
                },
            )
            inbounds!!.add(
                Inbound_HTTPMixedOptions().apply {
                    type = SingBoxOptions.TYPE_MIXED
                    tag = TAG_MIXED
                    listen = bind
                    listen_port = mixedPort
                    if (!PlatformInfo.isAndroid) {
                        if (DataStore.appendHttpProxy.get()) {
                            set_system_proxy = true
                        }
                    }
                    val inboundUsername = DataStore.inboundUsername.get()
                    val inboundPassword = DataStore.inboundPassword.get()
                    if (inboundUsername.isNotBlank() || inboundPassword.isNotBlank()) {
                        users = mutableListOf(
                            User().apply {
                                username = inboundUsername
                                password = inboundPassword
                            },
                        )
                    }
                },
            )
        }

        outbounds = mutableListOf()

        // init routing object
        route = MyRouteOptions().apply {
            auto_detect_interface = true
            rules = mutableListOf()
            rule_set = mutableListOf()
            // Android always searches processes through the platform interface,
            // so this option only means something on desktop.
            // https://github.com/SagerNet/sing-box/commit/4b1b00a4f6729a027a653f417cda0701c6a32934
            if (!PlatformInfo.isAndroid && !forTest && DataStore.forcedSearchProcess.get()) {
                find_process = true
            }
        }

        // returns outbound tag
        // chainId == 0L => main proxy
        suspend fun buildChain(chainId: Long, entity: ProxyEntity): String {
            // The main proxy is an in-memory synthesized root (id borrowed from
            // proxy_sets table — see BaseService). It can never appear as a member
            // of any chain/set (never persisted to proxy_entities), so skip adding
            // its id to resolvingReferences. That avoids a false "circular" report
            // when the root's id coincides with a real member's id from a different
            // auto-increment sequence.
            val isRoot = chainId == 0L
            val resolvedChain = entity.resolveChain(register = !isRoot)
            val profileList = resolvedChain.entries.map { it.copyForBuild() }
            val profileEntriesByKey = profileList.associateBy { it.key }
            var currentOutbound = mutableMapOf<String, Any?>()

            val chainTag = "c-$chainId"

            val isProxySet = entity.requireBean() is ProxySetBean
            val outboundsByTag = HashMap<String, JSONMap>()
            val outboundChunkStart = outbounds!!.size

            fun addReadableName(name: String): String {
                if (readableNames.add(name)) {
                    return name
                }
                var count = 0
                var newName = "$name-$count"
                while (!readableNames.add(newName)) {
                    count++
                    newName = "$name-$count"
                }
                return newName
            }

            val reservedTags = HashMap<ChainEntryKey, String>()

            fun reserveTag(entry: ChainEntry): String {
                reservedTags[entry.key]?.let { return it }
                val tag = addReadableName(entry.entity.displayName())
                reservedTags[entry.key] = tag
                return tag
            }

            fun JSONMap.detourTo(tag: String) {
                this["detour"] = tag
                remove("domain_resolver")
            }

            suspend fun connectChainNode(previousEntry: ChainEntry, currentTag: String) {
                val previousTag = checkNotNull(reservedTags[previousEntry.key])
                checkNotNull(outboundsByTag[previousTag]).detourTo(currentTag)
            }

            val entriesWithContinuation = resolvedChain.links.mapTo(HashSet()) { it.from }
            val alwaysReferenced = resolvedChain.alwaysReferencedKeys()

            fun addDNSDirectForce(bean: AbstractBean) {
                if (bean is ChainBean || bean is ProxySetBean) return

                bean.serverAddress.takeIf { it.isNotBlank() }?.let { address ->
                    if (!address.isIpAddress()) {
                        domainListDNSDirectForce.add(address)
                    }
                }
            }

            // Resolve DNS for every actual dial target. A flattened iteration is ambiguous when
            // a selector contains several chains with independent exits.
            for (entry in profileList) {
                if (entry.key in entriesWithContinuation) {
                    addDNSDirectForce(entry.entity.requireBean())
                }
            }
            for (exit in resolvedChain.exits) {
                profileEntriesByKey[exit.key]?.entity?.requireBean()?.let(::addDNSDirectForce)
            }

            profileList.forEach { entry ->
                val proxyEntity = entry.entity
                val bean = proxyEntity.requireBean()

                val tagOut = reserveTag(entry)

                currentOutbound = when (bean) {
                    is ConfigBean -> bean.config.toJsonMapKxs()

                    is ShadowTLSBean -> // before StandardV2RayBean
                        buildSingBoxOutboundShadowTLSBean(bean).asKxsMap()

                    is StandardV2RayBean -> // http/trojan/vmess/vless
                        buildSingBoxOutboundStandardV2RayBean(bean).asKxsMap()

                    is HysteriaBean -> buildSingBoxOutboundHysteriaBean(bean).asKxsMap()

                    is TuicBean -> buildSingBoxOutboundTuicBean(bean).asKxsMap()

                    is SOCKSBean -> buildSingBoxOutboundSocksBean(bean).asKxsMap()

                    is ShadowsocksBean -> buildSingBoxOutboundShadowsocksBean(bean).asKxsMap()

                    is SnellBean -> buildSingBoxOutboundSnellBean(bean).asKxsMap()

                    is WireGuardBean -> buildSingBoxEndpointWireGuardBean(bean).asKxsMap()

                    is MasqueBean -> buildSingBoxOutboundMasqueBean(bean).asKxsMap()

                    is OpenConnectBean -> {
                        vpnWithPushDNS[tagOut] = SingBoxOptions.DNS_TYPE_OPENCONNECT
                        buildSingBoxEndpointOpenConnectBean(bean).asKxsMap()
                    }

                    is OpenVPNBean -> {
                        vpnWithPushDNS[tagOut] = SingBoxOptions.DNS_TYPE_OPENVPN
                        buildSingBoxEndpointOpenVPNBean(bean).asKxsMap()
                    }

                    is SSHBean -> buildSingBoxOutboundSSHBean(bean).asKxsMap()

                    is DirectBean -> buildSingBoxOutboundDirectBean(bean).asKxsMap()

                    is AnyTLSBean -> buildSingBoxOutboundAnyTLSBean(bean).asKxsMap()

                    is JuicityBean -> buildSingBoxOutboundJuicityBean(bean).asKxsMap()

                    is NaiveBean -> buildSingBoxOutboundNaiveBean(bean).asKxsMap()

                    is ProxySetBean -> {
                        val memberTags = LinkedHashSet<String>()
                        for (member in resolvedChain.proxySetMembers[entry.key].orEmpty()) {
                            memberTags.add(reserveTag(member))
                        }
                        val tags = memberTags.toList().filterNot { it == tagOut }
                        buildSingBoxOutboundProxySetBean(bean, tags).asKxsMap()
                    }

                    else -> throw IllegalStateException("can't reach")
                }

                currentOutbound.apply {
                    if (!forTest && bean !is ProxySetBean) {
                        if (disableTcpKeepAlive) {
                            this["disable_tcp_keep_alive"] = true
                        } else {
                            tcpKeepAliveIdle?.let {
                                this["tcp_keep_alive"] = it
                            }
                            tcpKeepAliveInterval?.let {
                                this["tcp_keep_alive_interval"] = it
                            }
                        }
                        if (networkPreferredInterfaces.isNotEmpty()) {
                            this["network_type"] = networkPreferredInterfaces
                            this["network_strategy"] =
                                mapNetworkInterfaceStrategy(networkInterfaceStrategy)
                        }
                    }
                }

                // internal
                currentOutbound.apply {
                    // Set uot here so that naive socks can apply it.
                    // And it is not necessarily to enable it when enabling multiplex.
                    if (bean.needUDPOverTCP && this["multiplex"] == null) {
                        this["udp_over_tcp"] = true
                    }

                    if (bean !is ProxySetBean && (!forTest || serverDomainStrategy != null)) {
                        this["domain_resolver"] = DomainResolveOptions().apply {
                            server = if (forTest) {
                                TAG_DNS_LOCAL
                            } else {
                                TAG_DNS_DIRECT
                            }
                            strategy = serverDomainStrategy
                        }.asKxsMap()
                    }

                    if (isEndpoint(this["type"].toString()) && entry.key !in alwaysReferenced) {
                        this["on_demand"] = true
                    }

                    // custom JSON merge
                    bean.customOutboundJson.blankAsNull()?.toJsonMapKxs()?.let {
                        mergeJson(it, currentOutbound)
                    }
                    if (this["detour"] != null) {
                        remove("domain_resolver")
                    }
                }

                currentOutbound["tag"] = tagOut
                tagToID[tagOut] = proxyEntity.id
                outboundsByTag[tagOut] = currentOutbound

                bean.finalAddress = bean.serverAddress
                bean.finalPort = bean.serverPort

                outbounds!!.add(currentOutbound)
            }

            // Keep terminal profiles available for the bypass lookup pass below.
            for (exit in resolvedChain.exits) {
                profileEntriesByKey[exit.key]?.entity?.requireBean()?.let(bypassDNSBeans::add)
            }

            for (link in resolvedChain.links) {
                val previousEntry = checkNotNull(profileEntriesByKey[link.from])
                val currentTag = checkNotNull(reservedTags[link.to])
                connectChainNode(previousEntry, currentTag)
            }

            // Mirror the shape of what was just emitted, so traffic accounting can walk
            // a connection back to every profile that carried it. ProxyEntity equality
            // includes mutable bean state (finalAddress/finalPort), so profiles are
            // collected by their stable database id.
            val chainIDs = resolvedChain.containers.mapTo(HashSet()) { it.entity.id }
            val detourByFrom = resolvedChain.links.associate { it.from to it.to }
            for (entry in profileList) {
                trafficProfiles.putIfAbsent(entry.entity.id, entry.entity)
                val owners = entry.referencePath.filterTo(LinkedHashSet()) { it in chainIDs }
                owners.add(entry.entity.id)
                trafficGraph[checkNotNull(reservedTags[entry.key])] = TrafficNode(
                    profileIDs = owners,
                    detour = detourByFrom[entry.key]?.let { checkNotNull(reservedTags[it]) },
                    memberTags = resolvedChain.proxySetMembers[entry.key].orEmpty().map {
                        checkNotNull(reservedTags[it.key])
                    },
                )
            }
            for (container in resolvedChain.containers) {
                trafficProfiles.putIfAbsent(container.entity.id, container.entity)
            }
            trafficProfiles.putIfAbsent(entity.id, entity)

            val firstChainTag = checkNotNull(reservedTags[checkNotNull(resolvedChain.root).key])
            if (chainId == 0L) {
                mainTag = firstChainTag
            }
            if (isProxySet) {
                val proxySetEntry = profileList.first {
                    it.entity.id == entity.id && it.referencePath.isEmpty()
                }
                val proxySetTag = checkNotNull(reservedTags[proxySetEntry.key])

                // Keep selector above its children.
                val proxySetIndex = outbounds!!.indexOfLast { it["tag"] == proxySetTag }
                if (proxySetIndex in outboundChunkStart..outbounds!!.lastIndex) {
                    outbounds!!.add(outboundChunkStart, outbounds!!.removeAt(proxySetIndex))
                }
            }

            return firstChainTag
        }

        // build outbounds
        rootTagMap[proxy.id] = buildChain(0, proxy)
        // build outbounds from route item
        extraProxies.forEach { (key, p) ->
            rootTagMap[key] = buildChain(key, p)
        }

        // apply user rules
        for (rule in extraRules) {
            val (packageNames, processRules) = parseRuleProcessRules(
                rule.packages,
                defaultToPackage = PlatformInfo.isAndroid,
            )

            val ruleObj = Rule_Default().apply {
                action = SingBoxOptions.ACTION_ROUTE
                if (rule.invert) {
                    invert = true
                }
                if (packageNames.isNotEmpty()) {
                    package_name = packageNames.toMutableList()
                }
                rule.packageNameRegex.blankAsNull()?.let {
                    // Do not use listByLineOrComma for regex
                    package_name_regex = it.split("\n").toMutableList()
                }
                if (processRules.isNotEmpty()) {
                    makeProcessRule(processRules)
                }
                var domainList: List<RuleItem> = listOf()
                var ipList: List<RuleItem> = listOf()
                if (rule.domains.isNotBlank()) {
                    domainList = RuleItem.parseRules(rule.domains.listByLineOrComma(), true)
                    makeCommonRule(domainList, false)
                }
                if (rule.ip.isNotBlank()) {
                    ipList = RuleItem.parseRules(rule.ip.listByLineOrComma(), false)
                    makeCommonRule(ipList, true)
                }
                if (rule.port.isNotBlank()) {
                    port = mutableListOf()
                    port_range = mutableListOf()
                    rule.port.listByLineOrComma().forEach {
                        if (it.contains(":")) {
                            port_range!!.add(it)
                        } else {
                            it.toIntOrNull()?.apply { port!!.add(this) }
                        }
                    }
                }
                if (rule.sourcePort.isNotBlank()) {
                    source_port = mutableListOf()
                    source_port_range = mutableListOf()
                    rule.sourcePort.listByLineOrComma().forEach {
                        if (it.contains(":")) {
                            source_port_range!!.add(it)
                        } else {
                            it.toIntOrNull()?.apply { source_port!!.add(this) }
                        }
                    }
                }
                if (rule.network.isNotEmpty()) {
                    network = rule.network.toMutableList()
                }
                if (rule.source.isNotBlank()) {
                    val sourceIPs = mutableListOf<String>()
                    for (source in rule.source.listByLineOrComma()) {
                        if (source == RuleItem.CONTENT_PRIVATE) {
                            source_ip_is_private = true
                        } else {
                            sourceIPs.add(source)
                        }
                    }
                    if (sourceIPs.isNotEmpty()) source_ip_cidr = sourceIPs
                }
                if (rule.protocol.isNotEmpty()) {
                    protocol = rule.protocol.toMutableList()
                }
                if (rule.clientType.isNotBlank()) {
                    client = rule.clientType.listByLineOrComma().toMutableList()
                }
                if (rule.ssid.isNotBlank()) {
                    wifi_ssid = rule.ssid.listByLineOrComma().toMutableList()
                }
                if (rule.bssid.isNotBlank()) {
                    wifi_bssid = rule.bssid.listByLineOrComma().toMutableList()
                }
                if (rule.clashMode.isNotBlank()) {
                    clash_mode = rule.clashMode
                }
                if (rule.networkType.isNotEmpty()) {
                    network_type = rule.networkType.toMutableList()
                }
                if (rule.networkIsExpensive) {
                    network_is_expensive = true
                }
                if (rule.networkInterfaceAddress.isNotEmpty()) {
                    network_interface_address = rule.networkInterfaceAddress
                        .mapValuesTo(mutableMapOf()) { (_, addresses) ->
                            addresses.listByLineOrComma().toMutableList()
                        }
                }

                fun RuleItem.isResponseOnlyRule(): Boolean {
                    return content == RuleItem.CONTENT_ANY || content == RuleItem.CONTENT_PRIVATE
                }

                val requestDNSRules = domainList.filter { it.dns && !it.isResponseOnlyRule() }
                val responseDNSRules = buildList {
                    addAll(domainList.filter { it.dns && it.isResponseOnlyRule() })
                    addAll(ipList.filter { it.dns })
                }

                fun DNSRule_Default.applyDnsBase(
                    useFakeQueryScope: Boolean = false,
                ): DNSRule_Default {
                    if (rule.invert) {
                        invert = true
                    }
                    if (packageNames.isNotEmpty()) package_name = packageNames.toMutableList()
                    rule.packageNameRegex.blankAsNull()?.let {
                        package_name_regex = mutableListOf(it)
                    }
                    if (processRules.isNotEmpty()) {
                        makeProcessRule(processRules)
                    }
                    if (requestDNSRules.isNotEmpty()) {
                        makeCommonRule(requestDNSRules)
                    }
                    if (useFakeQueryScope) {
                        inbound = mutableListOf(TAG_TUN)
                        query_type = DNS_QUERY_TYPE_ADDRESS.toMutableList()
                    }
                    return this
                }

                fun buildDnsRules(
                    action: String? = null,
                    group: DNSServerGroup? = null,
                    useFakeQueryScope: Boolean = false,
                ): MutableList<JSONMap>? {
                    val hasResponseRule = DNSRule_Default().apply {
                        makeResponseRule(responseDNSRules)
                    }.let { !it.checkEmpty() }
                    val terminalAction = if (
                        hasResponseRule &&
                        action == SingBoxOptions.ACTION_ROUTE &&
                        group?.primaryTag == TAG_DNS_REMOTE
                    ) {
                        SingBoxOptions.ACTION_RESPOND
                    } else {
                        action
                    }
                    val terminalRule = DNSRule_Default().applyDnsBase(useFakeQueryScope).apply {
                        if (hasResponseRule) {
                            match_response = JsonPrimitive(true)
                            makeResponseRule(responseDNSRules)
                        }
                        this.action = terminalAction
                        this.server = if (terminalAction == SingBoxOptions.ACTION_RESPOND) {
                            null
                        } else {
                            group?.primaryTag
                        }
                    }
                    if (!hasResponseRule) {
                        if (terminalRule.checkEmpty()) return null
                        if (group?.races == true && terminalAction == SingBoxOptions.ACTION_ROUTE) {
                            return group.routeRules {
                                applyDnsBase(useFakeQueryScope)
                            }.toMutableList()
                        }
                        return mutableListOf(terminalRule.asKxsMap())
                    }
                    val evaluateRule = DNSRule_Default().applyDnsBase(useFakeQueryScope).apply {
                        this.action = SingBoxOptions.ACTION_EVALUATE
                        this.server = remoteDNSGroup.primaryTag
                    }
                    return mutableListOf(
                        evaluateRule.asKxsMap(),
                        terminalRule.asKxsMap(),
                    )
                }

                var dnsRuleList: MutableList<JSONMap>? = null
                when (val ruleAction = rule.action) {
                    "", SingBoxOptions.ACTION_ROUTE -> {
                        action = SingBoxOptions.ACTION_ROUTE

                        when (val outID = rule.outbound) {
                            RuleEntity.OUTBOUND_DIRECT -> {
                                if (dnsRuleList == null) {
                                    dnsRuleList = buildDnsRules(
                                        action = SingBoxOptions.ACTION_ROUTE,
                                        group = if (fakeDNSForAll) {
                                            fakeDNSGroup
                                        } else {
                                            directDNSGroup
                                        },
                                    )
                                }
                                outbound = TAG_DIRECT
                            }

                            RuleEntity.OUTBOUND_PROXY -> {
                                if (dnsRuleList == null) {
                                    dnsRuleList = buildDnsRules(
                                        action = SingBoxOptions.ACTION_ROUTE,
                                        group = if (useFakeDns) {
                                            fakeDNSGroup
                                        } else {
                                            remoteDNSGroup
                                        },
                                        useFakeQueryScope = useFakeDns,
                                    )
                                }
                                outbound = mainTag
                            }

                            RuleEntity.OUTBOUND_BLOCK -> {
                                if (dnsRuleList == null) {
                                    dnsRuleList = buildDnsRules(
                                        action = SingBoxOptions.ACTION_REJECT,
                                    )
                                }
                                outbound = TAG_BLOCK
                            }

                            RuleEntity.OUTBOUND_BRIDGE -> {
                                outbound = TAG_BRIDGE
                            }

                            else -> outbound = if (outID == proxy.id) {
                                mainTag
                            } else {
                                rootTagMap[outID] ?: ""
                            }
                        }
                    }

                    SingBoxOptions.ACTION_BYPASS -> {
                        action = ruleAction
                        outbound = when (val outID = rule.outbound) {
                            RuleEntity.OUTBOUND_PROXY -> mainTag
                            RuleEntity.OUTBOUND_DIRECT -> TAG_DIRECT
                            RuleEntity.OUTBOUND_BLOCK -> TAG_BLOCK
                            RuleEntity.OUTBOUND_BRIDGE -> TAG_BRIDGE
                            else -> if (outID == proxy.id) {
                                mainTag
                            } else {
                                rootTagMap[outID] ?: ""
                            }
                        }
                    }

                    SingBoxOptions.ACTION_ROUTE_OPTIONS -> {
                        action = ruleAction

                        override_address = rule.overrideAddress.blankAsNull()
                        override_port = rule.overridePort.takeIf { it > 0 }
                        if (rule.tlsFragment) {
                            tls_fragment = true
                            tls_fragment_fallback_delay =
                                rule.tlsFragmentFallbackDelay.blankAsNull()
                        }
                        if (rule.tlsRecordFragment) {
                            tls_record_fragment = true
                        }
                        tls_spoof = rule.tlsSpoof.blankAsNull()
                        tls_spoof_method = rule.tlsSpoofMethod.blankAsNull()
                    }

                    SingBoxOptions.ACTION_RESOLVE -> {
                        action = ruleAction

                        strategy = rule.resolveStrategy
                        if (rule.resolveDisableCache) {
                            disable_cache = true
                        }
                        rewrite_ttl = rule.resolveRewriteTTL.takeIf { it >= 0 }
                        client_subnet = rule.resolveClientSubnet.blankAsNull()
                    }

                    SingBoxOptions.ACTION_SNIFF -> {
                        action = ruleAction

                        timeout = rule.sniffTimeout.blankAsNull()
                        sniffer = rule.sniffers.takeIf { it.isNotEmpty() }?.toMutableList()
                    }

                    SingBoxOptions.ACTION_HIJACK_DNS -> {
                        action = ruleAction
                    }

                    SingBoxOptions.ACTION_REJECT -> {
                        if (dnsRuleList == null) {
                            dnsRuleList = buildDnsRules(
                                action = SingBoxOptions.ACTION_REJECT,
                            )
                        }
                        action = ruleAction
                    }

                    else -> error("unsupported action: $ruleAction")
                }

                rule.customDnsConfig.blankAsNull()?.toJsonMapKxs()?.let { customDns ->
                    if (dnsRuleList == null) {
                        dnsRuleList = mutableListOf(customDns)
                    } else {
                        mergeJson(customDns, dnsRuleList.last())
                    }
                }
                dnsRuleList?.let {
                    userDNSRuleList.addAll(it)
                }

            }

            fun addRule() {
                val ruleMap = ruleObj.asKxsMap()
                rule.customConfig.blankAsNull()?.let {
                    mergeJson(it.toJsonMapKxs(), ruleMap)
                }
                route!!.rules!!.add(ruleMap)
            }
            if (!rule.dnsOnly) {
                if (!ruleObj.checkEmpty()) {
                    // Empty or "route"
                    val needOutbound = when (ruleObj.action) {
                        null, "", SingBoxOptions.ACTION_ROUTE -> true
                        else -> false
                    }
                    if (needOutbound && ruleObj.outbound.isNullOrBlank()) {
                        showToast(
                            "Warning: " + rule.displayName() + ": A non-existent outbound was specified.",
                            long = true,
                        )
                    } else {
                        addRule()
                    }
                } else if (ruleObj.action != SingBoxOptions.ACTION_ROUTE) {
                    addRule()
                } else if (rule.domains.isBlank() && rule.ip.isBlank()) {
                    addRule()
                }
            }
        }

        outbounds!!.add(
            Outbound_DirectOptions().apply {
                tag = TAG_DIRECT
                type = SingBoxOptions.TYPE_DIRECT
                domain_resolver = DomainResolveOptions().apply {
                    server = if (forTest) {
                        TAG_DNS_LOCAL
                    } else {
                        TAG_DNS_DIRECT
                    }
                    strategy = defaultOr(
                        DataStore.domainStrategyForDirect.get()
                            .replace(DOMAIN_STRATEGY_AUTO, "")
                            .blankAsNull(),
                        { defaultStrategy },
                    )
                }

                if (!forTest) {
                    if (disableTcpKeepAlive) {
                        disable_tcp_keep_alive = false
                    } else {
                        tcp_keep_alive = tcpKeepAliveIdle
                        tcp_keep_alive_interval = tcpKeepAliveInterval
                    }
                    if (networkPreferredInterfaces.isNotEmpty()) {
                        network_type = networkPreferredInterfaces.toMutableList()
                        network_strategy = mapNetworkInterfaceStrategy(networkInterfaceStrategy)
                    }
                }
            }.asKxsMap(),
        )
        outbounds!!.add(
            Outbound().apply {
                tag = TAG_BLOCK
                type = SingBoxOptions.TYPE_BLOCK
            }.asKxsMap(),
        )
        if (!PlatformInfo.isAndroid && route!!.rules!!.any { it["outbound"] == TAG_BRIDGE }) outbounds!!.add(
            Outbound().apply {
                tag = TAG_BRIDGE
                type = SingBoxOptions.TYPE_BRIDGE
            }.asKxsMap(),
        )

        if (!forTest) localDNSPort?.let {
            inbounds!!.add(
                0,
                Inbound_DirectOptions().apply {
                    type = SingBoxOptions.TYPE_DIRECT
                    tag = TAG_DNS_IN
                    listen = bind
                    listen_port = it
                    override_address = "8.8.8.8"
                    override_port = 53
                },
            )
        }

        // Bypass lookup for the terminal profiles in each expanded graph.
        bypassDNSBeans.forEach {
            if (it is ChainBean || it is ProxySetBean) return@forEach
            var serverAddr = it.serverAddress

            if (it is ConfigBean) {
                val config = it.config.toJsonMapKxs()
                config["server"]?.let { server ->
                    serverAddr = server.toString()
                }
            }

            if (serverAddr.isNotBlank() && !serverAddr.isIpAddress()) {
                domainListDNSDirectForce.add(serverAddr)
            }
        }

        remoteDns.forEach {
            var address = it
            if (address.contains("://")) {
                address = address.substringAfter("://")
            }
            try {
                parseURL("https://$address").apply {
                    if (!host.isIpAddress()) {
                        domainListDNSDirectForce.add(host)
                    }
                }
            } catch (_: Exception) {
            }
        }

        remoteDNSGroup.serverTags.zip(remoteDns).forEach { (tag, link) ->
            dns!!.servers!!.add(
                buildDNSServer(
                    link,
                    mainTag,
                    tag,
                    DomainResolveOptions().apply {
                        server = TAG_DNS_DIRECT
                    },
                ),
            )
        }

        directDNSGroup.serverTags.zip(directDNS).forEach { (tag, link) ->
            dns!!.servers!!.add(
                buildDNSServer(
                    link,
                    null,
                    tag,
                    DomainResolveOptions().apply {
                        server = TAG_DNS_LOCAL
                    },
                ),
            )
        }

        // underlyingDns
        dns!!.servers!!.add(
            NewDNSServerOptions_LocalDNSServerOptions().apply {
                tag = TAG_DNS_LOCAL
                type = SingBoxOptions.DNS_TYPE_LOCAL
            },
        )

        // dns object user rules
        dns!!.rules!!.addAll(userDNSRuleList)

        if (forTest) {
            // Always use system DNS for urlTest
            dns!!.servers = mutableListOf(
                NewDNSServerOptions_LocalDNSServerOptions().apply {
                    tag = TAG_DNS_LOCAL
                    type = SingBoxOptions.DNS_TYPE_LOCAL
                },
            )
            dns!!.rules = mutableListOf()
        } else {
            // clash mode
            route!!.rules!!.add(
                0,
                Rule_Default().apply {
                    clash_mode = RuleEntity.MODE_GLOBAL
                    outbound = mainTag
                }.asKxsMap(),
            )
            route!!.rules!!.add(
                0,
                Rule_Default().apply {
                    clash_mode = RuleEntity.MODE_DIRECT
                    outbound = TAG_DIRECT
                }.asKxsMap(),
            )
            route!!.rules!!.add(
                0,
                Rule_Default().apply {
                    clash_mode = RuleEntity.MODE_BLOCK
                    action = SingBoxOptions.ACTION_REJECT
                }.asKxsMap(),
            )

            // built-in DNS rules
            // TUN hijack mode handles port 53 automatically;
            // only the local DNS inbound needs an explicit hijack rule.
            localDNSPort?.let {
                route!!.rules!!.add(
                    0,
                    Rule_Default().apply {
                        inbound = mutableListOf(TAG_DNS_IN)
                        action = SingBoxOptions.ACTION_HIJACK_DNS
                    }.asKxsMap(),
                )
            }

            // FakeDNS obj
            if (useFakeDns) {
                val fakeRange4 = if (networkStrategy == SingBoxOptions.STRATEGY_IPV6_ONLY) {
                    null
                } else {
                    DataStore.fakeDNSRange4.get().blankAsNull()
                }
                val fakeRange6 = if (networkStrategy == SingBoxOptions.STRATEGY_IPV4_ONLY) {
                    null
                } else {
                    DataStore.fakeDNSRange6.get().blankAsNull()
                }
                dns!!.servers!!.add(
                    NewDNSServerOptions_FakeIPDNSServerOptions().apply {
                        type = SingBoxOptions.DNS_TYPE_FAKEIP
                        tag = TAG_DNS_FAKE
                        inet4_range = fakeRange4
                        inet6_range = fakeRange6
                    },
                )
                dns!!.rules!!.add(
                    DNSRule_Default().apply {
                        inbound = mutableListOf(TAG_TUN)
                        server = TAG_DNS_FAKE
                        disable_cache = true
                        query_type = DNS_QUERY_TYPE_ADDRESS.toMutableList()
                    }.asKxsMap(),
                )
            }

            // Pre-filter:
            // Hosts [->mDNS] -> local

            fun addPreferredDNSRule(
                dnsServerTag: String,
                queryType: MutableList<String>? = null,
            ) {
                dns!!.rules!!.add(
                    0,
                    DNSRule_Default().apply {
                        preferred_by = mutableListOf(dnsServerTag)
                        server = dnsServerTag
                        query_type = queryType
                    }.asKxsMap(),
                )
            }

            addPreferredDNSRule(
                TAG_DNS_LOCAL,
                if (localDNSSupportRaw) {
                    null
                } else {
                    DNS_QUERY_TYPE_ADDRESS.toMutableList()
                },
            )

            // VPN with server-push DNS
            // What if a endpoint on demand while using this DNS? This may be a bug, change it until user noticing it.
            for ((endpointTag, dnsType) in vpnWithPushDNS) {
                val dnsTag = "dns-${endpointTag}"
                val server = when (dnsType) {
                    SingBoxOptions.DNS_TYPE_OPENCONNECT -> {
                        SingBoxOptions.NewDNSServerOptions_OpenConnectDNSServerOptions().apply {
                            type = dnsType
                            tag = dnsTag
                            endpoint = endpointTag
                        }
                    }

                    SingBoxOptions.DNS_TYPE_OPENVPN -> {
                        SingBoxOptions.NewDNSServerOptions_OpenVPNDNSServerOptions().apply {
                            type = dnsType
                            tag = dnsTag
                            endpoint = endpointTag
                        }
                    }

                    else -> error("unsupported VPN DNS type: $dnsType")
                }
                dns!!.servers!!.add(server)
                addPreferredDNSRule(dnsTag)
            }

            // mDNS
            // Make sure mDNS rule is before local, because raw local includes mDNS
            val resolveMDNSByLocal = localDNSSupportRaw && mDNSInterfaces == null
            if (!resolveMDNSByLocal) {
                dns!!.servers!!.add(
                    NewDNSServerOptions_MDNSDNSServerOptions().apply {
                        type = SingBoxOptions.DNS_TYPE_MDNS
                        tag = TAG_DNS_MDNS
                        `interface` = mDNSInterfaces?.toMutableList()
                    },
                )
                addPreferredDNSRule(TAG_DNS_MDNS)
            }

            dnsHosts?.let {
                dns!!.servers!!.add(
                    NewDNSServerOptions_HostsDNSServerOptions().apply {
                        type = SingBoxOptions.DNS_TYPE_HOSTS
                        tag = TAG_DNS_HOSTS
                        predefined = it.toMutableMap()
                    },
                )
                addPreferredDNSRule(TAG_DNS_HOSTS)
            }

            // clash mode
            dns!!.rules!!.addAll(
                0,
                remoteDNSGroup.routeRules {
                    clash_mode = RuleEntity.MODE_GLOBAL
                },
            )
            dns!!.rules!!.addAll(
                0,
                directDNSGroup.routeRules {
                    clash_mode = RuleEntity.MODE_DIRECT
                },
            )
            dns!!.rules!!.add(
                0,
                DNSRule_Default().apply {
                    clash_mode = RuleEntity.MODE_BLOCK
                    action = SingBoxOptions.ACTION_REJECT
                }.asKxsMap(),
            )

            if (domainListDNSDirectForce.isNotEmpty()) {
                dns!!.rules!!.addAll(
                    0,
                    directDNSGroup.routeRules {
                        domain = domainListDNSDirectForce.distinct().toMutableList()
                    },
                )
            }

            if (remoteDNSGroup.races) {
                dns!!.rules!!.addAll(remoteDNSGroup.routeRules {})
            }
        }
        route!!.final_ = mainTag
        if (!forTest) dns!!.final_ = remoteDNSGroup.primaryTag

        if (forExport) {
            http_clients = mutableListOf(
                SingBoxOptions.HTTPClient().apply {
                    tag = TAG_HTTP_CLIENT_DEFAULT
                    detour = mainTag
                },
            )
            route!!.default_http_client = TAG_HTTP_CLIENT_DEFAULT
        }

        val ruleSetSource = (if (forExport) {
            exportRuleSetSource()
        } else {
            null
        }) ?: RuleSetSource.Local(
            routeGeoDir(repository.externalAssetsDir).invariantPathString(),
        )
        buildRuleSets(ruleSetSource)
        partitionEndpoints()
    }.let {
        val optionsMap = it.toKxs().asKxsMap().apply {
            optionsToMerge.blankAsNull()?.toJsonMapKxs()?.let { jsonMap ->
                mergeJson(jsonMap, this)
            }
        }
        ConfigBuildResult(
            kxs.encodeToString(optionsMap.toJsonElementKxs()),
            ConfigMetadata(
                mainTag = mainTag,
                trafficProfiles = trafficProfiles.values.toList(),
                tagToID = tagToID,
                trafficGraph = trafficGraph,
            ),
        )
    }

}

private suspend fun exportRuleSetSource(): RuleSetSource.Remote? {
    // "https://raw.githubusercontent.com/SagerNet/sing-geosite/rule-set/geosite-cn.srs"
    val pathPrefix = "https://raw.githubusercontent.com"
    val provider = DataStore.rulesProvider.get()

    val normalBranch = "rule-set"
    val geoipBranch = normalBranch
    val geositeBranch = if (RuleProvider.hasUnstableBranch(provider)) {
        "rule-set-unstable"
    } else {
        normalBranch
    }

    val repositoryOwner = when (provider) {
        RuleProvider.OFFICIAL -> "SagerNet"
        RuleProvider.LOYALSOLDIER -> "xchacha20-poly1305"
        RuleProvider.CHOCOLATE4U -> "Chocolate4U"
        else -> return null // Can't generate.
    }

    return RuleSetSource.Remote(
        geoipPrefix = "$pathPrefix/$repositoryOwner/sing-geoip/$geoipBranch",
        geositePrefix = "$pathPrefix/$repositoryOwner/sing-geosite/$geositeBranch",
        assets = ThroneDatabase.assetDao.getAll().first(),
    )
}

/**
 * Partition outbounds and endpoints.
 */
fun MyOptions.partitionEndpoints() {
    val pair = outbounds!!.partition { isEndpoint(it["type"].toString()) }
    endpoints = pair.first.toMutableList()
    outbounds = pair.second.toMutableList()
}

fun mapNetworkInterfaceStrategy(strategy: Int): String = when (strategy) {
    NetworkInterfaceStrategy.DEFAULT -> SingBoxOptions.STRATEGY_DEFAULT
    NetworkInterfaceStrategy.HYBRID -> SingBoxOptions.STRATEGY_HYBRID
    NetworkInterfaceStrategy.FALLBACK -> SingBoxOptions.STRATEGY_FALLBACK
    else -> throw IllegalStateException()
}

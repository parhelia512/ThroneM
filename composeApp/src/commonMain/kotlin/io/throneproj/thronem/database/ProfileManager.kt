package io.throneproj.thronem.database

import androidx.sqlite.SQLiteException
import io.throneproj.thronem.fmt.AbstractBean
import io.throneproj.thronem.fmt.RuleItem
import io.throneproj.thronem.fmt.SingBoxOptions.ACTION_HIJACK_DNS
import io.throneproj.thronem.fmt.SingBoxOptions.ACTION_REJECT
import io.throneproj.thronem.fmt.SingBoxOptions.ACTION_ROUTE
import io.throneproj.thronem.fmt.SingBoxOptions.ACTION_SNIFF
import io.throneproj.thronem.fmt.SingBoxOptions.NetworkICMP
import io.throneproj.thronem.fmt.SingBoxOptions.NetworkUDP
import io.throneproj.thronem.ktx.applyDefaultValues
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.bypass_icmp
import io.throneproj.thronem.resources.hijack_dns
import io.throneproj.thronem.resources.route_bypass_domain
import io.throneproj.thronem.resources.route_bypass_ip
import io.throneproj.thronem.resources.route_opt_block_ads
import io.throneproj.thronem.resources.route_opt_block_quic
import io.throneproj.thronem.resources.route_opt_bypass_lan
import io.throneproj.thronem.resources.route_play_store
import io.throneproj.thronem.resources.sniff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.*

object ProfileManager {

    private val defaultGroupMutex = Mutex()
    private val repository get() = resolveRepository()

    suspend fun createProfile(groupId: Long, bean: AbstractBean): ProxyEntity {
        bean.applyDefaultValues()

        val profile = ProxyEntity(groupId = groupId).apply {
            id = 0
            putBean(bean)
            userOrder = ThroneDatabase.proxyDao.nextOrder(groupId) ?: 1
        }
        profile.id = ThroneDatabase.proxyDao.addProxy(profile)
        return profile
    }

    suspend fun updateProfile(profile: ProxyEntity) {
        ThroneDatabase.proxyDao.updateProxy(profile)
    }

    suspend fun updateProfile(profiles: List<ProxyEntity>) {
        ThroneDatabase.proxyDao.updateProxy(profiles)
    }

    suspend fun updateTraffic(profile: ProxyEntity, tx: Long?, rx: Long?) {
        ThroneDatabase.proxyDao.updateTraffic(profile.id, tx, rx)
    }

    suspend fun deleteProfile(groupId: Long, profileId: Long) {
        if (ThroneDatabase.proxyDao.deleteById(profileId) == 0) return
        if (ThroneDatabase.proxyDao.countByGroup(groupId).first() > 1) {
            GroupManager.rearrange(groupId)
        }
    }

    suspend fun deleteProfiles(groupId: Long, profileIDs: List<Long>) {
        if (profileIDs.isEmpty()) return
        ThroneDatabase.proxyDao.deleteProxies(profileIDs)
        if (ThroneDatabase.proxyDao.countByGroup(groupId).first() > 1) {
            GroupManager.rearrange(groupId)
        }
    }

    suspend fun getProfile(profileId: Long): ProxyEntity? {
        if (profileId == 0L) return null
        return try {
            ThroneDatabase.proxyDao.getById(profileId)
        } catch (e: SQLiteException) {
            throw IOException(e)
        }
    }

    suspend fun getProfiles(profileIds: List<Long>): List<ProxyEntity> {
        if (profileIds.isEmpty()) return listOf()
        return try {
            ThroneDatabase.proxyDao.getEntities(profileIds)
        } catch (e: SQLiteException) {
            throw IOException(e)
        }
    }

    suspend fun createRule(rule: RuleEntity, post: Boolean = true): RuleEntity {
        rule.userOrder = ThroneDatabase.rulesDao.nextOrder() ?: 1
        rule.id = ThroneDatabase.rulesDao.createRule(rule)
        return rule
    }

    suspend fun updateRule(rule: RuleEntity) {
        ThroneDatabase.rulesDao.updateRule(rule)
    }

    suspend fun deleteRule(ruleId: Long) {
        ThroneDatabase.rulesDao.deleteById(ruleId)
    }

    suspend fun deleteRules(rules: List<RuleEntity>) {
        ThroneDatabase.rulesDao.deleteRules(rules)
    }

    suspend fun deleteRulesByIds(ruleIds: List<Long>) {
        ThroneDatabase.rulesDao.deleteByIds(ruleIds)
    }

    /**
     * Get all rules as a Flow with automatic initialization.
     *
     * This is a wrapper around [ThroneDatabase.rulesDao.allRules] that ensures default rules
     * are created on first app launch. When the Flow is first collected, it checks if the
     * rule list is empty and creates the following rules.
     *
     * Always use this method instead of calling the DAO directly to ensure proper initialization.
     */
    fun getRules(): Flow<List<RuleEntity>> {
        return ThroneDatabase.rulesDao.allRules().onStart {
            val currentRules = ThroneDatabase.rulesDao.allRules().first()
            if (currentRules.isEmpty() && !DataStore.rulesFirstCreate.get()) {
                DataStore.rulesFirstCreate.set(true)
                createRule(
                    RuleEntity(
                        enabled = true,
                        name = repository.getString(Res.string.sniff),
                        action = ACTION_SNIFF,
                    ),
                )
                createRule(
                    RuleEntity(
                        enabled = true,
                        name = repository.getString(Res.string.hijack_dns),
                        protocol = setOf("dns"),
                        action = ACTION_HIJACK_DNS,
                    ),
                )
                createRule(
                    RuleEntity(
                        enabled = true,
                        action = ACTION_ROUTE,
                        name = repository.getString(Res.string.bypass_icmp),
                        network = setOf(NetworkICMP),
                        outbound = RuleEntity.OUTBOUND_DIRECT,
                    ),
                )
                createRule(
                    RuleEntity(
                        name = repository.getString(Res.string.route_opt_block_quic),
                        action = ACTION_REJECT,
                        protocol = setOf("quic"),
                        network = setOf(NetworkUDP),
                    ),
                )
                createRule(
                    RuleEntity(
                        name = repository.getString(Res.string.route_opt_block_ads),
                        action = ACTION_REJECT,
                        domains = "set+dns:geosite-category-ads-all",
                    ),
                )
                val walledCountry = mutableListOf("cn:中国")
                if (Locale.getDefault().country == Locale.US.country) {
                    // English users
                    walledCountry += "ir:Iran"
                }
                for (c in walledCountry) {
                    val country = c.substringBefore(":")
                    val displayCountry = c.substringAfter(":")
                    if (country == "cn") createRule(
                        RuleEntity(
                            name = repository.getString(Res.string.route_play_store, displayCountry),
                            action = ACTION_ROUTE,
                            domains = "set+dns:geosite-google-play",
                            outbound = RuleEntity.OUTBOUND_PROXY,
                        ),
                        false,
                    )
                    createRule(
                        RuleEntity(
                            name = repository.getString(Res.string.route_bypass_domain, displayCountry),
                            action = ACTION_ROUTE,
                            domains = "set+dns:geosite-$country",
                            outbound = RuleEntity.OUTBOUND_DIRECT,
                        ),
                        false,
                    )
                    createRule(
                        RuleEntity(
                            name = repository.getString(Res.string.route_bypass_ip, displayCountry),
                            action = ACTION_ROUTE,
                            ip = "set-dns:geoip-$country",
                            outbound = RuleEntity.OUTBOUND_DIRECT,
                        ),
                        false,
                    )
                }
                createRule(
                    RuleEntity(
                        name = repository.getString(Res.string.route_opt_bypass_lan),
                        action = ACTION_ROUTE,
                        ip = RuleItem.CONTENT_PRIVATE,
                        outbound = RuleEntity.OUTBOUND_DIRECT,
                    ),
                    false,
                )
            }
        }
    }

    fun enabledRules(): Flow<List<RuleEntity>> {
        return getRules().map {
            it.filter { it.enabled }
        }
    }

    /**
     * Get all groups as a Flow with automatic initialization.
     *
     * This is a wrapper around [ThroneDatabase.groupDao.allGroups] that ensures at least one
     * group exists. When the Flow is first collected, it checks if the group list is empty
     * and creates a default ungrouped group if needed.
     *
     * Always use this method instead of calling the DAO directly to ensure proper initialization.
     */
    fun getGroups(): Flow<List<ProxyGroup>> {
        return ThroneDatabase.groupDao.allGroups().onStart {
            ensureDefaultGroupId()
        }
    }

    suspend fun ensureDefaultGroupId(): Long = defaultGroupMutex.withLock {
        ThroneDatabase.groupDao.firstGroupId()
            ?: ThroneDatabase.groupDao.ungroupedId()
            ?: ThroneDatabase.groupDao.createGroup(ProxyGroup(ungrouped = true))
    }

}

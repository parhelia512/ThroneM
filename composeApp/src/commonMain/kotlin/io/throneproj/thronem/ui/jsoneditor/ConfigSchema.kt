package io.throneproj.thronem.ui.jsoneditor

import io.nekohasekai.libbox.Libbox
import io.throneproj.thronem.core.CoreClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.koin.core.context.GlobalContext
import kotlin.time.Duration.Companion.seconds

@Serializable
enum class ConfigSchema {
    CONFIG,
    OUTBOUND,
    DNS_RULE;

    val completer: ConfigSchemaCompleter
        get() = when (this) {
            CONFIG -> configSchemaCompleter
            OUTBOUND -> outboundSchemaCompleter
            DNS_RULE -> dnsRuleSchemaCompleter
        }
}

private val configSchemaCompleter by lazy {
    schemaCompleter(loadSchema(ConfigSchema.CONFIG))
}

private val outboundSchemaCompleter by lazy {
    schemaCompleter(loadSchema(ConfigSchema.OUTBOUND))
}

private val dnsRuleSchemaCompleter by lazy {
    schemaCompleter(loadSchema(ConfigSchema.DNS_RULE))
}

/**
 * Prefer [CoreClient.generateSchema] when a host is reachable. Fall back to
 * the bound libbox generator. libbox produces a single schema for all kinds;
 * that one schema serves every completer.
 *
 * Both generators can fail (the lx libbox schema builder chokes on custom
 * AWG types, e.g. `option.AWGRange` at peers.persistent_keepalive_interval),
 * so the failure degrades to an empty schema — no completions, no crash.
 */
private fun loadSchema(schema: ConfigSchema): String {
    val client = GlobalContext.getOrNull()?.get<CoreClient>()
    if (client != null) {
        try {
            return runBlocking {
                withTimeout(10.seconds) { client.generateSchema() }
            }
        } catch (_: Exception) {
            // Host down / timeout — use the bound generator.
        }
    }
    return try {
        Libbox.generateConfigSchema().value
    } catch (_: Exception) {
        "{}"
    }
}

private fun schemaCompleter(content: String) =
    ConfigSchemaCompleter(Json.parseToJsonElement(content).jsonObject, configJsonEngine)

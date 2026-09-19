package io.throneproj.thronem.database

/**
 * The object wrapper is must.
 * Otherwise, "[MissingType]: Element 'io.throneproj.thronem.database.ThroneDatabase' references a type that is not present"
 */
internal expect object ThroneDatabaseProvider {
    fun create(): ThroneDatabase
}

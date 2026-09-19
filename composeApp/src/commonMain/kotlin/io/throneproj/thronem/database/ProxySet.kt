package io.throneproj.thronem.database

import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import io.throneproj.thronem.fmt.internal.ProxySetBean
import kotlinx.coroutines.flow.Flow

/**
 * A proxy set is a standalone aggregation of proxies (selector / urltest / balancer).
 * The managed options live in [ProxySetBean], stored as a BLOB via [io.throneproj.thronem.fmt.BeanConverters].
 */
@Entity(tableName = "proxy_sets")
data class ProxySet(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    var userOrder: Long = 0L,
    var bean: ProxySetBean? = null,
) {
    fun displayName(): String = bean?.displayName() ?: ""
    fun displayType(): String = bean?.displayType() ?: ""

    @androidx.room.Dao
    interface Dao {

        @Query("SELECT * FROM proxy_sets ORDER BY userOrder, id")
        fun allSets(): Flow<List<ProxySet>>

        @Query("SELECT * FROM proxy_sets WHERE id = :setId")
        fun getById(setId: Long): Flow<ProxySet?>

        @Query("SELECT MAX(userOrder) + 1 FROM proxy_sets")
        suspend fun nextOrder(): Long?

        @Insert
        suspend fun createSet(set: ProxySet): Long

        @Update
        suspend fun updateSet(set: ProxySet)

        @Query("DELETE FROM proxy_sets WHERE id = :setId")
        suspend fun deleteById(setId: Long): Int

        @Query("DELETE FROM proxy_sets")
        suspend fun reset()
    }
}

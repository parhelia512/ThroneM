package io.throneproj.thronem.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import io.throneproj.thronem.fmt.BeanConverters

@Database(
    entities = [
        ProxyGroup::class,
        ProxyEntity::class,
        ProxySet::class,
        RuleEntity::class,
        AssetEntity::class,
    ],
    // TODO remove group front and landing proxy
    version = 1,
)
@TypeConverters(value = [BeanConverters::class])
@ConstructedBy(ThroneDatabaseConstructor::class)
abstract class ThroneDatabase : RoomDatabase() {

    companion object {

        val instance by lazy { ThroneDatabaseProvider.create() }

        val groupDao get() = instance.groupDao()
        val proxyDao get() = instance.proxyDao()
        val proxySetDao get() = instance.proxySetDao()
        val rulesDao get() = instance.rulesDao()
        val assetDao get() = instance.assetDao()

    }

    abstract fun groupDao(): ProxyGroup.Dao
    abstract fun proxyDao(): ProxyEntity.Dao
    abstract fun proxySetDao(): ProxySet.Dao
    abstract fun rulesDao(): RuleEntity.Dao
    abstract fun assetDao(): AssetEntity.Dao

}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ThroneDatabaseConstructor : RoomDatabaseConstructor<ThroneDatabase>

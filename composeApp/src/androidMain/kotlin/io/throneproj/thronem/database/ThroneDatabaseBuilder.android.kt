package io.throneproj.thronem.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.throneproj.thronem.Key
import io.throneproj.thronem.repository.resolveAndroidRepository
import kotlinx.coroutines.Dispatchers

internal actual object ThroneDatabaseProvider {
    actual fun create(): ThroneDatabase {
        val dbFile = resolveAndroidRepository().resolveDatabaseFile(Key.DB_PROFILE)
        dbFile.parentFile?.mkdirs()
        return Room.databaseBuilder<ThroneDatabase>(
            context = resolveAndroidRepository().context,
            name = dbFile.absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .allowMainThreadQueries()
            .enableMultiInstanceInvalidation()
            .fallbackToDestructiveMigration(true)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
    }
}

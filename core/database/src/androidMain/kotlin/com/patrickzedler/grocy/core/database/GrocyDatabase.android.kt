package com.patrickzedler.grocy.core.database

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/** The database file of the app; excluded from backups, the server has all data. */
fun createGrocyDatabase(context: Context): GrocyDatabase =
    Room.databaseBuilder<GrocyDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath(DATABASE_NAME).absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

/**
 * A database that lives only in memory, for tests; [queryContext] lets queries run on a test
 * dispatcher, so they finish in virtual time.
 */
fun createInMemoryGrocyDatabase(queryContext: CoroutineContext? = null): GrocyDatabase =
    Room.inMemoryDatabaseBuilder<GrocyDatabase>()
        .setDriver(BundledSQLiteDriver())
        .apply { queryContext?.let { setQueryCoroutineContext(it) } }
        .build()

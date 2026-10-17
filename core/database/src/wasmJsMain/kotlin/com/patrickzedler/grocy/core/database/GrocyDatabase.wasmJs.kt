package com.patrickzedler.grocy.core.database

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

/**
 * The database in the browser's origin private file system, run by the worker of
 * `core/database/web-worker` (an in-memory one in further tabs).
 */
fun createGrocyDatabase(): GrocyDatabase =
    Room.databaseBuilder<GrocyDatabase>(name = "/$DATABASE_NAME")
        .setDriver(WebWorkerSQLiteDriver(createDatabaseWorker()))
        .build()

// webpack bundles the worker and its sqlite3.wasm from the npm package
@OptIn(ExperimentalWasmJsInterop::class)
private fun createDatabaseWorker(): Worker =
    js(
        """new Worker(
            new URL("grocy-sqlite-worker/worker.js", import.meta.url),
            { type: "module" },
        )""",
    )

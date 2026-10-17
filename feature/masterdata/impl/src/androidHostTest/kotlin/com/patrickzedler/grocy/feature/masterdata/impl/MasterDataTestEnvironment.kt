package com.patrickzedler.grocy.feature.masterdata.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import kotlinx.coroutines.test.TestDispatcher

/** Fake server and a real in-memory database, both running on [dispatcher]. */
internal class MasterDataTestEnvironment(dispatcher: TestDispatcher) {
    val server = FakeGrocyServer(requestDispatcher = dispatcher)
    val database = createInMemoryGrocyDatabase(queryContext = dispatcher)
    val repository = MasterDataRepository(
        api = server.api,
        database = database,
        connection = { ServerConnection.ApiKey("https://grocy.test", "key") },
    )

    fun close() = database.close()
}

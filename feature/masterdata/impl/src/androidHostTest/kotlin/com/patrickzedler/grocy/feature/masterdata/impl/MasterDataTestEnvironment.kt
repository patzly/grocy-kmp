package com.patrickzedler.grocy.feature.masterdata.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestDispatcher

/** Fake server and a real in-memory database, both running on [dispatcher]. */
internal class MasterDataTestEnvironment(dispatcher: TestDispatcher) {
    val server = FakeGrocyServer(requestDispatcher = dispatcher)
    val database = createInMemoryGrocyDatabase(queryContext = dispatcher)
    private val connection = { ServerConnection.ApiKey("https://grocy.test", "key") }
    val repository = MasterDataRepository(
        api = server.api,
        database = database,
        connection = connection,
    )
    val stockRepository = StockRepository(
        api = server.api,
        database = database,
        connection = connection,
    )

    // Check offs are sent on the test dispatcher too, so advanceUntilIdle finishes them
    val shoppingListRepository = ShoppingListRepository(
        api = server.api,
        database = database,
        scope = CoroutineScope(dispatcher),
        connection = connection,
    )

    fun close() = database.close()
}

package com.patrickzedler.grocy.core.testing

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestDispatcher

/**
 * The repositories of the screens on [FakeGrocyServer] and a real in-memory database, both
 * running on [dispatcher]. Android only: Room needs a real SQLite, which host tests load.
 */
class RepositoryTestEnvironment(dispatcher: TestDispatcher) {
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

    val recipesRepository = RecipesRepository(
        api = server.api,
        database = database,
        connection = connection,
    )

    // Check offs are sent on the test dispatcher too, so advanceUntilIdle finishes them; retries
    // would keep it from ever being idle while offline
    val shoppingListRepository = ShoppingListRepository(
        api = server.api,
        database = database,
        scope = CoroutineScope(dispatcher),
        connection = connection,
        firstRetryDelay = null,
    )

    fun close() = database.close()
}

package com.patrickzedler.grocy.core.productcard

import app.cash.turbine.test
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class LabelPrintViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    @Test
    fun printedLabelIsReported() = runTest(dispatcher) {
        environment.server.respond("/api/stock/products/1/printlabel", body = "{}")
        val viewModel = LabelPrintViewModel(environment.stockRepository)

        viewModel.results.test {
            viewModel.printProduct(1)
            advanceUntilIdle()

            assertEquals(LabelPrintResult.Printed, awaitItem())
        }
    }

    @Test
    fun failedPrintIsReportedWithGrocysMessage() = runTest(dispatcher) {
        environment.server.respond(
            "/api/stock/entry/2/printlabel",
            status = HttpStatusCode.BadRequest,
            body = """{"error_message":"Label printer webhook failed"}""",
        )
        val viewModel = LabelPrintViewModel(environment.stockRepository)

        viewModel.results.test {
            viewModel.printStockEntry(2)
            advanceUntilIdle()

            assertEquals(
                LabelPrintResult.Failed(ChangeError.Failed("Label printer webhook failed")),
                awaitItem(),
            )
        }
    }
}

package com.patrickzedler.grocy.core.productcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What printing a label on Grocy's label printer resulted in. */
internal sealed interface LabelPrintResult {
    data object Printed : LabelPrintResult

    data class Failed(val error: ChangeError) : LabelPrintResult
}

/**
 * Prints grocycodes and stock entry labels on the server's label printer, like Grocy's "Print
 * on label printer"; results arrive as [results].
 */
class LabelPrintViewModel(private val repository: StockRepository) : ViewModel() {

    private val resultChannel = Channel<LabelPrintResult>(Channel.BUFFERED)
    internal val results: Flow<LabelPrintResult> = resultChannel.receiveAsFlow()

    fun printProduct(productId: Int) = print { repository.printProductLabel(productId) }

    fun printStockEntry(entryId: Int) = print { repository.printStockEntryLabel(entryId) }

    private fun print(block: suspend () -> Unit) {
        viewModelScope.launch {
            val error = changeErrorOf(block)
            resultChannel.send(
                if (error == null) LabelPrintResult.Printed else LabelPrintResult.Failed(error),
            )
        }
    }
}

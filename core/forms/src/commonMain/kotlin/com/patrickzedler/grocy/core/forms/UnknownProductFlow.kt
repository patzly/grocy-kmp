package com.patrickzedler.grocy.core.forms

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import kotlinx.coroutines.flow.first

/** What a form asks the product form for on the way through [UnknownProductFlow]. */
data class ProductFormRequest(
    val productId: Int? = null,
    val name: String? = null,
    val barcode: String? = null,
)

/** The answer of [UnknownProductFlow.lookUpExternally]. */
sealed interface ExternalLookupResult {
    /** The server created product [productId], which the form then shows for checking. */
    data class Found(val productId: Int) : ExternalLookupResult

    data object NotFound : ExternalLookupResult

    data class Failed(val error: ChangeError) : ExternalLookupResult
}

/**
 * Grocy's product picker workflows for text or a code that names no product: the question
 * "Create or assign product" and what its answers leave for the form.
 *
 * @param canCreate offers the answers that create a product (`disallowAddProductWorkflows`).
 */
@Stable
class UnknownProductFlow(val canCreate: Boolean = true) {
    /** The text in question while the dialog is open. */
    var input by mutableStateOf<String?>(null)
        private set

    /** The code that becomes a barcode of the product the form saves. */
    var pendingBarcode by mutableStateOf<String?>(null)
        private set

    /** Changes when the picker's text was answered for and has to go. */
    var fieldResetKey by mutableIntStateOf(0)
        private set

    /** Grocy's "This product is not in stock" under the picker, for a product it does not offer. */
    var isNotOffered by mutableStateOf(false)
        private set

    private var awaited: AwaitedProduct? = null

    fun ask(text: String) {
        input = text.trim().ifEmpty { null }
        isNotOffered = false
    }

    /**
     * What Grocy's picker makes of text that chooses none of its products: a suggestion containing
     * it takes it (false); the name or barcode of a product it does not offer, an inactive one,
     * clears the field with Grocy's error; everything else asks the question.
     */
    fun onTextWithoutProduct(
        text: String,
        hasSuggestion: Boolean,
        allProducts: List<Product>,
        barcodes: List<ProductBarcode>,
    ): Boolean {
        if (hasSuggestion) return false
        val trimmed = text.trim()
        val exists = barcodes.any { it.barcode == trimmed } ||
            allProducts.any { it.name.equals(trimmed, ignoreCase = true) }
        if (exists) {
            showNotOffered()
        } else {
            ask(trimmed)
        }
        return true
    }

    /** Grocy's error for a product its picker does not offer, which also clears the field. */
    fun showNotOffered() {
        isNotOffered = true
        input = null
        clearField()
    }

    fun dismiss() {
        input = null
    }

    fun clearField() {
        fieldResetKey++
    }

    /** "Add as barcode to existing product": the next chosen product gets it. */
    fun addAsBarcode() {
        pendingBarcode = input
        awaited = null
        input = null
        clearField()
    }

    /** "Add as new product": named after the text; the product that appears next is chosen. */
    fun newProductWithName(products: List<Product>): ProductFormRequest {
        val name = input
        awaited = AwaitedProduct.New(products.mapTo(HashSet()) { it.id })
        input = null
        return ProductFormRequest(name = name)
    }

    /** "Add as new product and prefill barcode": chosen once that barcode exists. */
    fun newProductWithBarcode(): ProductFormRequest {
        val code = input
        pendingBarcode = code
        awaited = code?.let(AwaitedProduct::WithBarcode)
        input = null
        return ProductFormRequest(barcode = code)
    }

    /** The scanner's turn again, the camera button of Grocy's dialog. */
    fun scanAgain() {
        input = null
        clearField()
    }

    /** "External barcode lookup": the server's plugin creates the product it finds. */
    suspend fun lookUpExternally(repository: MasterDataRepository): ExternalLookupResult {
        val code = input ?: return ExternalLookupResult.NotFound
        input = null
        // Set before the download that brings the product along
        awaited = AwaitedProduct.WithBarcode(code)
        var productId: Int? = null
        val error = changeErrorOf { productId = repository.externalBarcodeLookup(code) }
        val found = productId
        if (found == null) awaited = null
        return when {
            error != null -> ExternalLookupResult.Failed(error)
            found != null -> ExternalLookupResult.Found(found)
            else -> ExternalLookupResult.NotFound
        }
    }

    /** The product one of the answers waited for, once; null until it is there. */
    fun takeCreatedProduct(products: List<Product>, barcodes: List<ProductBarcode>): Product? {
        val product = when (val waiting = awaited) {
            null -> null
            is AwaitedProduct.New -> products.filter { it.id !in waiting.knownIds }
                .maxByOrNull { it.id }
            is AwaitedProduct.WithBarcode -> barcodes.firstOrNull { it.barcode == waiting.code }
                ?.let { barcode -> products.firstOrNull { it.id == barcode.productId } }
        } ?: return null
        awaited = null
        return product
    }

    /** The user chose for themselves, so a product arriving later must not replace it. */
    fun stopWaiting() {
        awaited = null
        isNotOffered = false
    }

    /** Adds the pending barcode to [productId]; the product form may have done that already. */
    suspend fun savePendingBarcode(productId: Int, repository: MasterDataRepository) {
        val code = pendingBarcode ?: return
        val exists = repository.productBarcodes.first()
            .any { it.barcode == code && it.productId == productId }
        if (!exists) {
            repository.saveProductBarcode(ProductBarcode(0, productId, code), emptyMap())
        }
        pendingBarcode = null
    }

    /** The barcode was saved by the form itself, with more values (a booking). */
    fun onBarcodeSaved() {
        pendingBarcode = null
    }

    private sealed interface AwaitedProduct {
        class New(val knownIds: Set<Int>) : AwaitedProduct

        class WithBarcode(val code: String) : AwaitedProduct
    }
}

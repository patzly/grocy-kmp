package com.patrickzedler.grocy.feature.recipes.impl.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.patrickzedler.grocy.core.forms.ObjectFormLoad
import com.patrickzedler.grocy.core.model.recipes.ImportTexts
import com.patrickzedler.grocy.core.model.recipes.importedPreparation
import com.patrickzedler.grocy.core.network.RecipeImportApi
import com.patrickzedler.grocy.core.network.RecipeImportException
import io.ktor.http.Url
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.settings.ScannerFormsRepository
import com.patrickzedler.grocy.core.forms.AmountUnit
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.FormFocusTarget
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.forms.ScannerFieldState
import com.patrickzedler.grocy.core.forms.UnknownProductFlow
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.forms.fieldState
import com.patrickzedler.grocy.core.forms.ignoringServerErrors
import com.patrickzedler.grocy.core.forms.productAmountUnits
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.scanner.ScannerForm
import com.patrickzedler.grocy.core.model.recipes.RecipeIngredient
import com.patrickzedler.grocy.core.model.recipes.RecipeNesting
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.productcard.productIdForCode
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** An ingredient of the form's "Ingredients list", like a row of Grocy's table. */
internal data class IngredientItem(
    val id: Int,
    /** "2 Pieces Milk", or the variable amount instead of amount and unit. */
    val text: String,
    val group: String?,
    val note: String?,
    val isVariableAmount: Boolean,
)

/** An included recipe of the form, like a row of Grocy's table. */
internal data class NestingItem(val id: Int, val name: String, val servings: String)

/** The ingredients and included recipes of the edited recipe. */
internal data class RecipeRelated(
    val ingredients: List<IngredientItem> = emptyList(),
    val nestings: List<NestingItem> = emptyList(),
)

/** Grocy's ingredient form of a dialog; [id] is 0 for a new ingredient. */
@Stable
internal class IngredientDialogState(
    val id: Int,
    val original: RecipeIngredient?,
    initial: IngredientFormValues,
    val recipeName: String,
    products: List<Product>,
    private var allProducts: List<Product>,
    barcodes: List<ProductBarcode>,
    private val allUnits: List<QuantityUnit>,
    conversions: List<ResolvedQuantityUnitConversion>,
    val decimals: Int,
    val showsStockOptions: Boolean,
    val showsPriceFactor: Boolean,
    /** The product's scanner, as it was left in the last new ingredient. */
    val scanner: ScannerFieldState =
        ScannerFieldState(hasValue = id != MasterDataRepository.NEW_OBJECT_ID),
    /** The recipe site's line this ingredient is made from, shown above the fields. */
    val importedText: String? = null,
) {
    var products by mutableStateOf(products)
        private set
    private var barcodes by mutableStateOf(barcodes)
    private var conversions by mutableStateOf(conversions)

    /** Grocy's "Create or assign product" for what names no product. */
    val unknownProduct = UnknownProductFlow()

    var productId by mutableStateOf(initial.productId)
    var onlyCheckSingleUnitInStock by mutableStateOf(initial.onlyCheckSingleUnitInStock)
    val amount = TextFieldState(initial.amount)
    var quId by mutableStateOf(initial.quId)
    val variableAmount = TextFieldState(initial.variableAmount)
    var roundUp by mutableStateOf(initial.roundUp)
    var notCheckStockFulfillment by mutableStateOf(initial.notCheckStockFulfillment)
    val group = TextFieldState(initial.group)
    val note = TextFieldState(initial.note)
    val priceFactor = TextFieldState(initial.priceFactor)

    var isProductUnmatched by mutableStateOf(false)
    var isUnitUnmatched by mutableStateOf(false)
    var errors by mutableStateOf<Map<IngredientField, FieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    val isNew: Boolean
        get() = id == MasterDataRepository.NEW_OBJECT_ID

    /**
     * The units of the amount: those the product's stock unit converts into, every unit with
     * "Only check if any amount is in stock" like Grocy's picker then.
     */
    val units: List<AmountUnit>
        get() {
            val product = products.firstOrNull { it.id == productId } ?: return emptyList()
            return if (onlyCheckSingleUnitInStock) {
                allUnits.filter { it.active }.map { AmountUnit(it, 1.0) }
                    .sortedBy { it.unit.name.lowercase() }
            } else {
                productAmountUnits(product.id, product.quIdStock, conversions, allUnits)
            }
        }

    private fun productForCode(code: String): Product? = productIdForCode(code, barcodes)
        ?.let { id -> products.firstOrNull { it.id == id } }

    /** A scanned code: its product is chosen and the amount is next, else like Grocy's picker. */
    fun onScanned(code: String) {
        val product = productForCode(code)
        if (product == null) {
            unknownProduct.onTextWithoutProduct(code, false, allProducts, barcodes)
            // A code the form does not offer keeps scanning, a question needs its answer
            if (unknownProduct.input != null) scanner.closeWithoutFocus()
            return
        }
        selectProduct(product.id)
        scanner.onScanned()
        scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
    }

    /**
     * Text submitted or left standing in the product field, which a barcode reader types like a
     * keyboard: a code chooses its product, other text goes the way of Grocy's picker. The scanner
     * opening only takes the focus away, which asks nothing.
     */
    fun onTextSubmitted(text: String, hasSuggestion: Boolean): Boolean {
        if (scanner.isOpen) return false
        val product = productForCode(text)
        if (product != null) {
            selectProduct(product.id)
            scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
            return true
        }
        val used = unknownProduct.onTextWithoutProduct(text, hasSuggestion, allProducts, barcodes)
        // Grocy's error leaves the focus in the field for the next try
        if (unknownProduct.isNotOffered) scanner.requestFocus(FormFocusTarget.CODE_FIELD)
        return used
    }

    fun dismissUnknownProduct() {
        unknownProduct.dismiss()
        scanner.onFormReset()
    }

    /** Grocy's "Add as barcode to existing product": the user chooses the product now. */
    fun onBarcodeAssigned() {
        selectProduct(null)
        scanner.requestFocus(FormFocusTarget.CODE_FIELD)
    }

    /** Grocy's camera button in the question: on with the next code. */
    fun scanAgain() {
        selectProduct(null)
        if (!scanner.isOpen) scanner.toggle()
    }

    /** A product created meanwhile becomes a choice; the one the question waited for is chosen. */
    fun updateChoices(
        products: List<Product>,
        barcodes: List<ProductBarcode>,
        conversions: List<ResolvedQuantityUnitConversion>,
    ) {
        allProducts = products
        this.products = products.filter { it.active || it.id == original?.productId }
        this.barcodes = barcodes
        this.conversions = conversions
        unknownProduct.takeCreatedProduct(this.products, barcodes)?.let { created ->
            selectProduct(created.id)
            scanner.requestFocus(FormFocusTarget.NEXT_FIELD)
        }
    }

    /** Grocy's choices for a newly chosen product: its stock unit, its fulfillment check. */
    fun selectProduct(id: Int?) {
        if (id != null) {
            unknownProduct.stopWaiting()
            scanner.onValueChosen()
        }
        productId = id
        val product = products.firstOrNull { it.id == id } ?: return
        if (!onlyCheckSingleUnitInStock) quId = product.quIdStock
        if (isNew) notCheckStockFulfillment = product.notCheckStockFulfillmentForRecipes
    }

    /** Back to the product's stock unit when any unit is no longer allowed. */
    fun setOnlyCheckSingleUnit(checked: Boolean) {
        onlyCheckSingleUnitInStock = checked
        if (!checked && units.none { it.unit.id == quId }) {
            quId = products.firstOrNull { it.id == productId }?.quIdStock
        }
    }

    fun values() = IngredientFormValues(
        productId = productId,
        onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock,
        amount = amount.text.toString(),
        quId = quId,
        variableAmount = variableAmount.text.toString(),
        roundUp = roundUp,
        notCheckStockFulfillment = notCheckStockFulfillment,
        group = group.text.toString(),
        note = note.text.toString(),
        priceFactor = priceFactor.text.toString(),
    )
}

/** Grocy's included recipe form of a dialog; [id] is 0 for a new inclusion. */
@Stable
internal class NestingDialogState(
    val id: Int,
    initial: NestingFormValues,
    /** The recipes it can include, the edited one left out. */
    val recipes: List<Recipe>,
    val decimals: Int,
) {
    var recipeId by mutableStateOf(initial.recipeId)
    val servings = TextFieldState(initial.servings)
    var isRecipeUnmatched by mutableStateOf(false)
    var errors by mutableStateOf<Map<ObjectField, FieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    val isNew: Boolean
        get() = id == MasterDataRepository.NEW_OBJECT_ID

    fun values() = NestingFormValues(recipeId, servings.text.toString())
}

/** Grocy's questions before an ingredient or an included recipe is removed. */
internal sealed interface RelatedDelete {
    data class Ingredient(val id: Int, val name: String) : RelatedDelete

    data class Nesting(val id: Int, val name: String) : RelatedDelete
}

/**
 * Grocy's recipe form: name, servings, shopping list check, produced product, preparation,
 * userfields and picture are saved together; ingredients and included recipes, Grocy's tables
 * of the saved recipe, through their own dialogs right away. A created recipe becomes its edit
 * form, so they can be added, like Grocy's "Save & continue".
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class RecipeFormViewModel(
    id: Int?,
    repository: MasterDataRepository,
    private val recipes: RecipesRepository,
    private val language: String,
    private val decimalSeparator: Char = platformDecimalSeparator(),
    private val scannerForms: ScannerFormsRepository? = null,
    /** The recipe site's page a new recipe is filled from. */
    private val importUrl: String? = null,
    private val importer: RecipeImportApi? = null,
) : ObjectFormViewModel<RecipeFormValues, RecipeFormState>(id, repository) {

    override val userfieldEntity: String = RecipesRepository.RECIPES

    var config by mutableStateOf(ServerConfig())
        private set

    var amountDecimals by mutableStateOf(DEFAULT_DECIMALS)
        private set

    var ingredientDialog by mutableStateOf<IngredientDialogState?>(null)
        private set

    var nestingDialog by mutableStateOf<NestingDialogState?>(null)
        private set

    var relatedDelete by mutableStateOf<RelatedDelete?>(null)
        private set

    /** Why removing an ingredient or included recipe failed, shown once. */
    var relatedDeleteError by mutableStateOf<ChangeError?>(null)
        private set

    /** Reading the recipe site's page; null without import. */
    var importState by mutableStateOf<RecipeImportState?>(null)
        private set

    /** The site's ingredient lines that are no ingredient yet, in the site's order. */
    var importLines by mutableStateOf<List<ImportLine>>(emptyList())
        private set

    var isAddingImportLines by mutableStateOf(false)
        private set

    /** Why adding the proposed ingredients stopped, shown once. */
    var importAddError by mutableStateOf<ChangeError?>(null)
        private set

    val canImport: Boolean
        get() = importUrl != null && importer != null

    /** The recipe site the form is filled from, "chefkoch.de". */
    val importHost: String = importUrl?.let(::hostOf).orEmpty()

    // The imported line the open ingredient dialog is made from
    private var dialogImportKey: Int? = null

    val related: StateFlow<RecipeRelated> = snapshotFlow { objectId }
        .flatMapLatest { recipeId ->
            combine(
                recipes.ingredients,
                recipes.nestings,
                recipes.recipes,
                combine(repository.products, repository.quantityUnits, ::Pair),
                repository.resolvedQuantityUnitConversions,
            ) { ingredients, nestings, all, (products, units), conversions ->
                relatedOf(recipeId, ingredients, nestings, all, products, units, conversions)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RecipeRelated())

    init {
        start()
        // A product created from the ingredient dialog's question becomes its choice
        viewModelScope.launch {
            combine(
                repository.products,
                repository.productBarcodes,
                repository.resolvedQuantityUnitConversions,
            ) { products, barcodes, conversions ->
                ingredientDialog?.updateChoices(products, barcodes, conversions)
            }.collect()
        }
    }

    override suspend fun createForm(id: Int?): RecipeFormState? {
        config = repository.serverConfig.first()
        amountDecimals = repository.userSettings.first().stockDecimalPlacesAmounts
        val products = repository.products.first().filter { it.active }
        if (id == null) return RecipeFormState(RecipeFormValues(), null, products)
        val recipe = recipes.recipe(id).first() ?: return null
        // A disabled product produced so far stays choosable
        val choices = products + repository.products.first()
            .filter { !it.active && it.id == recipe.productId }
        val values = recipe.toFormValues(amountDecimals, decimalSeparator)
        return RecipeFormState(values, recipe, choices)
    }

    override suspend fun userfieldValues(id: Int): Map<String, String?> =
        recipes.recipe(id).first()?.userfields.orEmpty()

    override suspend fun refreshForUnknownObject() {
        recipes.refresh()
    }

    override fun validate(form: RecipeFormState): Map<ObjectField, FieldError> =
        form.values().validate(amountDecimals)

    override suspend fun save(
        id: Int,
        form: RecipeFormState,
        userfields: Map<String, String?>,
    ): Int {
        val values = form.values()
        val savedId = recipes.saveRecipe(
            recipe = values.toRecipe(id, form.original),
            userfields = userfields,
            picture = values.picture,
        )
        // The form shows the saved recipe from now on, its new picture included
        form.original = recipes.recipe(savedId).first()
        form.picture = PictureChange.Keep
        return savedId
    }

    /** The picture the form shows: the stored one of the saved recipe. */
    suspend fun picture(fileName: String): ByteArray? =
        ignoringServerErrors { recipes.picture(fileName) }

    /** Opens Grocy's ingredient form of ingredient [id], or of a new one without. */
    fun openIngredient(id: Int? = null) {
        val recipeId = objectId.takeUnless { isNew } ?: return
        dialogImportKey = null
        viewModelScope.launch {
            val original = id?.let { ingredientId ->
                recipes.ingredients.first().firstOrNull { it.id == ingredientId }
            }
            if (id != null && original == null) return@launch
            val products = repository.products.first()
            val units = repository.quantityUnits.first()
            val conversions = repository.resolvedQuantityUnitConversions.first()
            val choices = products.filter { it.active || it.id == original?.productId }
            val initial = original?.toFormValues(
                units = productAmountUnits(
                    productId = original.productId,
                    destinationQuId = products.firstOrNull { it.id == original.productId }
                        ?.quIdStock,
                    resolved = conversions,
                    units = units,
                ),
                decimals = amountDecimals,
                decimalSeparator = decimalSeparator,
            ) ?: IngredientFormValues()
            ingredientDialog = IngredientDialogState(
                id = original?.id ?: MasterDataRepository.NEW_OBJECT_ID,
                original = original,
                initial = initial,
                recipeName = recipes.recipe(recipeId).first()?.name.orEmpty(),
                products = choices,
                allProducts = products,
                barcodes = repository.productBarcodes.first(),
                allUnits = units,
                conversions = conversions,
                decimals = amountDecimals,
                showsStockOptions = config.stock,
                showsPriceFactor = config.stockPriceTracking,
                scanner = scannerForms?.fieldState(
                    ScannerForm.RECIPE_INGREDIENT,
                    hasValue = original != null,
                ) ?: ScannerFieldState(hasValue = original != null),
            )
        }
    }

    /** Saves the open ingredient form; it closes once the server saved the ingredient. */
    fun saveIngredient() {
        val form = ingredientDialog ?: return
        if (form.isSaving) return
        val values = form.values()
        form.errors = values.validate(form.decimals)
        if (form.errors.isNotEmpty() || form.isProductUnmatched || form.isUnitUnmatched) return
        val recipeId = objectId
        form.isSaving = true
        form.error = null
        viewModelScope.launch {
            val supportsRoundUp = recipes.ingredients.first().any { it.roundUp != null }
            val ingredient = values.toIngredient(
                id = form.id,
                recipeId = recipeId,
                units = form.units,
                original = form.original,
                supportsRoundUp = supportsRoundUp,
                decimals = form.decimals,
            )
            val error = changeErrorOf {
                form.unknownProduct.savePendingBarcode(ingredient.productId, repository)
                recipes.saveIngredient(ingredient)
            }
            form.isSaving = false
            if (error == null) {
                if (ingredientDialog === form) {
                    ingredientDialog = null
                    // The line is an ingredient now
                    dialogImportKey?.let(::removeImportLine)
                    dialogImportKey = null
                }
            } else {
                form.error = error
            }
        }
    }

    fun dismissIngredient() {
        if (ingredientDialog?.isSaving != true) {
            ingredientDialog = null
            dialogImportKey = null
        }
    }

    /**
     * Reads the recipe site's page and fills the new recipe's form with it: name, servings,
     * picture and as preparation everything else the site tells ([importedPreparation]); its
     * ingredient lines become [importLines]. Fields typed meanwhile are kept.
     */
    fun startImport(texts: ImportTexts) {
        val url = importUrl ?: return
        val importer = importer ?: return
        // Once, a failed one again through its retry
        if (importState != null && importState !is RecipeImportState.Failed) return
        importState = RecipeImportState.Loading
        viewModelScope.launch {
            val recipe = try {
                importer.recipe(url)
            } catch (e: RecipeImportException) {
                importState = RecipeImportState.Failed(e.failure)
                return@launch
            }
            val picture = recipe.imageUrl?.let { imageUrl ->
                importer.picture(imageUrl)?.let { PictureChange.Replace(it, pictureName(imageUrl)) }
            }
            val form = loadedForm()
            if (form != null) {
                if (form.name.text.isBlank()) form.name.setTextAndPlaceCursorAtEnd(recipe.name)
                recipe.servings?.let { form.baseServings.setTextAndPlaceCursorAtEnd(it.toString()) }
                if (form.preparation.text.isBlank()) {
                    form.preparation.setTextAndPlaceCursorAtEnd(importedPreparation(recipe, texts))
                }
                if (picture != null && form.picture == PictureChange.Keep) form.picture = picture
            }
            importLines = importLinesOf(
                lines = recipe.ingredients,
                products = repository.products.first(),
                units = repository.quantityUnits.first(),
                conversions = repository.resolvedQuantityUnitConversions.first(),
                decimals = amountDecimals,
                decimalSeparator = decimalSeparator,
            )
            importState = RecipeImportState.Done
        }
    }

    /** Opens the ingredient form with what was proposed for the imported line [key]. */
    fun openImportLine(key: Int) {
        val recipeId = objectId.takeUnless { isNew } ?: return
        val line = importLines.firstOrNull { it.key == key } ?: return
        viewModelScope.launch {
            val products = repository.products.first()
            ingredientDialog = IngredientDialogState(
                id = MasterDataRepository.NEW_OBJECT_ID,
                original = null,
                initial = line.toFormValues(),
                recipeName = recipes.recipe(recipeId).first()?.name.orEmpty(),
                products = products.filter { it.active },
                allProducts = products,
                barcodes = repository.productBarcodes.first(),
                allUnits = repository.quantityUnits.first(),
                conversions = repository.resolvedQuantityUnitConversions.first(),
                decimals = amountDecimals,
                showsStockOptions = config.stock,
                showsPriceFactor = config.stockPriceTracking,
                // The proposal is chosen, the product field shows it without scanning
                scanner = ScannerFieldState(hasValue = true),
                importedText = line.line.text,
            )
            dialogImportKey = key
        }
    }

    /** Leaves the imported line [key] out; the preparation still has it as the site wrote it. */
    fun removeImportLine(key: Int) {
        importLines = importLines.filterNot { it.key == key }
    }

    /** Adds every imported line that is [ImportLine.isReady] as ingredient at once. */
    fun addReadyImportLines() {
        val recipeId = objectId.takeUnless { isNew } ?: return
        if (isAddingImportLines) return
        val ready = importLines.filter { it.isReady }
        if (ready.isEmpty()) return
        isAddingImportLines = true
        importAddError = null
        viewModelScope.launch {
            val products = repository.products.first()
            val units = repository.quantityUnits.first()
            val conversions = repository.resolvedQuantityUnitConversions.first()
            val supportsRoundUp = recipes.ingredients.first().any { it.roundUp != null }
            val ingredients = ready.map { line ->
                val product = products.first { it.id == line.productId }
                line.toFormValues()
                    .copy(notCheckStockFulfillment = product.notCheckStockFulfillmentForRecipes)
                    .toIngredient(
                        id = MasterDataRepository.NEW_OBJECT_ID,
                        recipeId = recipeId,
                        units = productAmountUnits(
                            productId = product.id,
                            destinationQuId = product.quIdStock,
                            resolved = conversions,
                            units = units,
                        ),
                        original = null,
                        supportsRoundUp = supportsRoundUp,
                        decimals = amountDecimals,
                    )
            }
            importAddError = changeErrorOf {
                recipes.saveIngredients(ingredients) { index -> removeImportLine(ready[index].key) }
            }
            isAddingImportLines = false
        }
    }

    fun onImportAddErrorShown() {
        importAddError = null
    }

    private fun loadedForm(): RecipeFormState? = (load as? ObjectFormLoad.Ready)?.form

    /** Opens Grocy's included recipe form of inclusion [id], or of a new one without. */
    fun openNesting(id: Int? = null) {
        val recipeId = objectId.takeUnless { isNew } ?: return
        viewModelScope.launch {
            val original = id?.let { nestingId ->
                recipes.nestings.first().firstOrNull { it.id == nestingId }
            }
            if (id != null && original == null) return@launch
            nestingDialog = NestingDialogState(
                id = original?.id ?: MasterDataRepository.NEW_OBJECT_ID,
                initial = original?.let {
                    NestingFormValues(
                        recipeId = it.includesRecipeId,
                        servings = formatDecimal(it.servings, amountDecimals, decimalSeparator),
                    )
                } ?: NestingFormValues(),
                recipes = recipes.recipes.first().filter { it.id != recipeId },
                decimals = amountDecimals,
            )
        }
    }

    /** Saves the open included recipe form; it closes once the server saved it. */
    fun saveNesting() {
        val form = nestingDialog ?: return
        if (form.isSaving) return
        val recipeId = objectId
        val values = form.values()
        form.errors = values.validate(form.decimals, recipeId)
        if (form.errors.isNotEmpty() || form.isRecipeUnmatched) return
        form.isSaving = true
        form.error = null
        viewModelScope.launch {
            val error = changeErrorOf { recipes.saveNesting(values.toNesting(form.id, recipeId)) }
            form.isSaving = false
            if (error == null) {
                if (nestingDialog === form) nestingDialog = null
            } else {
                form.error = error
            }
        }
    }

    fun dismissNesting() {
        if (nestingDialog?.isSaving != true) nestingDialog = null
    }

    /** Grocy asks before an ingredient or included recipe is removed. */
    fun requestDelete(delete: RelatedDelete) {
        relatedDelete = delete
    }

    fun dismissDelete() {
        relatedDelete = null
    }

    fun confirmDelete() {
        val delete = relatedDelete ?: return
        relatedDelete = null
        viewModelScope.launch {
            relatedDeleteError = changeErrorOf {
                when (delete) {
                    is RelatedDelete.Ingredient -> recipes.deleteIngredient(delete.id)
                    is RelatedDelete.Nesting -> recipes.deleteNesting(delete.id)
                }
            }
        }
    }

    fun onDeleteErrorShown() {
        relatedDeleteError = null
    }

    private fun relatedOf(
        recipeId: Int,
        ingredients: List<RecipeIngredient>,
        nestings: List<RecipeNesting>,
        all: List<Recipe>,
        products: List<Product>,
        units: List<QuantityUnit>,
        conversions: List<ResolvedQuantityUnitConversion>,
    ): RecipeRelated {
        if (recipeId == MasterDataRepository.NEW_OBJECT_ID) return RecipeRelated()
        val productsById = products.associateBy { it.id }
        val unitsById = units.associateBy { it.id }
        val names = all.associate { it.id to it.name }
        return RecipeRelated(
            ingredients = ingredients.filter { it.recipeId == recipeId }
                // Grocy's table groups by ingredient group, then its own order
                .sortedWith(compareBy(nullsFirst()) { it.ingredientGroup })
                .map { ingredient ->
                    val product = productsById[ingredient.productId]
                    val shown = ingredient.toFormValues(
                        units = productAmountUnits(
                            productId = ingredient.productId,
                            destinationQuId = product?.quIdStock,
                            resolved = conversions,
                            units = units,
                        ),
                        decimals = amountDecimals,
                        decimalSeparator = decimalSeparator,
                    )
                    val unit = ingredient.quId?.let { unitsById[it] }
                    val amount = ingredient.variableAmount ?: listOfNotNull(
                        shown.amount,
                        unit?.nameFor(parseDecimal(shown.amount) ?: 1.0, language),
                    ).joinToString(" ")
                    IngredientItem(
                        id = ingredient.id,
                        text = "$amount ${product?.name.orEmpty()}".trim(),
                        group = ingredient.ingredientGroup,
                        note = ingredient.note,
                        isVariableAmount = ingredient.variableAmount != null,
                    )
                },
            nestings = nestings.filter { it.recipeId == recipeId }.map { nesting ->
                NestingItem(
                    id = nesting.id,
                    name = names[nesting.includesRecipeId].orEmpty(),
                    servings = formatDecimal(nesting.servings, amountDecimals, decimalSeparator),
                )
            },
        )
    }

    private companion object {
        // Grocy's default of the user setting, until it is read
        const val DEFAULT_DECIMALS = 4
    }
}

/** The site of [url] without "www.", the address itself where it has none. */
internal fun hostOf(url: String): String =
    runCatching { Url(url.trim()).host }.getOrNull()?.removePrefix("www.")?.ifEmpty { null } ?: url

/** A file name for the picture at [url]: its own, with an extension Grocy shows. */
internal fun pictureName(url: String): String {
    val name = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
    val known = name.substringAfterLast('.', "").lowercase() in PICTURE_EXTENSIONS
    return when {
        name.isBlank() -> "recipe.jpg"
        known -> name
        else -> "$name.jpg"
    }
}

private val PICTURE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")

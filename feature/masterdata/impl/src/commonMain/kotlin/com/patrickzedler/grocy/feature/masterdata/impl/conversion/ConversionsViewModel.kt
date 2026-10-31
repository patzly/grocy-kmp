package com.patrickzedler.grocy.feature.masterdata.impl.conversion

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.UserfieldError
import com.patrickzedler.grocy.core.forms.UserfieldInput
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.forms.changedValues
import com.patrickzedler.grocy.core.forms.userfieldInputs
import com.patrickzedler.grocy.core.forms.validationErrors
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whose conversions are shown: a product's overrides or a unit's default conversions. */
internal sealed interface ConversionOwner {
    data class Product(val productId: Int) : ConversionOwner

    data class Unit(val quId: Int) : ConversionOwner
}

/** A conversion of the list, as Grocy explains it. */
internal data class ConversionItem(val id: Int, val meaning: ConversionMeaning)

/**
 * The conversions of [owner] like Grocy's tables: a product's own overrides, or the default
 * conversions from a unit; by the units' names.
 */
internal fun buildConversionItems(
    owner: ConversionOwner,
    conversions: List<QuantityUnitConversion>,
    units: List<QuantityUnit>,
    decimals: Int,
    decimalSeparator: Char,
    language: String,
): List<ConversionItem> {
    val unitsById = units.associateBy { it.id }
    return conversions
        .filter {
            when (owner) {
                is ConversionOwner.Product -> it.productId == owner.productId
                is ConversionOwner.Unit -> it.productId == null && it.fromQuId == owner.quId
            }
        }
        .mapNotNull { conversion ->
            val from = unitsById[conversion.fromQuId] ?: return@mapNotNull null
            val to = unitsById[conversion.toQuId] ?: return@mapNotNull null
            ConversionItem(
                id = conversion.id,
                meaning = conversionMeaning(
                    from, to, conversion.factor, decimals, decimalSeparator, language,
                ),
            )
        }
        .sortedWith(compareBy({ it.meaning.from.lowercase() }, { it.meaning.to.lowercase() }))
}

/** Conversions Grocy derived for a product, grouped by the unit they convert from. */
internal data class ResolvedGroup(val from: String, val conversions: List<ConversionMeaning>)

/**
 * Grocy's "QU conversions resolved" of [productId] (all products without), the units by name;
 * a unit converted into itself says nothing and is left out.
 */
internal fun buildResolvedGroups(
    productId: Int?,
    resolved: List<ResolvedQuantityUnitConversion>,
    units: List<QuantityUnit>,
    decimals: Int,
    decimalSeparator: Char,
    language: String,
): List<ResolvedGroup> {
    val unitsById = units.associateBy { it.id }
    return resolved
        .filter { it.productId == productId && it.fromQuId != it.toQuId }
        .mapNotNull { conversion ->
            val from = unitsById[conversion.fromQuId] ?: return@mapNotNull null
            val to = unitsById[conversion.toQuId] ?: return@mapNotNull null
            conversionMeaning(from, to, conversion.factor, decimals, decimalSeparator, language)
        }
        .distinct()
        .groupBy { it.from }
        .map { (from, meanings) -> ResolvedGroup(from, meanings.sortedBy { it.to.lowercase() }) }
        .sortedBy { it.from.lowercase() }
}

/** The open conversion form; [id] is 0 for a new conversion. */
@Stable
internal class ConversionDialogState(
    val id: Int,
    initial: ConversionFormValues,
    /** The product's or default unit's name the subtitle names. */
    val ownerName: String,
    val isProductOverride: Boolean,
    /** A default conversion always starts from its unit. */
    val isFromFixed: Boolean,
    val units: List<QuantityUnit>,
    val decimals: Int,
    val userfields: List<UserfieldInput>,
) {
    var fromQuId by mutableStateOf(initial.fromQuId)
    var toQuId by mutableStateOf(initial.toQuId)
    val factor = TextFieldState(initial.factor)

    var isFromUnmatched by mutableStateOf(false)
    var isToUnmatched by mutableStateOf(false)

    var errors by mutableStateOf<Map<ConversionField, ConversionError>>(emptyMap())
    var userfieldErrors by mutableStateOf<Map<String, UserfieldError>>(emptyMap())
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)

    val isNew: Boolean
        get() = id == MasterDataRepository.NEW_OBJECT_ID

    fun values() = ConversionFormValues(fromQuId, toQuId, factor.text.toString())
}

/**
 * The conversions of a product or unit in its form: Grocy's table ([items]), its conversion form
 * as [dialog] and, for products, the resolved conversions. Saving is online only. The owner is
 * set by [showOwner], a new object gets its id once created.
 */
internal class ConversionsViewModel(
    private val repository: MasterDataRepository,
    private val decimalSeparator: Char = platformDecimalSeparator(),
    private val language: String,
) : ViewModel() {

    private val ownerFlow = MutableStateFlow<ConversionOwner?>(null)

    val items: StateFlow<List<ConversionItem>> = combine(
        ownerFlow,
        repository.quantityUnitConversions,
        repository.quantityUnits,
        repository.userSettings,
    ) { owner, conversions, units, settings ->
        if (owner == null) return@combine emptyList()
        buildConversionItems(
            owner = owner,
            conversions = conversions,
            units = units,
            decimals = settings.stockDecimalPlacesAmounts,
            decimalSeparator = decimalSeparator,
            language = language,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Grocy's "Show resolved conversions" of the product, every product for units. */
    val resolvedGroups: StateFlow<List<ResolvedGroup>> = combine(
        ownerFlow,
        repository.resolvedQuantityUnitConversions,
        repository.quantityUnits,
        repository.userSettings,
    ) { owner, resolved, units, settings ->
        buildResolvedGroups(
            productId = (owner as? ConversionOwner.Product)?.productId,
            resolved = resolved,
            units = units,
            decimals = settings.stockDecimalPlacesAmounts,
            decimalSeparator = decimalSeparator,
            language = language,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var dialog by mutableStateOf<ConversionDialogState?>(null)
        private set

    var showsResolved by mutableStateOf(false)
        private set

    fun showOwner(owner: ConversionOwner) {
        ownerFlow.value = owner
    }

    fun showResolved(show: Boolean) {
        showsResolved = show
    }

    /** Opens the form of conversion [id], or of a new conversion without one. */
    fun open(id: Int? = null) {
        val owner = ownerFlow.value ?: return
        viewModelScope.launch {
            val original = id?.let { conversionId ->
                repository.quantityUnitConversions.first().firstOrNull { it.id == conversionId }
            }
            if (id != null && original == null) return@launch
            val allUnits = repository.quantityUnits.first()
            val decimals = repository.userSettings.first().stockDecimalPlacesAmounts
            val (ownerName, defaultFrom) = when (owner) {
                is ConversionOwner.Product -> {
                    val product = repository.product(owner.productId).first() ?: return@launch
                    product.name to product.quIdStock
                }
                is ConversionOwner.Unit -> {
                    val unit = allUnits.firstOrNull { it.id == owner.quId } ?: return@launch
                    unit.name to unit.id
                }
            }
            val initial = original?.toFormValues(decimals, decimalSeparator)
                ?: ConversionFormValues(fromQuId = defaultFrom)
            val chosen = setOf(initial.fromQuId, initial.toQuId)
            val fields = repository.userfields(MasterDataRepository.QU_CONVERSIONS).first()
            dialog = ConversionDialogState(
                id = original?.id ?: MasterDataRepository.NEW_OBJECT_ID,
                initial = initial,
                ownerName = ownerName,
                isProductOverride = owner is ConversionOwner.Product,
                isFromFixed = owner is ConversionOwner.Unit,
                units = allUnits
                    .filter { it.active || it.id in chosen }
                    .sortedBy { it.name.lowercase() },
                decimals = decimals,
                userfields = userfieldInputs(fields, original?.userfields),
            )
        }
    }

    fun dismissDialog() {
        if (dialog?.isSaving != true) dialog = null
    }

    /** Saves the open form; it closes once the server saved the conversion. */
    fun save() {
        val form = dialog ?: return
        if (form.isSaving) return
        val values = form.values()
        form.errors = values.validate(form.decimals)
        form.userfieldErrors = form.userfields.validationErrors()
        val hasUnmatched = form.isFromUnmatched || form.isToUnmatched ||
            form.userfields.any { it.isUnmatched }
        if (form.errors.isNotEmpty() || form.userfieldErrors.isNotEmpty() || hasUnmatched) return

        val owner = ownerFlow.value ?: return
        val conversion = values.toConversion(
            id = form.id,
            productId = (owner as? ConversionOwner.Product)?.productId,
        )
        form.isSaving = true
        form.error = null
        viewModelScope.launch {
            val error = changeErrorOf {
                repository.saveQuantityUnitConversion(conversion, form.userfields.changedValues())
            }
            form.isSaving = false
            if (error == null) {
                if (dialog === form) dialog = null
            } else {
                form.error = error
            }
        }
    }
}

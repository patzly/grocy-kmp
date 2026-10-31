package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormState
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.TaskCategory
import kotlinx.coroutines.flow.first

/**
 * The inputs of Grocy's simple master data forms as typed: locations, stores, quantity units,
 * product groups, batteries and task categories; each uses the fields of its form.
 */
internal data class SimpleObjectValues(
    val name: String = "",
    val active: Boolean = true,
    val description: String = "",
    /** Locations. */
    val isFreezer: Boolean = false,
    /** Quantity units. */
    val namePlural: String = "",
    val pluralForms: String = "",
    /** Batteries. */
    val usedIn: String = "",
    val chargeIntervalDays: String = "0",
)

/** Checks the values like Grocy's form of [kind]: the name, the charge cycle of batteries. */
internal fun SimpleObjectValues.validate(kind: ObjectListKind): Map<ObjectField, FieldError> =
    buildMap {
        if (name.isBlank()) put(ObjectField.NAME, FieldError.Required)
        if (kind == ObjectListKind.BATTERIES && wholeNumber(chargeIntervalDays, min = 0) == null) {
            put(ObjectField.CHARGE_INTERVAL, FieldError.InvalidNumber("0", 0))
        }
    }

/** [text] as whole number of at least [min], null if it is none. */
internal fun wholeNumber(text: String, min: Int): Int? =
    text.trim().toIntOrNull()?.takeIf { it >= min }

internal fun Location.toValues() =
    SimpleObjectValues(name, active, description.orEmpty(), isFreezer = isFreezer)

/** Without freezing Grocy's form sends "no freezer" for the hidden checkbox. */
internal fun SimpleObjectValues.toLocation(id: Int, freezing: Boolean) = Location(
    id = id,
    name = name.trim(),
    description = description,
    isFreezer = freezing && isFreezer,
    active = active,
)

internal fun Store.toValues() = SimpleObjectValues(name, active, description.orEmpty())

internal fun SimpleObjectValues.toStore(id: Int) =
    Store(id = id, name = name.trim(), description = description, active = active)

internal fun QuantityUnit.toValues() = SimpleObjectValues(
    name = name,
    active = active,
    description = description.orEmpty(),
    namePlural = namePlural.orEmpty(),
    pluralForms = pluralForms.orEmpty(),
)

/** Plural forms are only part of the form in languages with more than two; else they stay. */
internal fun SimpleObjectValues.toQuantityUnit(
    id: Int,
    original: QuantityUnit?,
    withPluralForms: Boolean,
) = QuantityUnit(
    id = id,
    name = name.trim(),
    namePlural = namePlural.trim(),
    description = description,
    active = active,
    pluralForms = if (withPluralForms) pluralForms.ifBlank { null } else original?.pluralForms,
)

internal fun ProductGroup.toValues() = SimpleObjectValues(name, active, description.orEmpty())

internal fun SimpleObjectValues.toProductGroup(id: Int) =
    ProductGroup(id = id, name = name.trim(), description = description, active = active)

internal fun Battery.toValues() = SimpleObjectValues(
    name = name,
    active = active,
    description = description.orEmpty(),
    usedIn = usedIn.orEmpty(),
    chargeIntervalDays = chargeIntervalDays.toString(),
)

internal fun SimpleObjectValues.toBattery(id: Int) = Battery(
    id = id,
    name = name.trim(),
    description = description,
    active = active,
    usedIn = usedIn,
    chargeIntervalDays = wholeNumber(chargeIntervalDays, min = 0) ?: 0,
)

internal fun TaskCategory.toValues() = SimpleObjectValues(name, active, description.orEmpty())

internal fun SimpleObjectValues.toTaskCategory(id: Int) =
    TaskCategory(id = id, name = name.trim(), description = description, active = active)

@Stable
internal class SimpleObjectFormState(initial: SimpleObjectValues) :
    ObjectFormState<SimpleObjectValues> {
    val name = TextFieldState(initial.name)
    var active by mutableStateOf(initial.active)
    val description = TextFieldState(initial.description)
    var isFreezer by mutableStateOf(initial.isFreezer)
    val namePlural = TextFieldState(initial.namePlural)
    val pluralForms = TextFieldState(initial.pluralForms)
    val usedIn = TextFieldState(initial.usedIn)
    val chargeIntervalDays = TextFieldState(initial.chargeIntervalDays)

    override fun values() = SimpleObjectValues(
        name = name.text.toString(),
        active = active,
        description = description.text.toString(),
        isFreezer = isFreezer,
        namePlural = namePlural.text.toString(),
        pluralForms = pluralForms.text.toString(),
        usedIn = usedIn.text.toString(),
        chargeIntervalDays = chargeIntervalDays.text.toString(),
    )
}

/**
 * Grocy's form of a location, store, quantity unit, product group, battery or task category.
 *
 * @param pluralFormCount plural forms of the user's language; Grocy asks for a unit's plural
 *   forms from three on.
 */
internal class SimpleObjectViewModel(
    val kind: ObjectListKind,
    id: Int?,
    repository: MasterDataRepository,
    val pluralFormCount: Int = 2,
) : ObjectFormViewModel<SimpleObjectValues, SimpleObjectFormState>(id, repository) {

    var showFreezer by mutableStateOf(false)
        private set

    val showPluralForms: Boolean
        get() = pluralFormCount > 2

    override val userfieldEntity: String? = kind.userfieldEntity

    init {
        start()
    }

    override suspend fun createForm(id: Int?): SimpleObjectFormState? {
        showFreezer = repository.serverConfig.first().stockProductFreezing
        if (id == null) return SimpleObjectFormState(SimpleObjectValues())
        val values = when (kind) {
            ObjectListKind.LOCATIONS -> repository.locations.first().find { it.id == id }
                ?.toValues()
            ObjectListKind.STORES -> repository.stores.first().find { it.id == id }?.toValues()
            ObjectListKind.QUANTITY_UNITS -> quantityUnit(id)?.toValues()
            ObjectListKind.PRODUCT_GROUPS -> repository.productGroups.first()
                .find { it.id == id }?.toValues()
            ObjectListKind.BATTERIES -> repository.batteries.first().find { it.id == id }
                ?.toValues()
            ObjectListKind.TASK_CATEGORIES -> repository.taskCategories.first()
                .find { it.id == id }?.toValues()
            else -> error("$kind has a form of its own")
        }
        return values?.let { SimpleObjectFormState(it) }
    }

    override suspend fun userfieldValues(id: Int): Map<String, String?> = when (kind) {
        ObjectListKind.LOCATIONS -> repository.locations.first().find { it.id == id }?.userfields
        ObjectListKind.STORES -> repository.stores.first().find { it.id == id }?.userfields
        ObjectListKind.QUANTITY_UNITS -> quantityUnit(id)?.userfields
        ObjectListKind.PRODUCT_GROUPS -> repository.productGroups.first()
            .find { it.id == id }?.userfields
        ObjectListKind.BATTERIES -> repository.batteries.first().find { it.id == id }?.userfields
        ObjectListKind.TASK_CATEGORIES -> repository.taskCategories.first()
            .find { it.id == id }?.userfields
        else -> null
    }.orEmpty()

    override fun validate(form: SimpleObjectFormState) = form.values().validate(kind)

    override suspend fun save(
        id: Int,
        form: SimpleObjectFormState,
        userfields: Map<String, String?>,
    ): Int {
        val values = form.values()
        return when (kind) {
            ObjectListKind.LOCATIONS ->
                repository.saveLocation(values.toLocation(id, showFreezer), userfields)
            ObjectListKind.STORES -> repository.saveStore(values.toStore(id), userfields)
            ObjectListKind.QUANTITY_UNITS -> repository.saveQuantityUnit(
                values.toQuantityUnit(id, quantityUnit(id), showPluralForms),
                userfields,
            )
            ObjectListKind.PRODUCT_GROUPS ->
                repository.saveProductGroup(values.toProductGroup(id), userfields)
            ObjectListKind.BATTERIES -> repository.saveBattery(values.toBattery(id), userfields)
            ObjectListKind.TASK_CATEGORIES ->
                repository.saveTaskCategory(values.toTaskCategory(id), userfields)
            else -> error("$kind has a form of its own")
        }
    }

    private suspend fun quantityUnit(id: Int): QuantityUnit? =
        repository.quantityUnits.first().find { it.id == id }
}

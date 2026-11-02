package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormState
import com.patrickzedler.grocy.core.forms.ObjectFormViewModel
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.forms.minimumAmount
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.ChoreAssignmentType
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.Weekday
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.decimalPlaces
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.grocyDateTimeOf
import com.patrickzedler.grocy.core.model.text.localNowMillis
import com.patrickzedler.grocy.core.model.text.normalizedDateTime
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The chore form's inputs as typed; a new chore starts with the first choices like Grocy's. */
internal data class ChoreValues(
    val name: String = "",
    val active: Boolean = true,
    val description: String = "",
    val periodType: ChorePeriodType = ChorePeriodType.entries.first(),
    val periodDays: String = "0",
    val periodInterval: String = "1",
    val weekdays: Set<Weekday> = emptySet(),
    val startDate: String = "",
    val assignmentType: ChoreAssignmentType = ChoreAssignmentType.entries.first(),
    val assignedUserIds: Set<Int> = emptySet(),
    val trackDateOnly: Boolean = false,
    val rollover: Boolean = false,
    val consumeProduct: Boolean = false,
    val productId: Int? = null,
    val productAmount: String = "",
)

/** Which parts of the chore form the server's features show. */
internal data class ChoreFormFeatures(val assignments: Boolean, val stock: Boolean) {
    companion object {
        fun of(config: ServerConfig) =
            ChoreFormFeatures(assignments = config.choresAssignments, stock = config.stock)
    }
}

/** Period types whose interval Grocy's form asks for. */
internal val ChorePeriodType.hasInterval: Boolean
    get() = this != ChorePeriodType.MANUALLY && this != ChorePeriodType.ADAPTIVE

/** Checks the values like Grocy's chore form, only the fields the period type shows. */
internal fun ChoreValues.validate(
    features: ChoreFormFeatures,
    decimals: Int,
): Map<ObjectField, FieldError> = buildMap {
    if (name.isBlank()) put(ObjectField.NAME, FieldError.Required)
    if (periodType == ChorePeriodType.MONTHLY &&
        wholeNumber(periodDays, min = 1)?.takeIf { it <= MAX_DAY_OF_MONTH } == null
    ) {
        put(ObjectField.PERIOD_DAYS, FieldError.InvalidNumber("1", 0))
    }
    if (periodType.hasInterval && wholeNumber(periodInterval, min = 1) == null) {
        put(ObjectField.PERIOD_INTERVAL, FieldError.InvalidNumber("1", 0))
    }
    when {
        startDate.isBlank() -> put(ObjectField.START_DATE, FieldError.Required)
        normalizedDateTime(startDate) == null ->
            put(ObjectField.START_DATE, FieldError.InvalidDateTime)
    }
    if (features.assignments && assignmentType != ChoreAssignmentType.NO_ASSIGNMENT &&
        assignedUserIds.isEmpty()
    ) {
        put(ObjectField.ASSIGNED_USERS, FieldError.Required)
    }
    if (features.stock && consumeProduct && productAmount.isNotBlank()) {
        val amount = parseDecimal(productAmount)
        val minimum = minimumAmount(decimals)
        if (amount == null || amount < minimum || decimalPlaces(productAmount) > decimals) {
            put(
                ObjectField.PRODUCT_AMOUNT,
                FieldError.InvalidNumber(formatDecimal(minimum, decimals), decimals),
            )
        }
    }
}

/**
 * The chore to save from valid values, like Grocy's form sends it: values of hidden fields as
 * the period type sets them, without the assignment feature no assignment, and the consumed
 * product of [original] where its inputs are off.
 */
internal fun ChoreValues.toChore(id: Int, original: Chore?, features: ChoreFormFeatures): Chore {
    val consume = if (features.stock) consumeProduct else original?.consumeProductOnExecution
    val ownProduct = features.stock && consumeProduct
    return Chore(
        id = id,
        name = name.trim(),
        description = description,
        active = active,
        periodType = periodType,
        // Grocy's form sets the hidden days to 1, hourly chores keep theirs
        periodDays = when (periodType) {
            ChorePeriodType.MONTHLY, ChorePeriodType.HOURLY -> wholeNumber(periodDays, 0) ?: 0
            else -> 1
        },
        periodInterval = if (periodType.hasInterval) wholeNumber(periodInterval, 1) ?: 1 else 1,
        weekdays = weekdays,
        startDate = normalizedDateTime(startDate),
        assignmentType = if (features.assignments) {
            assignmentType
        } else {
            ChoreAssignmentType.NO_ASSIGNMENT
        },
        assignedUserIds = if (features.assignments) assignedUserIds.sorted() else emptyList(),
        trackDateOnly = trackDateOnly,
        rollover = rollover,
        consumeProductOnExecution = consume ?: false,
        productId = if (ownProduct) productId else original?.productId,
        productAmount = if (ownProduct) {
            parseDecimal(productAmount)
        } else {
            original?.productAmount
        },
        userfields = original?.userfields.orEmpty(),
    )
}

internal fun Chore.toValues(decimals: Int, decimalSeparator: Char) = ChoreValues(
    name = name,
    active = active,
    description = description.orEmpty(),
    periodType = periodType,
    periodDays = periodDays.toString(),
    periodInterval = periodInterval.toString(),
    weekdays = weekdays,
    startDate = startDate?.let { normalizedDateTime(it) ?: it }.orEmpty(),
    assignmentType = assignmentType,
    assignedUserIds = assignedUserIds.toSet(),
    trackDateOnly = trackDateOnly,
    rollover = rollover,
    consumeProduct = consumeProductOnExecution,
    productId = productId,
    productAmount = productAmount?.let { formatDecimal(it, decimals, decimalSeparator) }.orEmpty(),
)

/** The pickers of the chore form. */
internal enum class ChoreChoice { PERIOD_TYPE, ASSIGNMENT_TYPE, PRODUCT }

@Stable
internal class ChoreFormState(initial: ChoreValues) : ObjectFormState<ChoreValues> {
    val name = TextFieldState(initial.name)
    var active by mutableStateOf(initial.active)
    val description = TextFieldState(initial.description)
    var periodType by mutableStateOf(initial.periodType)
    val periodDays = TextFieldState(initial.periodDays)
    val periodInterval = TextFieldState(initial.periodInterval)
    var weekdays by mutableStateOf(initial.weekdays)
    val startDate = TextFieldState(initial.startDate)
    var assignmentType by mutableStateOf(initial.assignmentType)
    var assignedUserIds by mutableStateOf(initial.assignedUserIds)
    var trackDateOnly by mutableStateOf(initial.trackDateOnly)
    var rollover by mutableStateOf(initial.rollover)
    var consumeProduct by mutableStateOf(initial.consumeProduct)
    var productId by mutableStateOf(initial.productId)
    val productAmount = TextFieldState(initial.productAmount)

    var unmatchedChoices by mutableStateOf(emptySet<ChoreChoice>())
        private set

    override val hasUnmatchedChoices: Boolean
        get() = unmatchedChoices.isNotEmpty()

    fun setUnmatched(choice: ChoreChoice, unmatched: Boolean) {
        unmatchedChoices = if (unmatched) unmatchedChoices + choice else unmatchedChoices - choice
    }

    override fun values() = ChoreValues(
        name = name.text.toString(),
        active = active,
        description = description.text.toString(),
        periodType = periodType,
        periodDays = periodDays.text.toString(),
        periodInterval = periodInterval.text.toString(),
        weekdays = weekdays,
        startDate = startDate.text.toString(),
        assignmentType = assignmentType,
        assignedUserIds = assignedUserIds,
        trackDateOnly = trackDateOnly,
        rollover = rollover,
        consumeProduct = consumeProduct,
        productId = productId,
        productAmount = productAmount.text.toString(),
    )
}

/** Choices of the chore form's pickers, from the database. */
internal data class ChoreOptions(
    val users: List<User> = emptyList(),
    val products: List<Product> = emptyList(),
    val quantityUnits: List<QuantityUnit> = emptyList(),
)

/** Grocy's chore form; a new chore starts now, like Grocy's start date. */
internal class ChoreViewModel(
    id: Int?,
    repository: MasterDataRepository,
    private val now: () -> String = { grocyDateTimeOf(localNowMillis()) },
    private val decimalSeparator: Char = platformDecimalSeparator(),
) : ObjectFormViewModel<ChoreValues, ChoreFormState>(id, repository) {

    var features by mutableStateOf(ChoreFormFeatures(assignments = true, stock = true))
        private set

    var decimals by mutableStateOf(DEFAULT_DECIMALS)
        private set

    /** Grocy's server settings of the chore's grocycode. */
    var config by mutableStateOf(ServerConfig())
        private set

    private var original: Chore? = null

    private val labelChannel = Channel<ChangeError?>(Channel.BUFFERED)

    /** The results of [printLabel], null when printed. */
    val labelResults: Flow<ChangeError?> = labelChannel.receiveAsFlow()

    val options: StateFlow<ChoreOptions> = combine(
        repository.users,
        repository.products,
        repository.quantityUnits,
    ) { users, products, units -> ChoreOptions(users, products, units) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ChoreOptions())

    override val userfieldEntity: String = MasterDataRepository.CHORES

    init {
        start()
    }

    override suspend fun createForm(id: Int?): ChoreFormState? {
        config = repository.serverConfig.first()
        features = ChoreFormFeatures.of(config)
        decimals = repository.userSettings.first().stockDecimalPlacesAmounts
        if (id == null) return ChoreFormState(ChoreValues(startDate = now()))
        val chore = repository.chores.first().find { it.id == id } ?: return null
        original = chore
        return ChoreFormState(chore.toValues(decimals, decimalSeparator))
    }

    /** Grocy's "Print on label printer" of the saved chore's grocycode. */
    fun printLabel() {
        viewModelScope.launch {
            labelChannel.send(changeErrorOf { repository.printChoreLabel(objectId) })
        }
    }

    override suspend fun userfieldValues(id: Int): Map<String, String?> =
        repository.chores.first().find { it.id == id }?.userfields.orEmpty()

    override fun validate(form: ChoreFormState) = form.values().validate(features, decimals)

    override suspend fun save(id: Int, form: ChoreFormState, userfields: Map<String, String?>) =
        repository.saveChore(form.values().toChore(id, original, features), userfields).also {
            original = repository.chores.first().find { chore -> chore.id == it }
        }

    private companion object {
        const val DEFAULT_DECIMALS = 4
    }
}

private const val MAX_DAY_OF_MONTH = 31

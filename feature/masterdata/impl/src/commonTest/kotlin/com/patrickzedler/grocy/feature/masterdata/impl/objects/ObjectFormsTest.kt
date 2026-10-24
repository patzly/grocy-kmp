package com.patrickzedler.grocy.feature.masterdata.impl.objects

import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.ChoreAssignmentType
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.masterdata.Weekday
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObjectFormsTest {

    private val allFeatures = ChoreFormFeatures(assignments = true, stock = true)

    @Test
    fun everySimpleObjectNeedsAName() {
        val errors = SimpleObjectValues(name = " ").validate(ObjectListKind.STORES)

        assertEquals(mapOf(ObjectField.NAME to FieldError.Required), errors)
    }

    @Test
    fun batteriesNeedAWholeChargeIntervalOfAtLeastZero() {
        fun errors(days: String) =
            SimpleObjectValues(name = "Remote", chargeIntervalDays = days)
                .validate(ObjectListKind.BATTERIES)

        assertEquals(emptyMap(), errors("0"))
        assertEquals(FieldError.InvalidNumber("0", 0), errors("-1")[ObjectField.CHARGE_INTERVAL])
        assertEquals(FieldError.InvalidNumber("0", 0), errors("1.5")[ObjectField.CHARGE_INTERVAL])
        assertEquals(FieldError.InvalidNumber("0", 0), errors("")[ObjectField.CHARGE_INTERVAL])
    }

    @Test
    fun locationsWithoutFreezingAreNoFreezers() {
        val values = SimpleObjectValues(name = " Cellar ", isFreezer = true)

        assertEquals(true, values.toLocation(1, freezing = true).isFreezer)
        assertEquals(false, values.toLocation(1, freezing = false).isFreezer)
        assertEquals("Cellar", values.toLocation(1, freezing = true).name)
    }

    @Test
    fun hiddenPluralFormsKeepTheSavedOnes() {
        val original = QuantityUnit(1, "Kus", pluralForms = "Kus\nKusy\nKusu")
        val values = original.toValues().copy(pluralForms = "")

        assertEquals("Kus\nKusy\nKusu", values.toQuantityUnit(1, original, false).pluralForms)
        assertNull(values.toQuantityUnit(1, original, withPluralForms = true).pluralForms)
    }

    @Test
    fun chargeIntervalIsSavedAsNumber() {
        val values = SimpleObjectValues(name = "Remote", usedIn = "TV", chargeIntervalDays = " 30 ")

        val battery = values.toBattery(4)

        assertEquals(30, battery.chargeIntervalDays)
        assertEquals("TV", battery.usedIn)
    }

    @Test
    fun choresCheckOnlyTheFieldsOfTheirPeriodType() {
        val manual = ChoreValues(
            name = "Clean",
            periodType = ChorePeriodType.MANUALLY,
            periodInterval = "",
            startDate = "2026-10-24 08:00:00",
            assignmentType = ChoreAssignmentType.NO_ASSIGNMENT,
        )

        assertEquals(emptyMap(), manual.validate(allFeatures, decimals = 2))
        assertEquals(
            setOf(ObjectField.PERIOD_INTERVAL, ObjectField.PERIOD_DAYS),
            manual.copy(periodType = ChorePeriodType.MONTHLY, periodDays = "32")
                .validate(allFeatures, 2).keys,
        )
    }

    @Test
    fun assignedChoresNeedSomeoneOnlyWithTheAssignmentFeature() {
        val values = ChoreValues(
            name = "Clean",
            startDate = "2026-10-24 08:00",
            assignmentType = ChoreAssignmentType.RANDOM,
        )

        assertEquals(
            FieldError.Required,
            values.validate(allFeatures, 2)[ObjectField.ASSIGNED_USERS],
        )
        assertEquals(
            emptyMap(),
            values.validate(allFeatures.copy(assignments = false), 2),
        )
    }

    @Test
    fun startDatesNeedDateAndTime() {
        val values = ChoreValues(name = "Clean", assignmentType = ChoreAssignmentType.NO_ASSIGNMENT)

        assertEquals(FieldError.Required, values.validate(allFeatures, 2)[ObjectField.START_DATE])
        assertEquals(
            FieldError.InvalidDateTime,
            values.copy(startDate = "2026-02-30 10:00").validate(allFeatures, 2)[
                ObjectField.START_DATE,
            ],
        )
    }

    @Test
    fun dateTimesAreNormalizedLikeGrocysPicker() {
        assertEquals("2026-10-24 08:05:00", normalizedDateTime("2026-10-24 08:05"))
        assertEquals("2026-10-24 08:05:09", normalizedDateTime(" 2026-10-24T08:05:09 "))
        assertEquals("2026-10-04 00:00:00", normalizedDateTime("2026-10-04"))
        assertNull(normalizedDateTime("2026-10-24 24:00"))
        assertNull(normalizedDateTime("24.10.2026"))
    }

    @Test
    fun consumedAmountsFollowTheDecimalsOfAmounts() {
        val values = ChoreValues(
            name = "Clean",
            startDate = "2026-10-24 08:00",
            assignmentType = ChoreAssignmentType.NO_ASSIGNMENT,
            consumeProduct = true,
            productId = 1,
            productAmount = "0.001",
        )

        assertEquals(
            FieldError.InvalidNumber("0.01", 2),
            values.validate(allFeatures, decimals = 2)[ObjectField.PRODUCT_AMOUNT],
        )
        assertEquals(emptyMap(), values.copy(productAmount = "0.5").validate(allFeatures, 2))
    }

    @Test
    fun hiddenPeriodFieldsGetTheValuesOfGrocysForm() {
        val values = ChoreValues(
            name = "Clean",
            periodType = ChorePeriodType.ADAPTIVE,
            periodDays = "12",
            periodInterval = "5",
            weekdays = setOf(Weekday.MONDAY),
            startDate = "2026-10-24 08:00",
        )

        val chore = values.toChore(0, original = null, features = allFeatures)

        assertEquals(1, chore.periodDays)
        assertEquals(1, chore.periodInterval)
        assertEquals("2026-10-24 08:00:00", chore.startDate)
        val monthly = values.copy(periodType = ChorePeriodType.MONTHLY)
            .toChore(0, original = null, features = allFeatures)
        assertEquals(12, monthly.periodDays)
        assertEquals(5, monthly.periodInterval)
    }

    @Test
    fun choresWithoutTheAssignmentFeatureAreAssignedToNobody() {
        val values = ChoreValues(
            name = "Clean",
            startDate = "2026-10-24 08:00",
            assignmentType = ChoreAssignmentType.RANDOM,
            assignedUserIds = setOf(3, 1),
        )

        val assigned = values.toChore(1, null, allFeatures)
        val unassigned = values.toChore(1, null, allFeatures.copy(assignments = false))

        assertEquals(listOf(1, 3), assigned.assignedUserIds)
        assertEquals(ChoreAssignmentType.NO_ASSIGNMENT, unassigned.assignmentType)
        assertEquals(emptyList(), unassigned.assignedUserIds)
    }

    @Test
    fun productInputsThatAreOffKeepTheSavedProduct() {
        val original = Chore(1, "Clean", consumeProductOnExecution = false, productId = 7)
        val values = original.toValues(decimals = 2, decimalSeparator = '.')
            .copy(startDate = "2026-10-24 08:00", productId = 9)

        val chore = values.toChore(1, original, allFeatures)

        assertEquals(false, chore.consumeProductOnExecution)
        assertEquals(7, chore.productId)
        val consumed = values.copy(consumeProduct = true).toChore(1, original, allFeatures)
        assertEquals(9, consumed.productId)
    }

    @Test
    fun userfieldsNeedEntityNameCaptionAndType() {
        val errors = UserfieldValues(name = "my field", sortNumber = "-1").validate()

        assertEquals(
            setOf(
                ObjectField.ENTITY,
                ObjectField.NAME,
                ObjectField.CAPTION,
                ObjectField.SORT_NUMBER,
                ObjectField.TYPE,
            ),
            errors.keys,
        )
        assertTrue(isValidFieldName("best_before2"))
    }

    @Test
    fun userfieldsSendOptionsAndDefaultsOnlyForTheirTypes() {
        val values = UserfieldValues(
            entity = "chores",
            name = "room",
            caption = " Room ",
            type = UserfieldType.PRESET_LIST,
            config = "Kitchen\n\n Bath \n",
            defaultValue = USERFIELD_DEFAULT_NOW,
        )

        val list = values.toUserfield(0)
        val date = values.copy(type = UserfieldType.DATE).toUserfield(0)

        assertEquals(listOf("Kitchen", "Bath"), list.options)
        assertNull(list.defaultValue)
        assertEquals("Room", list.caption)
        assertEquals(emptyList(), date.options)
        assertEquals(USERFIELD_DEFAULT_NOW, date.defaultValue)
        assertNull(date.sortNumber)
    }

    @Test
    fun userentitiesNeedAFieldNameAndCaption() {
        assertEquals(
            setOf(ObjectField.NAME, ObjectField.CAPTION),
            UserentityValues(name = "Bücher").validate().keys,
        )
        assertEquals(emptyMap(), UserentityValues(name = "books", caption = "Books").validate())
    }
}

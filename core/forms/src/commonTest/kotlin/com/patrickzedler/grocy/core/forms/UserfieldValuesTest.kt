package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserfieldValuesTest {

    private fun field(type: UserfieldType, required: Boolean = false) =
        Userfield(1, "products", "field", "Field", type, inputRequired = required)

    @Test
    fun requiredFieldsNeedAValue() {
        assertEquals(
            UserfieldError.REQUIRED,
            validateUserfield(field(UserfieldType.TEXT_SINGLE_LINE, required = true), " "),
        )
        assertNull(validateUserfield(field(UserfieldType.TEXT_SINGLE_LINE), ""))
    }

    @Test
    fun filesAreNeverRequiredInTheApp() {
        assertNull(validateUserfield(field(UserfieldType.FILE, required = true), null))
    }

    @Test
    fun numbersAndDatesAreChecked() {
        assertEquals(
            UserfieldError.INVALID_NUMBER,
            validateUserfield(field(UserfieldType.NUMBER_INTEGRAL), "1.5"),
        )
        assertNull(validateUserfield(field(UserfieldType.NUMBER_DECIMAL), "1,5"))
        assertEquals(
            UserfieldError.INVALID_DATE,
            validateUserfield(field(UserfieldType.DATE), "17.10.2026"),
        )
        assertNull(validateUserfield(field(UserfieldType.DATETIME), "2026-10-17 12:00:00"))
    }

    @Test
    fun numbersAreStoredWithADot() {
        assertEquals("1.5", normalizeUserfieldNumber(UserfieldType.NUMBER_DECIMAL, "1,50"))
        assertEquals("3", normalizeUserfieldNumber(UserfieldType.NUMBER_INTEGRAL, "3"))
    }

    @Test
    fun linkWithTitleIsJson() {
        val encoded = encodeLinkWithTitle("Shop", "https://shop.example")

        assertEquals("Shop" to "https://shop.example", decodeLinkWithTitle(encoded))
        assertEquals("", encodeLinkWithTitle("", " "))
        // Plain links of older values stay readable
        assertEquals("" to "https://x.example", decodeLinkWithTitle("https://x.example"))
    }

    @Test
    fun checklistKeepsTheOrderOfTheOptions() {
        val options = listOf("a", "b", "c")

        assertEquals("a,c", encodeChecklist(setOf("c", "a"), options))
        assertEquals(setOf("a", "c"), decodeChecklist("a, c"))
    }

    @Test
    fun nowIsNoDefaultValue() {
        assertNull(defaultUserfieldValue(field(UserfieldType.DATE).copy(defaultValue = "now")))
    }

    @Test
    fun onlyChangedValuesAreSent() {
        val input = UserfieldInput(field(UserfieldType.TEXT_SINGLE_LINE), "old")
        assertFalse(input.isChanged)

        input.text.setTextAndPlaceCursorAtEnd("new")

        assertTrue(input.isChanged)
        assertEquals("new", input.value)
    }

    @Test
    fun untouchedNewCheckboxIsNotSent() {
        val input = UserfieldInput(field(UserfieldType.CHECKBOX), null)

        assertFalse(input.isChanged)

        input.checked = true
        assertTrue(input.isChanged)
        assertEquals("1", input.value)
    }

    @Test
    fun emptyAndMissingValuesAreTheSame() {
        val input = UserfieldInput(field(UserfieldType.TEXT_SINGLE_LINE), null)

        assertFalse(input.isChanged)
    }
}

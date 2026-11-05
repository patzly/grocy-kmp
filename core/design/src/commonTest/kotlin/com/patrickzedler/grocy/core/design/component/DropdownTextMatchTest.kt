package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.text.input.KeyboardType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DropdownTextMatchTest {

    private val options = listOf(DropdownOption(1, "Milk"), DropdownOption(2, "Bread "))

    @Test
    fun textNamingAnOptionChoosesIt() {
        assertEquals(1, dropdownTextMatch(options, "Milk"))
    }

    @Test
    fun caseAndSurroundingSpacesAreIgnored() {
        assertEquals(2, dropdownTextMatch(options, "  bread"))
    }

    @Test
    fun emptyTextChoosesNothing() {
        assertNull(dropdownTextMatch(options, "   "))
    }

    @Test
    fun partOfALabelIsNoMatch() {
        assertEquals(NoMatch, dropdownTextMatch(options, "Mil"))
    }

    @Test
    fun textNamingNoOptionIsAnErrorOnceNoSuggestionIsLeft() {
        assertTrue(showsUnmatchedError(isUnmatched = true, isMenuShown = false, asksAboutText = false))
        assertFalse(showsUnmatchedError(isUnmatched = true, isMenuShown = true, asksAboutText = false))
        assertFalse(showsUnmatchedError(isUnmatched = false, isMenuShown = false, asksAboutText = false))
    }

    @Test
    fun aFieldThatAsksAboutItsTextShowsNoError() {
        assertFalse(showsUnmatchedError(isUnmatched = true, isMenuShown = false, asksAboutText = true))
    }

    @Test
    fun theKeyboardsActionTakesTheBestSuggestionForTypedText() {
        assertEquals(1, typedChoice(isTyped = true, "Mi", options))
    }

    @Test
    fun goingOnThroughAFormChoosesNothing() {
        assertNull(typedChoice(isTyped = false, "", options))
        assertNull(typedChoice(isTyped = false, "Milk", options))
        assertNull(typedChoice(isTyped = true, "  ", options))
    }

    @Test
    fun onlyNumbersAreSelectedWhenTheyTakeTheFocus() {
        assertTrue(selectsOnFocus(KeyboardType.Decimal))
        assertTrue(selectsOnFocus(KeyboardType.Number))
        assertFalse(selectsOnFocus(KeyboardType.Text))
    }

    @Test
    fun onlyASuggestionContainingTheTextIsLiteral() {
        val milk = DropdownOption(1, "Milk", searchTexts = listOf("4001"))

        assertTrue(hasLiteralSuggestion(listOf(milk), "mil"))
        assertTrue(hasLiteralSuggestion(listOf(milk), "400"))
        // Found by forgiving a typo: a new name, not this product
        assertFalse(hasLiteralSuggestion(listOf(milk), "Milh"))
        assertFalse(hasLiteralSuggestion(emptyList(), "Milk"))
    }
}

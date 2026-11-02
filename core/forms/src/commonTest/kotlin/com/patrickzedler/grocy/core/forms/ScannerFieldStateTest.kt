package com.patrickzedler.grocy.core.forms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScannerFieldStateTest {

    private val remembered = mutableListOf<Boolean>()

    private fun state(rememberedOpen: Boolean = false, hasValue: Boolean = false) =
        ScannerFieldState(rememberedOpen, { remembered += it }, hasValue)

    @Test
    fun keyboardUserStartsTypingInTheField() {
        val state = state(rememberedOpen = false)

        assertFalse(state.isOpen)
        assertEquals(FormFocusTarget.CODE_FIELD, state.focusTarget)
    }

    @Test
    fun scannerUserStartsWithTheScannerAndNoKeyboard() {
        val state = state(rememberedOpen = true)

        assertTrue(state.isOpen)
        assertNull(state.focusTarget)
    }

    @Test
    fun formWithItsValueNeedsNeitherScannerNorField() {
        val state = state(rememberedOpen = true, hasValue = true)

        assertFalse(state.isOpen)
        assertNull(state.focusTarget)
    }

    @Test
    fun formWithItsValueKeepsWhatTheUserUsedLast() {
        val state = state(rememberedOpen = true, hasValue = true)

        state.onFormReset()

        assertTrue(state.isOpen)
        assertTrue(remembered.isEmpty())
    }

    @Test
    fun openingWithTheButtonIsRemembered() {
        val state = state()
        state.onFocusHandled()

        state.toggle()

        assertTrue(state.isOpen)
        assertNull(state.focusTarget)
        assertEquals(listOf(true), remembered)
    }

    @Test
    fun closingWithTheButtonTypesInsteadAndIsRemembered() {
        val state = state(rememberedOpen = true)

        state.toggle()

        assertFalse(state.isOpen)
        assertEquals(FormFocusTarget.CODE_FIELD, state.focusTarget)
        assertEquals(listOf(false), remembered)
    }

    @Test
    fun scanClosesTheScannerButTheNextObjectIsScannedAgain() {
        val state = state(rememberedOpen = true)

        state.onScanned()
        assertFalse(state.isOpen)

        state.onFormReset()
        assertTrue(state.isOpen)
        assertNull(state.focusTarget)
    }

    @Test
    fun scanModeKeepsTheScannerOpen() {
        val state = state(rememberedOpen = true)

        state.onScanned(staysOpen = true)

        assertTrue(state.isOpen)
    }

    @Test
    fun tappingIntoTheFieldWhileScanningSwitchesToTyping() {
        val state = state(rememberedOpen = true)

        state.onFieldFocusChange(true)

        assertFalse(state.isOpen)
        assertEquals(listOf(false), remembered)
        state.onFormReset()
        assertFalse(state.isOpen)
        assertEquals(FormFocusTarget.CODE_FIELD, state.focusTarget)
    }

    @Test
    fun choosingFromTheFocusedFieldMovesOn() {
        val state = state()
        state.onFocusHandled()
        state.onFieldFocusChange(true)

        state.onValueChosen()

        assertEquals(FormFocusTarget.NEXT_FIELD, state.focusTarget)
    }

    @Test
    fun choosingWithTheArrowKeepsTheKeyboardClosed() {
        val state = state()
        state.onFocusHandled()

        state.onValueChosen()

        assertNull(state.focusTarget)
    }

    @Test
    fun unchangedChoiceIsNotStoredAgain() {
        val state = state(rememberedOpen = true)

        state.onScanned()

        assertTrue(remembered.isEmpty())
    }
}

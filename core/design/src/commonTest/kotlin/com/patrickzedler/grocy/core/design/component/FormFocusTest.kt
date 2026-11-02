package com.patrickzedler.grocy.core.design.component

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormFocusTest {

    @Test
    fun refusedSubmitLooksForTheInvalidField() {
        val form = FormFocus(onSubmit = { false })

        form.submit()

        assertTrue(form.isSeekingError)
    }

    @Test
    fun sentSubmitLooksForNothing() {
        val form = FormFocus(onSubmit = { true })

        form.submit()

        assertFalse(form.isSeekingError)
    }

    @Test
    fun onlyTheFirstInvalidFieldGetsTheFocus() {
        val form = FormFocus(onSubmit = { false })
        form.submit()

        assertTrue(form.claimError())

        assertFalse(form.claimError())
        assertFalse(form.isSeekingError)
    }

    @Test
    fun submitThatGoesThroughEndsAnEarlierSearch() {
        var isValid = false
        val form = FormFocus(onSubmit = { isValid })
        form.submit()

        isValid = true
        form.submit()

        assertFalse(form.isSeekingError)
    }
}

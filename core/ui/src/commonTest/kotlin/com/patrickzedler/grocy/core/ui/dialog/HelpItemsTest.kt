package com.patrickzedler.grocy.core.ui.dialog

import kotlin.test.Test
import kotlin.test.assertEquals

class HelpItemsTest {

    @Test
    fun everyQuestionShowsItsOwnAnswer() {
        val mismatched = HelpItems.filter { it.answer.key != "${it.question.key}_answer" }

        assertEquals(emptyList(), mismatched.map { it.question.key })
    }

    @Test
    fun everyQuestionIsAskedOnce() {
        val keys = HelpItems.map { it.question.key }

        assertEquals(keys.distinct(), keys)
    }

    @Test
    fun hostingIsExplainedRightAfterWhatGrocyIs() {
        val keys = HelpItems.map { it.question.key }

        assertEquals(listOf("help_grocy", "help_hosting"), keys.take(2))
    }
}

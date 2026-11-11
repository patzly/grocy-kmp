package com.patrickzedler.grocy.feature.settings.impl

import com.patrickzedler.grocy.feature.settings.impl.server.serverConfigMarkup
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerConfigMarkupTest {

    @Test
    fun everyPartIsAHeadlineOverItsBullets() {
        val markup = serverConfigMarkup(
            listOf(
                "Enabled" to listOf("Stock", "Chores"),
                "Values" to listOf("Currency: EUR"),
            ),
        )

        assertEquals(
            "### Enabled\n\n- Stock\n- Chores\n\n### Values\n\n- Currency: EUR",
            markup,
        )
    }

    @Test
    fun aPartWithoutEntriesIsLeftOut() {
        val markup = serverConfigMarkup(
            listOf("Disabled" to emptyList(), "Values" to listOf("Currency: EUR")),
        )

        assertEquals("### Values\n\n- Currency: EUR", markup)
    }
}

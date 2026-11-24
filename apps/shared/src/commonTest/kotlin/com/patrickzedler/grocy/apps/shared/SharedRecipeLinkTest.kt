package com.patrickzedler.grocy.apps.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedRecipeLinkTest {

    @Test
    fun aSharedAddressIsTheLink() {
        assertEquals(
            "https://www.chefkoch.de/rezepte/1/suppe.html",
            sharedRecipeLinkOf("https://www.chefkoch.de/rezepte/1/suppe.html"),
        )
    }

    @Test
    fun theAddressIsFoundInTheTextAroundIt() {
        assertEquals(
            "https://www.chefkoch.de/rezepte/1/suppe.html",
            sharedRecipeLinkOf("Schau mal: https://www.chefkoch.de/rezepte/1/suppe.html."),
        )
        assertEquals(
            "https://kitchenstories.com/de/rezepte/x",
            sharedRecipeLinkOf("Spaghetti Carbonara\nhttps://kitchenstories.com/de/rezepte/x"),
        )
    }

    @Test
    fun textWithoutAddressSharesNothing() {
        assertNull(sharedRecipeLinkOf("Spaghetti Carbonara"))
        assertNull(sharedRecipeLinkOf("ftp://example.com/x"))
        assertNull(sharedRecipeLinkOf(null))
    }
}

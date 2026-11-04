package com.patrickzedler.grocy.core.forms

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlTextTest {
    @Test
    fun lineBreaksOfTheSourceAreSpacesLikeInABrowser() {
        assertEquals(
            "Lorem ipsum dolor sit amet",
            htmlToPlainText("<p>Lorem ipsum\ndolor sit\n  amet</p>"),
        )
    }

    @Test
    fun tagsBreakLinesAndParagraphsStaySeparated() {
        assertEquals(
            "First\nline\n\nSecond",
            htmlToPlainText("<p>First<br>\nline</p>\n<p>Second</p>"),
        )
    }

    @Test
    fun listItemsGetABullet() {
        assertEquals("• One\n\n• Two", htmlToPlainText("<ul>\n<li>One</li>\n<li>Two</li>\n</ul>"))
    }

    @Test
    fun plainTextKeepsItsLineBreaks() {
        assertEquals("First\nSecond", htmlToPlainText("First\nSecond"))
    }

    @Test
    fun writtenNotesReadTheSameAgain() {
        assertEquals("One\nTwo\n\nThree", htmlToPlainText(plainTextToHtml("One\nTwo\n\nThree")))
    }
}

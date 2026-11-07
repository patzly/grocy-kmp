package com.patrickzedler.grocy.core.scanner.mlkit

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingOrderTest {

    @Test
    fun linesAreReadTopToBottomThenLeftToRight() {
        val lines = listOf(
            RecognizedLine("L5280226", top = 40, left = 10),
            RecognizedLine("22:27", top = 0, left = 90),
            RecognizedLine("06.10.2027", top = 0, left = 10),
        )

        assertEquals(listOf("06.10.2027", "22:27", "L5280226"), readingOrder(lines))
    }
}

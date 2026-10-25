package com.patrickzedler.grocy.core.scanner.writer

/**
 * Writes Code 128 in code set B, the 1D grocycodes of Grocy's `GROCYCODE_TYPE` "1D": printable
 * ASCII, a check symbol and the stop pattern.
 */
object Code128Writer {

    /** One row of modules, bars dark; the quiet zone around it is left to the drawing. */
    fun write(text: String): BarcodeMatrix {
        val values = mutableListOf(START_B)
        text.forEach { c ->
            require(c.code in 32..126) { "Not encodable in code set B: $c" }
            values += c.code - 32
        }
        val checksum = values.foldIndexed(0) { index, sum, value ->
            sum + value * maxOf(index, 1)
        } % 103
        values += checksum
        val widths = values.flatMap { PATTERNS[it].map { width -> width - '0' } } +
            STOP.map { it - '0' }
        val matrix = BarcodeMatrix(widths.sum(), 1)
        var x = 0
        widths.forEachIndexed { index, width ->
            // Patterns start with a bar and alternate
            repeat(width) { matrix[x++, 0] = index % 2 == 0 }
        }
        return matrix
    }

    private const val START_B = 104
    private const val STOP = "2331112"

    // Bar and space widths of the symbol values 0 to 105
    internal val PATTERNS = listOf(
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312",
        "132212", "221213", "221312", "231212", "112232", "122132", "122231", "113222",
        "123122", "123221", "223211", "221132", "221231", "213212", "223112", "312131",
        "311222", "321122", "321221", "312212", "322112", "322211", "212123", "212321",
        "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121",
        "313121", "211331", "231131", "213113", "213311", "213131", "311123", "311321",
        "331121", "312113", "312311", "332111", "314111", "221411", "431111", "111224",
        "111422", "121124", "121421", "141122", "141221", "112214", "112412", "122114",
        "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112",
        "421211", "212141", "214121", "412121", "111143", "111341", "131141", "114113",
        "114311", "411113", "411311", "113141", "114131", "311141", "411131", "211412",
        "211214", "211232",
    )
}

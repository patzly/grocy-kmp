package com.patrickzedler.grocy.core.scanner.writer

/**
 * Writes square ECC 200 Data Matrix symbols of ISO/IEC 16022 in ASCII encodation, the code
 * Grocy prints its grocycodes as. Symbols up to 48x48 modules, enough for every grocycode.
 */
object DataMatrixWriter {

    /** The symbol of [text]; the quiet zone around it is left to the drawing. */
    fun write(text: String): BarcodeMatrix {
        val data = encodeAscii(text)
        val symbol = SYMBOLS.firstOrNull { it.dataCodewords >= data.size }
            ?: throw IllegalArgumentException("Too long for a Data Matrix symbol: $text")
        val codewords = pad(data, symbol.dataCodewords)
        val all = codewords + ReedSolomon.encode(codewords, symbol.errorCodewords)
        val placement = placeModules(all, symbol.mappingSize, symbol.mappingSize)
        return symbolOf(placement, symbol)
    }

    /** ASCII encodation: digit pairs as one codeword, other characters as their code + 1. */
    internal fun encodeAscii(text: String): IntArray {
        val result = mutableListOf<Int>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val next = text.getOrNull(i + 1)
            if (c.isAsciiDigit() && next != null && next.isAsciiDigit()) {
                result += DIGIT_PAIR_BASE + (c - '0') * 10 + (next - '0')
                i += 2
            } else {
                val code = c.code
                require(code < EXTENDED_ASCII_LIMIT) { "Not encodable: $c" }
                if (code < ASCII_LIMIT) {
                    result += code + 1
                } else {
                    result += UPPER_SHIFT
                    result += code - ASCII_LIMIT + 1
                }
                i++
            }
        }
        return result.toIntArray()
    }

    /** Fills the symbol's free data codewords: one pad, then pseudo-random ones. */
    internal fun pad(data: IntArray, capacity: Int): IntArray {
        val result = data.copyOf(capacity)
        for (index in data.size until capacity) {
            result[index] = if (index == data.size) {
                PAD
            } else {
                // The 253-state randomizing algorithm of the standard, position counted from 1
                val random = (149 * (index + 1)) % 253 + 1
                val value = PAD + random
                if (value <= 254) value else value - 254
            }
        }
        return result
    }

    /** The standard's placement of the codewords' bits in the mapping matrix (Annex F). */
    internal fun placeModules(codewords: IntArray, rows: Int, cols: Int): Array<BooleanArray> {
        val placement = Placement(codewords, rows, cols)
        placement.place()
        return placement.bits
    }

    /** The mapping matrix split into data regions, each with its finder pattern. */
    private fun symbolOf(mapping: Array<BooleanArray>, symbol: SymbolInfo): BarcodeMatrix {
        val region = symbol.regionSize
        val regionsPerSide = symbol.mappingSize / region
        val size = symbol.mappingSize + 2 * regionsPerSide
        val matrix = BarcodeMatrix(size, size)
        var matrixY = 0
        for (y in 0 until symbol.mappingSize) {
            if (y % region == 0) {
                // Clock track along the top of each region row
                for (x in 0 until size) matrix[x, matrixY] = x % 2 == 0
                matrixY++
            }
            var matrixX = 0
            for (x in 0 until symbol.mappingSize) {
                if (x % region == 0) {
                    // Solid finder line on the left of each region
                    matrix[matrixX, matrixY] = true
                    matrixX++
                }
                matrix[matrixX, matrixY] = mapping[y][x]
                matrixX++
                if (x % region == region - 1) {
                    // Clock track on the right of each region
                    matrix[matrixX, matrixY] = y % 2 == 0
                    matrixX++
                }
            }
            matrixY++
            if (y % region == region - 1) {
                // Solid finder line along the bottom of each region row
                for (x in 0 until size) matrix[x, matrixY] = true
                matrixY++
            }
        }
        return matrix
    }

    private class Placement(
        private val codewords: IntArray,
        private val rows: Int,
        private val cols: Int,
    ) {
        private val placed = Array(rows) { BooleanArray(cols) }
        val bits = Array(rows) { BooleanArray(cols) }

        fun place() {
            var pos = 0
            var row = 4
            var col = 0
            do {
                if (row == rows && col == 0) corner1(pos++)
                if (row == rows - 2 && col == 0 && cols % 4 != 0) corner2(pos++)
                if (row == rows - 2 && col == 0 && cols % 8 == 4) corner3(pos++)
                if (row == rows + 4 && col == 2 && cols % 8 == 0) corner4(pos++)
                // Diagonally up and right
                do {
                    if (row < rows && col >= 0 && !placed[row][col]) utah(row, col, pos++)
                    row -= 2
                    col += 2
                } while (row >= 0 && col < cols)
                row++
                col += 3
                // Diagonally down and left
                do {
                    if (row >= 0 && col < cols && !placed[row][col]) utah(row, col, pos++)
                    row += 2
                    col -= 2
                } while (row < rows && col >= 0)
                row += 3
                col++
            } while (row < rows || col < cols)
            // The fixed pattern of the lower right corner where no codeword reaches
            if (!placed[rows - 1][cols - 1]) {
                set(rows - 1, cols - 1, true)
                set(rows - 2, cols - 2, true)
                set(rows - 1, cols - 2, false)
                set(rows - 2, cols - 1, false)
            }
        }

        private fun set(row: Int, col: Int, value: Boolean) {
            placed[row][col] = true
            bits[row][col] = value
        }

        private fun module(row: Int, col: Int, pos: Int, bit: Int) {
            var r = row
            var c = col
            if (r < 0) {
                r += rows
                c += 4 - (rows + 4) % 8
            }
            if (c < 0) {
                c += cols
                r += 4 - (cols + 4) % 8
            }
            set(r, c, codewords[pos] and (1 shl (8 - bit)) != 0)
        }

        private fun utah(row: Int, col: Int, pos: Int) {
            module(row - 2, col - 2, pos, 1)
            module(row - 2, col - 1, pos, 2)
            module(row - 1, col - 2, pos, 3)
            module(row - 1, col - 1, pos, 4)
            module(row - 1, col, pos, 5)
            module(row, col - 2, pos, 6)
            module(row, col - 1, pos, 7)
            module(row, col, pos, 8)
        }

        private fun corner1(pos: Int) {
            module(rows - 1, 0, pos, 1)
            module(rows - 1, 1, pos, 2)
            module(rows - 1, 2, pos, 3)
            module(0, cols - 2, pos, 4)
            module(0, cols - 1, pos, 5)
            module(1, cols - 1, pos, 6)
            module(2, cols - 1, pos, 7)
            module(3, cols - 1, pos, 8)
        }

        private fun corner2(pos: Int) {
            module(rows - 3, 0, pos, 1)
            module(rows - 2, 0, pos, 2)
            module(rows - 1, 0, pos, 3)
            module(0, cols - 4, pos, 4)
            module(0, cols - 3, pos, 5)
            module(0, cols - 2, pos, 6)
            module(0, cols - 1, pos, 7)
            module(1, cols - 1, pos, 8)
        }

        private fun corner3(pos: Int) {
            module(rows - 3, 0, pos, 1)
            module(rows - 2, 0, pos, 2)
            module(rows - 1, 0, pos, 3)
            module(0, cols - 2, pos, 4)
            module(0, cols - 1, pos, 5)
            module(1, cols - 1, pos, 6)
            module(2, cols - 1, pos, 7)
            module(3, cols - 1, pos, 8)
        }

        private fun corner4(pos: Int) {
            module(rows - 1, 0, pos, 1)
            module(rows - 1, cols - 1, pos, 2)
            module(0, cols - 3, pos, 3)
            module(0, cols - 2, pos, 4)
            module(0, cols - 1, pos, 5)
            module(1, cols - 3, pos, 6)
            module(1, cols - 2, pos, 7)
            module(1, cols - 1, pos, 8)
        }
    }

    /** A square symbol size: its data and error codewords and data regions. */
    private class SymbolInfo(
        val size: Int,
        val dataCodewords: Int,
        val errorCodewords: Int,
        val regionSize: Int,
    ) {
        /** Side of the matrix the codewords are placed in, without the finder patterns. */
        val mappingSize: Int
            get() = size - 2 * (size / (regionSize + 2))
    }

    // The square symbols of one interleaved block, from the standard's table
    private val SYMBOLS = listOf(
        SymbolInfo(10, 3, 5, 8),
        SymbolInfo(12, 5, 7, 10),
        SymbolInfo(14, 8, 10, 12),
        SymbolInfo(16, 12, 12, 14),
        SymbolInfo(18, 18, 14, 16),
        SymbolInfo(20, 22, 18, 18),
        SymbolInfo(22, 30, 20, 20),
        SymbolInfo(24, 36, 24, 22),
        SymbolInfo(26, 44, 28, 24),
        SymbolInfo(32, 62, 36, 14),
        SymbolInfo(36, 86, 42, 16),
        SymbolInfo(40, 114, 48, 18),
        SymbolInfo(44, 144, 56, 20),
        SymbolInfo(48, 174, 68, 22),
    )

    private const val PAD = 129
    private const val DIGIT_PAIR_BASE = 130
    private const val UPPER_SHIFT = 235
    private const val ASCII_LIMIT = 128
    private const val EXTENDED_ASCII_LIMIT = 256

    private fun Char.isAsciiDigit() = this in '0'..'9'
}

/** Reed-Solomon error correction over GF(256) with Data Matrix's polynomial 0x12D. */
internal object ReedSolomon {

    private val exp = IntArray(255)
    private val log = IntArray(256)

    init {
        var value = 1
        for (i in 0 until 255) {
            exp[i] = value
            log[value] = i
            value = value shl 1
            if (value >= 256) value = value xor PRIMITIVE
        }
    }

    private fun multiply(a: Int, b: Int): Int =
        if (a == 0 || b == 0) 0 else exp[(log[a] + log[b]) % 255]

    /** Coefficients of the generator polynomial with the roots 2^1 … 2^[count], highest first. */
    internal fun generator(count: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 1..count) {
            val next = IntArray(poly.size + 1)
            val root = exp[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor multiply(poly[j], root)
            }
            poly = next
        }
        return poly
    }

    /** The [count] error correction codewords of [data]: the remainder of the division. */
    fun encode(data: IntArray, count: Int): IntArray {
        val generator = generator(count)
        val remainder = IntArray(count)
        for (codeword in data) {
            val factor = codeword xor remainder[0]
            for (i in 0 until count - 1) {
                remainder[i] = remainder[i + 1] xor multiply(factor, generator[i + 1])
            }
            remainder[count - 1] = multiply(factor, generator[count])
        }
        return remainder
    }

    private const val PRIMITIVE = 0x12D
}

package com.patrickzedler.grocy.core.model.text

/**
 * A due date read from a package. [alternative] is the other reading of a date whose day and
 * month could be swapped, e.g. 06.10.2027 as 6 October or as June 10; null when there is only one.
 */
data class PrintedDueDate(
    val epochDay: Long,
    val alternative: Long? = null,
)

/**
 * The due date among [lines] of text read from a package, top to bottom, or null without one.
 *
 * Finds dates like `06.10.2027`, `07.08.27`, `2027-10-06`, `10/2029` (the month's last day, like
 * "best before end"), `06 OCT 2027` and, right after a keyword, `061027`. Lot numbers, times and
 * other codes around them do not count. A date after "MHD", "best before", "EXP" and the like in
 * its line or the two lines before wins, one after "produced" or "packed" loses, a later one wins
 * over an earlier one, so a production date next to the due date is not taken.
 *
 * Dots separate day, month and year in that order wherever they are used. Other separators
 * follow [order] (day first for year first orders) with the swapped date as alternative. Only
 * dates from about three years before [today] to 25 years after it count.
 */
fun printedDueDateOf(lines: List<String>, today: Long, order: DateFieldOrder): PrintedDueDate? {
    val normalized = lines.map(::normalizedPrintedLine)
    val candidates = normalized.flatMapIndexed { index, line ->
        val before = normalized.subList(maxOf(0, index - KEYWORD_LINES_BEFORE), index)
        datesIn(line, order).mapNotNull { found ->
            if (found.epochDay !in (today - DAYS_BEFORE_TODAY)..(today + DAYS_AFTER_TODAY)) {
                return@mapNotNull null
            }
            val prefix = line.substring(0, found.start)
            var score = found.precision
            if (DUE_KEYWORD.containsMatchIn(prefix)) {
                score += SAME_LINE_KEYWORD
            } else if (before.any { DUE_KEYWORD.containsMatchIn(it) }) {
                score += NEARBY_KEYWORD
            }
            if (OTHER_DATE_KEYWORD.containsMatchIn(prefix)) score -= OTHER_DATE_PENALTY
            if (found.epochDay < today) score -= PAST_PENALTY
            found to score
        }
    }
    val (best, _) = candidates.maxWithOrNull(
        compareBy<Pair<FoundDate, Int>> { it.second }.thenBy { it.first.epochDay },
    ) ?: return null
    return PrintedDueDate(best.epochDay, best.alternative)
}

/**
 * Turns the readings of consecutive camera frames into one due date: a date counts once it was
 * read [needed] times among the last [window] frames, so a misread digit of one frame never
 * fills a field. Day and month swapped count as the same date.
 */
class PrintedDueDateVotes(
    private val needed: Int = 2,
    private val window: Int = 5,
) {
    private val recent = ArrayDeque<PrintedDueDate?>()

    /** The reading of the next frame; returns the date that is now certain, or null. */
    fun accept(reading: PrintedDueDate?): PrintedDueDate? {
        recent.addLast(reading)
        if (recent.size > window) recent.removeFirst()
        if (reading == null) return null
        val same = recent.count { it != null && it.sameDateAs(reading) }
        return reading.takeIf { same >= needed }
    }

    /** Starts over, e.g. when the camera points at the next package. */
    fun reset() {
        recent.clear()
    }

    private fun PrintedDueDate.sameDateAs(other: PrintedDueDate) =
        setOfNotNull(epochDay, alternative) == setOfNotNull(other.epochDay, other.alternative)
}

private class FoundDate(
    val start: Int,
    val epochDay: Long,
    val alternative: Long?,
    /** Full dates with four-digit years are the most certain dates of a package. */
    val precision: Int,
)

/**
 * [line] in upper case with what text recognition confuses in numbers fixed: O and Q are 0, I,
 * a small l and | are 1 next to digits, and commas or middle dots are the dots of dates.
 */
internal fun normalizedPrintedLine(line: String): String {
    val fixed = CharArray(line.length) { index ->
        val char = line[index]
        val previous = line.getOrNull(index - 1)
        val next = line.getOrNull(index + 1)
        // "06OCT" keeps its O, "2O27" and "O6.10" lose it; a lot's "L5" stays
        val inNumber = (previous?.isDigit() == true || next?.isDigit() == true) &&
            previous?.isLetter() != true && next?.isLetter() != true
        when {
            char in ZERO_LOOKALIKES && inNumber -> '0'
            char in ONE_LOOKALIKES && inNumber -> '1'
            char == ',' || char == '·' -> '.'
            else -> char.uppercaseChar()
        }
    }
    return fixed.concatToString()
}

private fun datesIn(line: String, order: DateFieldOrder): List<FoundDate> {
    val found = mutableListOf<FoundDate>()
    val taken = mutableListOf<IntRange>()
    fun add(match: MatchResult, date: FoundDate?) {
        val overlaps = taken.any { it.first <= match.range.last && match.range.first <= it.last }
        if (date == null || overlaps) return
        taken += match.range
        found += date
    }
    NUMERIC.findAll(line).forEach { match ->
        val (first, second, year) = match.destructured
        // Day first wherever dates are written with dots
        val dateOrder = if ('.' in match.value) DateFieldOrder.DAY_MONTH_YEAR else order
        val date = numericDate(match.range.first, first.toInt(), second.toInt(), year, dateOrder)
        add(match, if ('.' in match.value) date?.withoutAlternative() else date)
    }
    YEAR_FIRST.findAll(line).forEach { match ->
        val (year, _, month, day) = match.destructured
        add(match, fullDate(match.range.first, year, month.toInt(), day.toInt(), alternative = null))
    }
    MONTH_NAME.findAll(line).forEach { match ->
        val (day, name, year) = match.destructured
        val month = MONTH_WORDS[name] ?: return@forEach
        add(
            match,
            if (day.isEmpty()) {
                monthDate(match.range.first, year, month)
            } else {
                fullDate(match.range.first, year, month, day.toInt(), alternative = null)
            },
        )
    }
    MONTH_YEAR.findAll(line).forEach { match ->
        val (month, year) = match.destructured
        add(match, monthDate(match.range.first, year, month.toInt()))
    }
    // Digits without separators are lot numbers too often, only a keyword makes them a date
    COMPACT_AFTER_KEYWORD.findAll(line).forEach { match ->
        val (first, second, year) = match.destructured
        val start = match.groups[1]!!.range.first
        add(match, numericDate(start, first.toInt(), second.toInt(), year, order))
    }
    return found
}

/** A date of day and month in either order, read in [order] first. */
private fun numericDate(
    start: Int,
    first: Int,
    second: Int,
    year: String,
    order: DateFieldOrder,
): FoundDate? {
    val dayFirst = order != DateFieldOrder.MONTH_DAY_YEAR
    val (day, month) = if (dayFirst) first to second else second to first
    val preferred = epochDayOf(year, month, day)
    val swapped = epochDayOf(year, day, month).takeIf { first != second }
    val epochDay = preferred ?: swapped ?: return null
    return FoundDate(
        start = start,
        epochDay = epochDay,
        alternative = swapped.takeIf { preferred != null },
        precision = precisionOf(year, isFullDate = true),
    )
}

private fun FoundDate.withoutAlternative() = FoundDate(start, epochDay, null, precision)

private fun fullDate(start: Int, year: String, month: Int, day: Int, alternative: Long?) =
    epochDayOf(year, month, day)?.let {
        FoundDate(start, it, alternative, precisionOf(year, isFullDate = true))
    }

/** "Best before end of": the month's last day. */
private fun monthDate(start: Int, year: String, month: Int): FoundDate? {
    val first = epochDayOf(year, month, 1) ?: return null
    return FoundDate(start, lastDayOfMonth(first), null, precisionOf(year, isFullDate = false))
}

private fun precisionOf(year: String, isFullDate: Boolean) =
    (if (isFullDate) 1 else 0) + (if (year.length == 4) 1 else 0)

private fun epochDayOf(year: String, month: Int, day: Int): Long? {
    val fullYear = if (year.length == 2) "20$year" else year
    val iso = "$fullYear-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    return if (month in 1..12 && day in 1..31) epochDayOf(iso) else null
}

/** Month names and their usual abbreviations in English, German, French, Italian, Spanish, Dutch. */
private val MONTH_WORDS: Map<String, Int> = listOf(
    "JAN JANUARY JANUAR JÄNNER JANVIER GEN GENNAIO ENE ENERO JANUARI",
    "FEB FEBRUARY FEBRUAR FEV FÉV FÉVRIER FEBBRAIO FEBRERO FEBRUARI",
    "MAR MARCH MÄR MÄRZ MRZ MARS MARZO MRT MAART",
    "APR APRIL AVR AVRIL APRILE ABR ABRIL",
    "MAY MAI MAG MAGGIO MAYO MEI",
    "JUN JUNE JUNI JUIN GIU GIUGNO JUNIO",
    "JUL JULY JULI JUIL JUILLET LUG LUGLIO JULIO",
    "AUG AUGUST AOU AOÛ AOÛT AGO AGOSTO AUGUSTUS",
    "SEP SEPT SEPTEMBER SEPTEMBRE SET SETTEMBRE SEPTIEMBRE",
    "OCT OCTOBER OKT OKTOBER OCTOBRE OTT OTTOBRE OCTUBRE",
    "NOV NOVEMBER NOVEMBRE NOVIEMBRE",
    "DEC DECEMBER DEZ DEZEMBER DÉC DÉCEMBRE DIC DICEMBRE DICIEMBRE",
).flatMapIndexed { index, words -> words.split(' ').map { it to index + 1 } }.toMap()

// A separator of date parts: a dot, slash or dash with spaces around it, or spaces alone
private const val SEPARATOR = """(?:\s*[./-]\s*|\s+)"""
private const val YEAR = """(\d{4}|\d{2})(?!\d)"""

private val NUMERIC = Regex("""(?<!\d)(\d{1,2})$SEPARATOR(\d{1,2})$SEPARATOR$YEAR""")
// The same real separator twice, so a year and a date after it are no date
private val YEAR_FIRST = Regex("""(?<!\d)(20\d{2})\s*([./-])\s*(\d{1,2})\s*\2\s*(\d{1,2})(?!\d)""")
private val MONTH_NAME = Regex(
    """(?<![0-9A-ZÄÖÜÉÛ])(?:(\d{1,2})$SEPARATOR?)?(${
        MONTH_WORDS.keys.sortedByDescending { it.length }.joinToString("|")
    })(?![A-ZÄÖÜÉÛ])\.?$SEPARATOR?$YEAR""",
)

// Month and year need a real separator, numbers with spaces between them are too common
private val MONTH_YEAR = Regex("""(?<![\d.:/-])(\d{1,2})\s*[./-]\s*$YEAR(?![.:/-]?\d)""")

private const val DUE_WORDS = """MHD|M\.?H\.?D|HALTBAR|VERBRAUCHEN|BEST|BB|BBE|BBD|EXP|EXPIRY|USE BY|""" +
    """CONSOMMER|DLC|DDM|DLUO|CONSUMIR|CADUCIDAD|CAD|CONSUMARSI|SCAD|TENMINSTE|TGT|THT"""
private val DUE_KEYWORD = Regex("""(?<![A-Z])(?:$DUE_WORDS)(?![A-Z])""")
private val OTHER_DATE_KEYWORD = Regex(
    """(?<![A-Z])(?:PROD|PRODUCED|PRODUZIERT|HERGESTELLT|HERST|PKD|PACKED|VERPACKT|ABGEFÜLLT|""" +
        """MFG|MFD|FAB|FABRIQUÉ|ELAB)(?![A-Z])""",
)
private val COMPACT_AFTER_KEYWORD = Regex(
    """(?<![A-Z])(?:$DUE_WORDS)[.:\s]*(\d{2})(\d{2})(\d{4}|\d{2})(?!\d)""",
)

private val ZERO_LOOKALIKES = setOf('O', 'o', 'Q')
private val ONE_LOOKALIKES = setOf('I', 'l', '|', '!')

private const val KEYWORD_LINES_BEFORE = 2
private const val SAME_LINE_KEYWORD = 4
private const val NEARBY_KEYWORD = 2
private const val OTHER_DATE_PENALTY = 6
private const val PAST_PENALTY = 3
private const val DAYS_BEFORE_TODAY = 3 * 366L
private const val DAYS_AFTER_TODAY = 25 * 366L

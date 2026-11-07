package com.patrickzedler.grocy.core.model.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrintedDatesTest {

    private val today = epochDayOf("2026-11-07")!!

    private fun read(
        vararg lines: String,
        order: DateFieldOrder = DateFieldOrder.DAY_MONTH_YEAR,
    ): PrintedDueDate? = printedDueDateOf(lines.toList(), today, order)

    private fun day(iso: String) = epochDayOf(iso)!!

    @Test
    fun dotMatrixCanFindsDateBetweenLotCodes() {
        val date = read("06.10.2027", "L5280226", "C GRN-28222C")

        assertEquals(PrintedDueDate(day("2027-10-06")), date)
    }

    @Test
    fun juiceCartonSkipsTimeAndBatch() {
        val date = read("07.08.27 22:27 211539L11NG")

        assertEquals(PrintedDueDate(day("2027-08-07")), date)
    }

    @Test
    fun syrupBottleSkipsLotAndTime() {
        val date = read("Mindestens haltbar bis: / LOT:", "23.02.2028", "DM40552602", "12:57")

        assertEquals(PrintedDueDate(day("2028-02-23")), date)
    }

    @Test
    fun monthAndYearMeanTheMonthsLastDay() {
        val date = read(
            "OSTMANN Gewürze GmbH",
            "49198 Dissen",
            "Verbraucher-Service: 05421-30 93",
            "Mindestens haltbar",
            "bis Ende:",
            "10/2029",
            "524200",
        )

        assertEquals(PrintedDueDate(day("2029-10-31")), date)
    }

    @Test
    fun misreadLettersInNumbersAreDigits() {
        val date = read("O3.O8.2O27", "L6226 FD2 C088")

        assertEquals(PrintedDueDate(day("2027-08-03")), date)
    }

    @Test
    fun commaReadForADotStillSeparates() {
        val date = read("03,08.2027")

        assertEquals(PrintedDueDate(day("2027-08-03")), date)
    }

    @Test
    fun lotPrefixBeforeADateIsNoDigit() {
        val date = read("L06.10.2027")

        assertEquals(PrintedDueDate(day("2027-10-06")), date)
    }

    @Test
    fun slashDateOffersSwappedDayAndMonth() {
        val date = read("06/10/2027")

        assertEquals(PrintedDueDate(day("2027-10-06"), alternative = day("2027-06-10")), date)
    }

    @Test
    fun slashDateFollowsMonthFirstOrder() {
        val date = read("06/10/2027", order = DateFieldOrder.MONTH_DAY_YEAR)

        assertEquals(PrintedDueDate(day("2027-06-10"), alternative = day("2027-10-06")), date)
    }

    @Test
    fun dayAboveTwelveLeavesOneReading() {
        val date = read("10/23/2027")

        assertEquals(PrintedDueDate(day("2027-10-23")), date)
    }

    @Test
    fun sameDayAndMonthHasNoAlternative() {
        val date = read("05/05/2027")

        assertEquals(PrintedDueDate(day("2027-05-05")), date)
    }

    @Test
    fun dotsAreAlwaysDayFirst() {
        val date = read("06.10.2027", order = DateFieldOrder.MONTH_DAY_YEAR)

        assertEquals(PrintedDueDate(day("2027-10-06")), date)
    }

    @Test
    fun spacesSeparateLikeDots() {
        val date = read("06 10 2027")

        assertEquals(day("2027-10-06"), date?.epochDay)
    }

    @Test
    fun yearFirstDate() {
        val date = read("EXP 2027-10-06")

        assertEquals(PrintedDueDate(day("2027-10-06")), date)
    }

    @Test
    fun monthNameWithDay() {
        val date = read("BEST BEFORE 06 OCT 2027")

        assertEquals(PrintedDueDate(day("2027-10-06")), date)
    }

    @Test
    fun germanMonthNameWithoutDay() {
        val date = read("MHD MÄRZ 28")

        assertEquals(PrintedDueDate(day("2028-03-31")), date)
    }

    @Test
    fun wordStartingLikeAMonthIsNoMonth() {
        val date = read("MARGARINE 2027")

        assertNull(date)
    }

    @Test
    fun compactDigitsCountOnlyAfterAKeyword() {
        assertEquals(
            PrintedDueDate(day("2027-10-06"), alternative = day("2027-06-10")),
            read("EXP 061027"),
        )
        assertNull(read("061027"))
    }

    @Test
    fun yearBeforeADateIsNoYearFirstDate() {
        val date = read("2026 01.02.2027")

        assertEquals(PrintedDueDate(day("2027-02-01")), date)
    }

    @Test
    fun dueKeywordWinsOverAnotherDate() {
        val date = read("PROD 01.12.2027", "MHD 01.06.2027")

        assertEquals(PrintedDueDate(day("2027-06-01")), date)
    }

    @Test
    fun laterDateWinsWithoutKeywords() {
        val date = read("01.11.2026", "01.05.2028")

        assertEquals(PrintedDueDate(day("2028-05-01")), date)
    }

    @Test
    fun futureDateWinsOverPastOne() {
        val date = read("01.05.2026 01.02.2027")

        assertEquals(PrintedDueDate(day("2027-02-01")), date)
    }

    @Test
    fun pastDateIsStillReadAlone() {
        val date = read("01.05.2026")

        assertEquals(PrintedDueDate(day("2026-05-01")), date)
    }

    @Test
    fun datesFarFromTodayAreNoDueDates() {
        assertNull(read("01.05.1999"))
        assertNull(read("01.05.2070"))
    }

    @Test
    fun impossibleDateIsNone() {
        assertNull(read("31.02.2027"))
    }

    @Test
    fun timesPhoneNumbersAndBarcodesAreNoDates() {
        val date = read("12:57", "05421-30 93", "4 260644 046321", "35 g")

        assertNull(date)
    }

    @Test
    fun votesNeedTheSameDateTwice() {
        val votes = PrintedDueDateVotes()
        val date = PrintedDueDate(day("2027-10-06"))

        assertNull(votes.accept(date))
        assertNull(votes.accept(PrintedDueDate(day("2027-10-08"))))
        assertNull(votes.accept(null))
        assertEquals(date, votes.accept(date))
    }

    @Test
    fun votesForgetFramesOutsideTheWindow() {
        val votes = PrintedDueDateVotes(needed = 2, window = 3)
        val date = PrintedDueDate(day("2027-10-06"))

        votes.accept(date)
        votes.accept(null)
        votes.accept(null)

        assertNull(votes.accept(date))
    }

    @Test
    fun votesCountSwappedReadingsAsOneDate() {
        val votes = PrintedDueDateVotes()
        val first = PrintedDueDate(day("2027-10-06"), alternative = day("2027-06-10"))
        val swapped = PrintedDueDate(day("2027-06-10"), alternative = day("2027-10-06"))

        votes.accept(first)

        assertEquals(swapped, votes.accept(swapped))
    }

    @Test
    fun resetVotesStartOver() {
        val votes = PrintedDueDateVotes()
        val date = PrintedDueDate(day("2027-10-06"))
        votes.accept(date)

        votes.reset()

        assertNull(votes.accept(date))
    }
}

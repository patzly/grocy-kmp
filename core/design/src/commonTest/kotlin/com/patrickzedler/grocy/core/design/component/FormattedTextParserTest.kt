package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormattedTextParserTest {

    private fun parse(text: String, isDialog: Boolean = false) = parseFormattedText(text, isDialog)

    private fun paragraph(text: String, isMedium: Boolean = false, keepDistance: Boolean = true) =
        TextBlock.Paragraph(AnnotatedString(text), isMedium, keepDistance)

    @Test
    fun blankLinesSeparateParagraphs() {
        assertEquals(listOf(paragraph("First"), paragraph("Second")), parse("First\n\nSecond"))
    }

    @Test
    fun windowsLineBreaksAndExtraBlankLinesAreIgnored() {
        assertEquals(
            listOf(paragraph("First"), paragraph("Second")),
            parse("\n\nFirst\r\n\r\n\r\n  \nSecond\n\n"),
        )
    }

    @Test
    fun singleLineBreakStaysInsideParagraph() {
        assertEquals(listOf(paragraph("Line one\nLine two")), parse("Line one\nLine two"))
    }

    @Test
    fun blankTextHasNoBlocks() {
        assertEquals(emptyList(), parse("\n\n  \n"))
    }

    @Test
    fun headlineLevelFollowsNumberOfHashes() {
        val blocks = parse("# One\n\n## Two\n\n#### Four")

        assertEquals(listOf(1, 2, 4), blocks.map { (it as TextBlock.Headline).level })
        assertEquals("Two", (blocks[1] as TextBlock.Headline).annotatedText.text)
    }

    @Test
    fun underscoreAfterHashesEnablesTabularNumbers() {
        val headline = parse("###_ 1.0.0").single() as TextBlock.Headline

        assertEquals(3, headline.level)
        assertTrue(headline.useTNum)
        assertEquals("1.0.0", headline.annotatedText.text)
    }

    @Test
    fun headlineDirectlyFollowedByLinkKeepsNoDistance() {
        val blocks = parse("# Links\n\n=> GitHub https://github.com")

        assertFalse((blocks[0] as TextBlock.Headline).keepDistance)
    }

    @Test
    fun subtitleIsMediumAndPreviousParagraphKeepsNoDistance() {
        assertEquals(
            listOf(
                paragraph("Intro", keepDistance = false),
                paragraph("What changes?", isMedium = true),
            ),
            parse("Intro\n\n__What changes?"),
        )
    }

    @Test
    fun dialogParagraphsAreMediumAndAlwaysKeepDistance() {
        assertEquals(
            listOf(
                paragraph("Intro", isMedium = true),
                paragraph("Subtitle", isMedium = true),
            ),
            parse("Intro\n\n__Subtitle", isDialog = true),
        )
    }

    @Test
    fun bulletListJoinsContinuationLines() {
        val list = parse("- First item\n  continues here\n- Second item").single()

        assertEquals(
            listOf("First item continues here", "Second item"),
            (list as TextBlock.BulletList).bullets.map { it.annotatedText.text },
        )
    }

    @Test
    fun linkWithLabelAndUrl() {
        assertEquals(
            TextBlock.Link(text = "Source code", url = "https://github.com/patzly/grocy-kmp"),
            parse("=> Source code https://github.com/patzly/grocy-kmp").single(),
        )
    }

    @Test
    fun linkWithoutSchemeGetsHttps() {
        assertEquals(
            TextBlock.Link(text = "Website", url = "https://patrickzedler.com"),
            parse("> Website patrickzedler.com").single(),
        )
        assertEquals(
            TextBlock.Link(text = "www.example.com", url = "https://www.example.com"),
            parse("> www.example.com").single(),
        )
    }

    @Test
    fun mailAndPhoneLinksKeepTheirScheme() {
        assertEquals(
            TextBlock.Link(text = "Mail", url = "mailto:dev@example.com"),
            parse("=> Mail mailto:dev@example.com").single(),
        )
        assertEquals(
            TextBlock.Link(text = "Call", url = "tel:+41000000000"),
            parse("=> Call tel:+41000000000").single(),
        )
    }

    @Test
    fun messageCardsAndDivider() {
        val blocks = parse("? Note\n\n! Warning\n\n---")

        assertEquals(
            listOf(
                card("Note", isError = false),
                card("Warning", isError = true),
                TextBlock.Divider,
            ),
            blocks,
        )
    }

    @Test
    fun messageCardsInDialogsUseTheParagraphSize() {
        val blocks = parse("? Note\n\n! Warning", isDialog = true)

        assertEquals(
            listOf(
                card("Note", isError = false, isMedium = true),
                card("Warning", isError = true, isMedium = true),
            ),
            blocks,
        )
    }

    @Test
    fun dashAfterTheCardMarkerMakesTheTextSmall() {
        val blocks = parse("?- Footnote\n\n!- Fine print")

        assertEquals(
            listOf(
                card("Footnote", isError = false, isSmall = true),
                card("Fine print", isError = true, isSmall = true),
            ),
            blocks,
        )
    }

    private fun card(
        text: String,
        isError: Boolean,
        isMedium: Boolean = false,
        isSmall: Boolean = false,
    ) = TextBlock.MessageCard(AnnotatedString(text), isError, isMedium, isSmall)

    @Test
    fun boldMarkersBecomeBoldSpans() {
        val text = parseAnnotatedText("Use **two** or <b>tag</b> markers")

        assertEquals("Use two or tag markers", text.text)
        assertEquals(
            listOf(4 to 7, 11 to 14),
            text.spanStyles.map { it.start to it.end },
        )
        assertTrue(text.spanStyles.all { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun boldSpanCanCrossLineBreaks() {
        val text = parseAnnotatedText("**first\nsecond** end")

        assertEquals("first\nsecond end", text.text)
        assertEquals(0 to 12, text.spanStyles.single().let { it.start to it.end })
    }

    @Test
    fun textWithoutMarkersHasNoSpans() {
        assertTrue(parseAnnotatedText("Plain * text").spanStyles.isEmpty())
    }
}

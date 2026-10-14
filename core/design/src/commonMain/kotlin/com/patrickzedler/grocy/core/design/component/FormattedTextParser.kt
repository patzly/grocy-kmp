package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

// [\s\S] instead of DOT_MATCHES_ALL, which only exists on the JVM
private val BOLD_REGEX = """<b>([\s\S]*?)</b>|\*\*([\s\S]*?)\*\*""".toRegex()

private val DOMAIN_URL_REGEX = """^([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(/.*)?$""".toRegex()

// Blank lines may contain spaces, more than one in a row still separates only once
private val PARAGRAPH_SEPARATOR_REGEX = """\n[ \t]*\n\s*""".toRegex()

internal sealed interface TextBlock {
    data class Headline(
        val annotatedText: AnnotatedString,
        val level: Int,
        val useTNum: Boolean,
        val keepDistance: Boolean,
    ) : TextBlock

    data class Paragraph(
        val annotatedText: AnnotatedString,
        val isMedium: Boolean,
        val keepDistance: Boolean,
    ) : TextBlock

    data class BulletItem(
        val annotatedText: AnnotatedString,
    )

    data class BulletList(
        val bullets: List<BulletItem>,
    ) : TextBlock

    data class Link(
        val text: String,
        val url: String,
    ) : TextBlock

    data class MessageCard(
        val annotatedText: AnnotatedString,
        val isError: Boolean,
    ) : TextBlock

    data object Divider : TextBlock
}

/** Splits the markup of [FormattedText] into blocks, one per paragraph. */
internal fun parseFormattedText(text: String, isDialog: Boolean): List<TextBlock> {
    val parts = text
        .replace("\r\n", "\n")
        .trim('\n')
        .split(PARAGRAPH_SEPARATOR_REGEX)
        .filter { it.isNotBlank() }

    return parts.mapIndexed { i, p ->
        val partNext = parts.getOrNull(i + 1).orEmpty()
        val isNextLink = partNext.startsWith("=> ") || partNext.startsWith("> ")
        val isNextSubtitle = partNext.startsWith("__")

        when {
            p.startsWith("__") -> TextBlock.Paragraph(
                annotatedText = parseAnnotatedText(p.substring(2).trimStart()),
                isMedium = true,
                keepDistance = true,
            )

            p.startsWith("#") -> parseHeadline(p, keepDistance = !isNextLink)
            p.startsWith("- ") -> parseBulletList(p)
            p.startsWith("> ") || p.startsWith("=> ") -> parseLink(p)

            p.startsWith("? ") -> TextBlock.MessageCard(
                annotatedText = parseAnnotatedText(p.substring(2)),
                isError = false,
            )

            p.startsWith("! ") -> TextBlock.MessageCard(
                annotatedText = parseAnnotatedText(p.substring(2)),
                isError = true,
            )

            p.startsWith("---") -> TextBlock.Divider

            else -> TextBlock.Paragraph(
                annotatedText = parseAnnotatedText(p),
                isMedium = isDialog,
                keepDistance = isDialog || (!isNextLink && !isNextSubtitle),
            )
        }
    }
}

private fun parseHeadline(p: String, keepDistance: Boolean): TextBlock.Headline {
    val firstSpace = p.indexOf(' ')
    val prefix = if (firstSpace != -1) p.substring(0, firstSpace) else p
    val useTNum = prefix.endsWith("_")
    val level = if (useTNum) prefix.length - 1 else prefix.length
    val headline = if (firstSpace != -1) p.substring(firstSpace + 1) else ""

    return TextBlock.Headline(
        annotatedText = parseAnnotatedText(headline),
        level = level,
        useTNum = useTNum,
        keepDistance = keepDistance,
    )
}

private fun parseBulletList(p: String): TextBlock.BulletList {
    val bulletStrings = mutableListOf<String>()
    p.lines().forEach { line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("- ")) {
            bulletStrings.add(trimmed.removePrefix("- ").trim())
        } else if (trimmed.isNotEmpty() && bulletStrings.isNotEmpty()) {
            // Continuation line of the previous bullet
            bulletStrings[bulletStrings.lastIndex] = "${bulletStrings.last()} $trimmed"
        }
    }
    return TextBlock.BulletList(
        bulletStrings.map { TextBlock.BulletItem(annotatedText = parseAnnotatedText(it)) },
    )
}

private fun parseLink(p: String): TextBlock.Link {
    val raw = p.removePrefix("=> ").removePrefix("> ").trim()
    val lastSpace = raw.lastIndexOf(' ')
    val (label, rawUrl) = if (lastSpace != -1) {
        val potentialUrl = raw.substring(lastSpace + 1).trim()
        val potentialLabel = raw.substring(0, lastSpace).trim()
        if (potentialUrl.isUrl()) potentialLabel to potentialUrl else raw to raw
    } else {
        raw to raw
    }

    val hasScheme = rawUrl.contains("://") ||
        rawUrl.startsWith("mailto:") ||
        rawUrl.startsWith("tel:")
    return TextBlock.Link(text = label, url = if (hasScheme) rawUrl else "https://$rawUrl")
}

private fun String.isUrl(): Boolean =
    contains("://") ||
        startsWith("mailto:") ||
        startsWith("tel:") ||
        startsWith("www.") ||
        DOMAIN_URL_REGEX.matches(this)

/** Applies bold to text wrapped in `**` or `<b>` tags. */
internal fun parseAnnotatedText(text: String): AnnotatedString {
    if (!BOLD_REGEX.containsMatchIn(text)) return AnnotatedString(text)

    return buildAnnotatedString {
        var currentIndex = 0
        for (match in BOLD_REGEX.findAll(text)) {
            append(text.substring(currentIndex, match.range.first))
            val boldContent = match.groups[1]?.value ?: match.groups[2]?.value.orEmpty()
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(boldContent)
            }
            currentIndex = match.range.last + 1
        }
        append(text.substring(currentIndex))
    }
}

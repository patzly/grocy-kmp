package com.patrickzedler.grocy.core.forms

/** Grocy keeps descriptions as HTML; the form edits them as plain text. */
fun htmlToPlainText(html: String): String {
    if (html.isBlank()) return ""
    val text = html
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</(p|div|li|h[1-6])>"), "\n\n")
        .replace(Regex("(?i)<li[^>]*>"), "• ")
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
    return text.replace(Regex("\n{3,}"), "\n\n").trim()
}

/** Plain text as HTML paragraphs for Grocy's description, line breaks kept. */
fun plainTextToHtml(text: String): String {
    if (text.isBlank()) return ""
    return text.trim()
        .split(Regex("\n\\s*\n"))
        .joinToString("") { paragraph ->
            val escaped = paragraph.trim()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>")
            "<p>$escaped</p>"
        }
}

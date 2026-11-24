package com.patrickzedler.grocy.apps.shared

/**
 * The address of a recipe page another app shared: browsers share the address alone, recipe
 * apps often "Title https://…" or a whole sentence around it. The first http(s) address counts,
 * without the punctuation of the sentence it ends.
 */
internal fun sharedRecipeLinkOf(text: String?): String? =
    text?.let { LINK.find(it)?.value?.trimEnd(*TRAILING_PUNCTUATION) }
        ?.takeIf { it.substringAfter("://").isNotEmpty() }

private val LINK = Regex("https?://[^\\s<>\"]+", RegexOption.IGNORE_CASE)
private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '\'')

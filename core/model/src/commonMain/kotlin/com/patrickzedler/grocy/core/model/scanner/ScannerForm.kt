package com.patrickzedler.grocy.core.model.scanner

/** A form that opens with its scanner when the user used the scanner there last. */
enum class ScannerForm {
    PURCHASE,
    CONSUME,
    TRANSFER,
    INVENTORY,
    SHOPPING_LIST_ITEM,
    BARCODE,
    CHORE_TRACKING,
    ;

    companion object {
        /** The forms of a stored text; unknown names of other app versions are left out. */
        fun of(stored: String?): Set<ScannerForm> {
            val names = stored.orEmpty().split(',')
            return entries.filterTo(mutableSetOf()) { it.name in names }
        }

        fun stored(forms: Set<ScannerForm>): String =
            forms.sortedBy { it.ordinal }.joinToString(",") { it.name }
    }
}

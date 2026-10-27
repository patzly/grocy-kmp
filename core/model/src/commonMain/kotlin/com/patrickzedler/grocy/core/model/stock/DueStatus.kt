package com.patrickzedler.grocy.core.model.stock

import com.patrickzedler.grocy.core.model.masterdata.DueType

/** How Grocy highlights stock by its due date. */
enum class DueStatus {
    /** Due within the user's "Due soon days". */
    DUE_SOON,

    /** A best before date in the past: still usable, Grocy greys it. */
    OVERDUE,

    /** An expiration date in the past: Grocy shows it red. */
    EXPIRED,
}

/**
 * Grocy's highlighting: a due date before today is overdue (best before) or expired
 * (expiration date), one within the next [dueSoonDays] days due soon.
 */
fun dueStatusOf(dueDay: Long, today: Long, dueSoonDays: Int, dueType: DueType): DueStatus? =
    when {
        dueDay < today ->
            if (dueType == DueType.EXPIRATION) DueStatus.EXPIRED else DueStatus.OVERDUE
        dueDay <= today + dueSoonDays -> DueStatus.DUE_SOON
        else -> null
    }

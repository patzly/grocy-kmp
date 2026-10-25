package com.patrickzedler.grocy.core.model.server

/** A value of one of Grocy's user settings, as its settings pages save it. */
sealed interface UserSettingValue {
    /** A checkbox. */
    data class Flag(val value: Boolean) : UserSettingValue

    /** A whole number, also the ids of presets (-1 for none). */
    data class Whole(val value: Long) : UserSettingValue

    /** A number with decimal places, e.g. an amount. */
    data class Number(val value: Double) : UserSettingValue
}

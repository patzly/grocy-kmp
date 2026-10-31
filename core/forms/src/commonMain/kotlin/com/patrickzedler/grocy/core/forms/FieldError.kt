package com.patrickzedler.grocy.core.forms

/** Why a form input is invalid, shown below the field like Grocy's validation messages. */
sealed interface FieldError {
    data object Required : FieldError

    /** Grocy's number message: at least [min], at most [decimals] decimal places. */
    data class InvalidNumber(val min: String, val decimals: Int) : FieldError

    /** Not a date and time as "YYYY-MM-DD HH:MM:SS". */
    data object InvalidDateTime : FieldError

    /** Not a date as "YYYY-MM-DD". */
    data object InvalidDate : FieldError
}

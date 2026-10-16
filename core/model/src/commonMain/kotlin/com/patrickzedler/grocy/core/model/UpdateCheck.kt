package com.patrickzedler.grocy.core.model

/** Result of looking for a newer version of the app. */
sealed interface UpdateCheck {

    data object UpToDate : UpdateCheck

    /** [versionName] is null where the source does not tell it, e.g. Google Play. */
    data class Available(val versionName: String?) : UpdateCheck

    /** No connection or no answer the app understands. */
    data object Failed : UpdateCheck
}

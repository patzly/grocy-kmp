package com.patrickzedler.grocy.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Base contract for all destinations, implemented in the feature api modules. The `@SerialName`
 * of a route is its URL path on the web ("settings/general"), and the back stack is kept in the
 * browser history, so routes must never carry secrets.
 */
interface Route : NavKey

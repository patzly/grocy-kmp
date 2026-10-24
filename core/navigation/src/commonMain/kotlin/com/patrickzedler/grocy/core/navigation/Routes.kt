package com.patrickzedler.grocy.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Base contract for all destinations, implemented in the feature api modules. The `@SerialName`
 * of a route is its URL path on the web ("settings/general"), and the back stack is kept in the
 * browser history, so routes must never carry secrets.
 */
interface Route : NavKey

/**
 * A route shown as detail pane beside its list where the window allows two panes; opened with
 * [Navigator.openDetail], so choosing another item replaces it instead of piling up.
 */
interface DetailRoute : Route

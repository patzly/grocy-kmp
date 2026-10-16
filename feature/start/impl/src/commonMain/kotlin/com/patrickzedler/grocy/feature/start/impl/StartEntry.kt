package com.patrickzedler.grocy.feature.start.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute

fun EntryProviderScope<NavKey>.startEntry(
    navigator: Navigator,
) {
    entry<StartRoute> {
        StartScreen(
            onSettingsClick = { navigator.navigate(SettingsRoute) },
            onAboutClick = { navigator.navigate(AboutRoute) },
        )
    }
}

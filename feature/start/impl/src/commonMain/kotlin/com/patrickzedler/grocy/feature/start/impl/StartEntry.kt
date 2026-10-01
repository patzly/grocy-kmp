package com.patrickzedler.grocy.feature.start.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.feature.start.api.StartRoute

fun EntryProviderScope<NavKey>.startEntry() {
    entry<StartRoute> {
        StartScreen()
    }
}
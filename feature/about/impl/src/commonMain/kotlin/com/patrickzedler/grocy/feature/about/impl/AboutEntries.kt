package com.patrickzedler.grocy.feature.about.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.feature.about.api.AboutRoute

fun EntryProviderScope<NavKey>.aboutEntries(
    dependencies: AboutDependencies,
    navigator: Navigator,
) {
    entry<AboutRoute> {
        AboutScreen(
            viewModel = viewModel {
                AboutViewModel(dependencies.unlockRepository, dependencies.updateRepository)
            },
            versionName = dependencies.versionName,
            onBackClick = navigator::goBack,
        )
    }
}

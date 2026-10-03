package com.patrickzedler.grocy.feature.login.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.feature.login.api.LoginChoiceRoute
import com.patrickzedler.grocy.feature.login.impl.choice.LoginChoiceScreen
import com.patrickzedler.grocy.feature.login.impl.choice.LoginChoiceViewModel

fun EntryProviderScope<NavKey>.loginEntries(
    dependencies: LoginDependencies,
) {
    entry<LoginChoiceRoute> {
        LoginChoiceScreen(
            viewModel = viewModel { LoginChoiceViewModel(dependencies.authRepository) },
        )
    }
}

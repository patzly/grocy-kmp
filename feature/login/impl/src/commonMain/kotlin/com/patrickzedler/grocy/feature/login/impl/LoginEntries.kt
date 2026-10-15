package com.patrickzedler.grocy.feature.login.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.feature.login.api.LoginChoiceRoute
import com.patrickzedler.grocy.feature.login.api.LoginManualRoute
import com.patrickzedler.grocy.feature.login.api.LoginScanRoute
import com.patrickzedler.grocy.feature.login.impl.choice.LoginChoiceScreen
import com.patrickzedler.grocy.feature.login.impl.choice.LoginChoiceViewModel
import com.patrickzedler.grocy.feature.login.impl.manual.LoginManualScreen
import com.patrickzedler.grocy.feature.login.impl.manual.LoginManualViewModel
import com.patrickzedler.grocy.feature.login.impl.scan.LoginScanScreen
import com.patrickzedler.grocy.feature.login.impl.scan.LoginScanViewModel

fun EntryProviderScope<NavKey>.loginEntries(
    dependencies: LoginDependencies,
    navigator: Navigator,
) {
    entry<LoginChoiceRoute> {
        LoginChoiceScreen(
            viewModel = viewModel { LoginChoiceViewModel(dependencies.authRepository) },
            onOwnServerClick = { navigator.navigate(LoginScanRoute) },
        )
    }
    entry<LoginScanRoute> {
        LoginScanScreen(
            viewModel = viewModel { LoginScanViewModel(dependencies.authRepository) },
            onBackClick = navigator::goBack,
            onManualEntryClick = { navigator.navigate(LoginManualRoute) },
        )
    }
    entry<LoginManualRoute> {
        LoginManualScreen(
            viewModel = viewModel { LoginManualViewModel(dependencies.authRepository) },
            onBackClick = navigator::goBack,
        )
    }
}

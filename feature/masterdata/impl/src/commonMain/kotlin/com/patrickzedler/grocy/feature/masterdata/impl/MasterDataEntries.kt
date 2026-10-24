package com.patrickzedler.grocy.feature.masterdata.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.ui.AppMenuDialogs
import com.patrickzedler.grocy.core.ui.rememberAppMenu
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.masterdata.api.MasterDataRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductsRoute
import com.patrickzedler.grocy.feature.masterdata.impl.product.ProductScreen
import com.patrickzedler.grocy.feature.masterdata.impl.product.ProductViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.products.ProductsScreen
import com.patrickzedler.grocy.feature.masterdata.impl.products.ProductsViewModel
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute

fun EntryProviderScope<NavKey>.masterDataEntries(
    dependencies: MasterDataDependencies,
    navigator: Navigator,
) {
    entry<MasterDataRoute> {
        MasterDataScreen(
            menuItems = appMenuItems(navigator),
            viewModel = viewModel { MasterDataViewModel(dependencies.masterDataRepository) },
            onBackClick = navigator::goBack,
            onCategoryClick = { category ->
                // The other categories follow one by one
                when (category) {
                    MasterDataCategory.PRODUCTS -> navigator.navigate(ProductsRoute)
                    else -> Unit
                }
            },
        )
    }

    entry<ProductsRoute> {
        ProductsScreen(
            menuItems = appMenuItems(navigator),
            viewModel = viewModel { ProductsViewModel(dependencies.masterDataRepository) },
            onBackClick = navigator::goBack,
            onProductClick = { id -> navigator.navigate(ProductRoute(id)) },
            onAddClick = { navigator.navigate(ProductRoute()) },
        )
    }

    entry<ProductRoute> { route ->
        ProductScreen(
            menuItems = appMenuItems(navigator),
            viewModel = viewModel {
                ProductViewModel(route.productId, dependencies.masterDataRepository)
            },
            onBackClick = navigator::goBack,
        )
    }
}

/** The menu of the app's screens with its dialogs, leading to the settings and About. */
@Composable
private fun appMenuItems(navigator: Navigator): List<AppBarMenuItem> {
    val menu = rememberAppMenu(
        onSettingsClick = { navigator.navigate(SettingsRoute) },
        onAboutClick = { navigator.navigate(AboutRoute) },
    )
    AppMenuDialogs(menu)
    return menu.items
}

package com.patrickzedler.grocy.feature.masterdata.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.feature.masterdata.api.MasterDataRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductsRoute
import com.patrickzedler.grocy.feature.masterdata.impl.product.ProductScreen
import com.patrickzedler.grocy.feature.masterdata.impl.product.ProductViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.products.ProductsScreen
import com.patrickzedler.grocy.feature.masterdata.impl.products.ProductsViewModel

fun EntryProviderScope<NavKey>.masterDataEntries(
    dependencies: MasterDataDependencies,
    navigator: Navigator,
) {
    entry<MasterDataRoute> {
        MasterDataScreen(
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
            viewModel = viewModel { ProductsViewModel(dependencies.masterDataRepository) },
            onBackClick = navigator::goBack,
            onProductClick = { id -> navigator.navigate(ProductRoute(id)) },
            onAddClick = { navigator.navigate(ProductRoute()) },
        )
    }

    entry<ProductRoute> { route ->
        ProductScreen(
            viewModel = viewModel {
                ProductViewModel(route.productId, dependencies.masterDataRepository)
            },
            onBackClick = navigator::goBack,
        )
    }
}

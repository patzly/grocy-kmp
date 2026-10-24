package com.patrickzedler.grocy.feature.masterdata.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.DetailPlaceholder
import com.patrickzedler.grocy.core.model.masterdata.grocyPluralFormCount
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.Route
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.rememberLeaveGuard
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.objects_select_hint
import com.patrickzedler.grocy.core.ui.AppMenuDialogs
import com.patrickzedler.grocy.core.ui.rememberAppMenu
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.masterdata.api.BatteriesRoute
import com.patrickzedler.grocy.feature.masterdata.api.BatteryRoute
import com.patrickzedler.grocy.feature.masterdata.api.ChoreRoute
import com.patrickzedler.grocy.feature.masterdata.api.ChoresRoute
import com.patrickzedler.grocy.feature.masterdata.api.LocationRoute
import com.patrickzedler.grocy.feature.masterdata.api.LocationsRoute
import com.patrickzedler.grocy.feature.masterdata.api.MasterDataRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductGroupRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductGroupsRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductsRoute
import com.patrickzedler.grocy.feature.masterdata.api.QuantityUnitRoute
import com.patrickzedler.grocy.feature.masterdata.api.QuantityUnitsRoute
import com.patrickzedler.grocy.feature.masterdata.api.StoreRoute
import com.patrickzedler.grocy.feature.masterdata.api.StoresRoute
import com.patrickzedler.grocy.feature.masterdata.api.TaskCategoriesRoute
import com.patrickzedler.grocy.feature.masterdata.api.TaskCategoryRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserentitiesRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserentityRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserfieldRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserfieldsRoute
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ChoreScreen
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ChoreViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.objects.FormFrame
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectActionEvent
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectActionsViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectListKind
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectListScreen
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectListViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.objects.SimpleObjectScreen
import com.patrickzedler.grocy.feature.masterdata.impl.objects.SimpleObjectViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.objects.UserentityScreen
import com.patrickzedler.grocy.feature.masterdata.impl.objects.UserentityViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.objects.UserfieldScreen
import com.patrickzedler.grocy.feature.masterdata.impl.objects.UserfieldViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.objects.entity
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductOverviewViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.product.ProductScreen
import com.patrickzedler.grocy.feature.masterdata.impl.product.ProductViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.products.ProductsScreen
import com.patrickzedler.grocy.feature.masterdata.impl.products.ProductsViewModel
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun EntryProviderScope<NavKey>.masterDataEntries(
    dependencies: MasterDataDependencies,
    navigator: Navigator,
) {
    val repository = dependencies.masterDataRepository
    // Opens the userfields of an entity, like Grocy's "Configure userfields"
    val openUserfields = { entity: String -> navigator.navigate(UserfieldsRoute(entity)) }

    entry<MasterDataRoute> {
        MasterDataScreen(
            menuItems = appMenuItems(navigator),
            viewModel = viewModel { MasterDataViewModel(repository, dependencies.serverConfig) },
            onBackClick = navigator::goBack,
            onCategoryClick = { category -> navigator.navigate(category.route) },
        )
    }

    entry<ProductsRoute>(
        metadata = listPaneMetadata(ProductsRoute) { SelectHint(MasterDataCategory.PRODUCTS) },
    ) { route ->
        ProductsScreen(
            menuItems = appMenuItems(navigator),
            viewModel = viewModel { ProductsViewModel(repository) },
            overviewViewModel = viewModel { ProductOverviewViewModel(repository) },
            objectActions = viewModel {
                ObjectActionsViewModel(MasterDataRepository.PRODUCTS, repository)
            },
            selectedId = (navigator.currentRoute as? ProductRoute)?.productId,
            isBesideDetail = showsDetailBesideList(),
            onBackClick = { navigator.goBack(from = route) },
            onProductClick = { id -> navigator.openDetail(ProductRoute(id)) },
            onAddClick = { navigator.openDetail(ProductRoute()) },
            onConfigureUserfieldsClick = { openUserfields(MasterDataRepository.PRODUCTS) },
            onActionDone = listActionDone(navigator, ::ProductRoute) {
                (it as? ProductRoute)?.productId
            },
        )
    }

    entry<ProductRoute>(metadata = detailPaneMetadata(ProductsRoute)) { route ->
        ProductScreen(
            viewModel = viewModel { ProductViewModel(route.productId, repository) },
            frame = formFrame(navigator, route, MasterDataRepository.PRODUCTS, repository) {
                ProductRoute(it)
            },
        )
    }

    simpleObjectEntries<LocationsRoute, LocationRoute>(
        kind = ObjectListKind.LOCATIONS,
        detail = ::LocationRoute,
        detailId = LocationRoute::id,
        repository = repository,
        navigator = navigator,
    )
    simpleObjectEntries<StoresRoute, StoreRoute>(
        kind = ObjectListKind.STORES,
        detail = ::StoreRoute,
        detailId = StoreRoute::id,
        repository = repository,
        navigator = navigator,
    )
    simpleObjectEntries<QuantityUnitsRoute, QuantityUnitRoute>(
        kind = ObjectListKind.QUANTITY_UNITS,
        detail = ::QuantityUnitRoute,
        detailId = QuantityUnitRoute::id,
        repository = repository,
        navigator = navigator,
    )
    simpleObjectEntries<ProductGroupsRoute, ProductGroupRoute>(
        kind = ObjectListKind.PRODUCT_GROUPS,
        detail = ::ProductGroupRoute,
        detailId = ProductGroupRoute::id,
        repository = repository,
        navigator = navigator,
    )
    simpleObjectEntries<BatteriesRoute, BatteryRoute>(
        kind = ObjectListKind.BATTERIES,
        detail = ::BatteryRoute,
        detailId = BatteryRoute::id,
        repository = repository,
        navigator = navigator,
    )
    simpleObjectEntries<TaskCategoriesRoute, TaskCategoryRoute>(
        kind = ObjectListKind.TASK_CATEGORIES,
        detail = ::TaskCategoryRoute,
        detailId = TaskCategoryRoute::id,
        repository = repository,
        navigator = navigator,
    )

    objectListEntry<ChoresRoute, ChoreRoute>(
        kind = ObjectListKind.CHORES,
        detail = ::ChoreRoute,
        detailId = ChoreRoute::id,
        repository = repository,
        navigator = navigator,
    )
    entry<ChoreRoute>(metadata = detailPaneMetadata(ObjectListKind.CHORES)) { route ->
        ChoreScreen(
            viewModel = viewModel { ChoreViewModel(route.id, repository) },
            frame = formFrame(navigator, route, MasterDataRepository.CHORES, repository) {
                ChoreRoute(it)
            },
            onConfigureUserfieldsClick = openUserfields,
        )
    }

    entry<UserfieldsRoute>(
        metadata = listPaneMetadata(ObjectListKind.USERFIELDS) {
            SelectHint(MasterDataCategory.USERFIELDS)
        },
    ) { route ->
        ObjectListScreen(
            menuItems = appMenuItems(navigator),
            viewModel = viewModel {
                ObjectListViewModel(ObjectListKind.USERFIELDS, repository, entity = route.entity)
            },
            objectActions = viewModel {
                ObjectActionsViewModel(MasterDataRepository.USERFIELDS, repository)
            },
            selectedId = (navigator.currentRoute as? UserfieldRoute)?.id,
            isBesideDetail = showsDetailBesideList(),
            onBackClick = { navigator.goBack(from = route) },
            onObjectClick = { id -> navigator.openDetail(UserfieldRoute(id)) },
            // A new userfield belongs to the filtered entity, like in Grocy
            onAddClick = { navigator.openDetail(UserfieldRoute(entity = route.entity)) },
            onConfigureUserfieldsClick = openUserfields,
            onActionDone = listActionDone(navigator, { UserfieldRoute(it) }) {
                (it as? UserfieldRoute)?.id
            },
        )
    }
    entry<UserfieldRoute>(metadata = detailPaneMetadata(ObjectListKind.USERFIELDS)) { route ->
        UserfieldScreen(
            viewModel = viewModel { UserfieldViewModel(route.id, route.entity, repository) },
            frame = formFrame(navigator, route, MasterDataRepository.USERFIELDS, repository) {
                UserfieldRoute(it)
            },
        )
    }

    objectListEntry<UserentitiesRoute, UserentityRoute>(
        kind = ObjectListKind.USERENTITIES,
        detail = ::UserentityRoute,
        detailId = UserentityRoute::id,
        repository = repository,
        navigator = navigator,
    )
    entry<UserentityRoute>(metadata = detailPaneMetadata(ObjectListKind.USERENTITIES)) { route ->
        UserentityScreen(
            viewModel = viewModel { UserentityViewModel(route.id, repository) },
            frame = formFrame(navigator, route, MasterDataRepository.USERENTITIES, repository) {
                UserentityRoute(it)
            },
            onConfigureFieldsClick = openUserfields,
        )
    }
}

/** List and form of a location, store, quantity unit, product group, battery or category. */
private inline fun <reified L : Route, reified D : DetailRoute> EntryProviderScope<NavKey>
    .simpleObjectEntries(
    kind: ObjectListKind,
    noinline detail: (id: Int?) -> D,
    noinline detailId: (D) -> Int?,
    repository: MasterDataRepository,
    navigator: Navigator,
) {
    objectListEntry<L, D>(kind, detail, detailId, repository, navigator)
    entry<D>(metadata = detailPaneMetadata(kind)) { route ->
        // Grocy asks for a unit's plural forms in languages with more than two
        val pluralFormCount = grocyPluralFormCount(Locale.current.toLanguageTag())
        SimpleObjectScreen(
            viewModel = viewModel {
                SimpleObjectViewModel(kind, detailId(route), repository, pluralFormCount)
            },
            frame = formFrame(navigator, route, kind.entity, repository, detail),
            onConfigureUserfieldsClick = { navigator.navigate(UserfieldsRoute(it)) },
        )
    }
}

/** The list of [kind], whose objects open as [D] beside it where the window allows. */
private inline fun <reified L : Route, reified D : DetailRoute> EntryProviderScope<NavKey>
    .objectListEntry(
    kind: ObjectListKind,
    noinline detail: (id: Int?) -> D,
    noinline detailId: (D) -> Int?,
    repository: MasterDataRepository,
    navigator: Navigator,
) {
    entry<L>(metadata = listPaneMetadata(kind) { SelectHint(kind.category) }) { route ->
        ObjectListScreen(
            menuItems = appMenuItems(navigator),
            viewModel = viewModel { ObjectListViewModel(kind, repository) },
            objectActions = viewModel { ObjectActionsViewModel(kind.entity, repository) },
            selectedId = (navigator.currentRoute as? D)?.let(detailId),
            isBesideDetail = showsDetailBesideList(),
            onBackClick = { navigator.goBack(from = route) },
            onObjectClick = { id -> navigator.openDetail(detail(id)) },
            onAddClick = { navigator.openDetail(detail(null)) },
            onConfigureUserfieldsClick = { navigator.navigate(UserfieldsRoute(it)) },
            onActionDone = listActionDone(navigator, detail) { (it as? D)?.let(detailId) },
        )
    }
}

/**
 * After an action in a list item's menu: closes the details of an object that is gone (deleted,
 * or merged into another), opens a copy like Grocy.
 *
 * @param shownId the object of the route shown on top, null for other routes.
 */
private fun listActionDone(
    navigator: Navigator,
    detail: (id: Int) -> DetailRoute,
    shownId: (NavKey) -> Int?,
): (ObjectActionEvent) -> Unit = { event ->
    val shown = shownId(navigator.currentRoute)
    when (event) {
        is ObjectActionEvent.Deleted -> if (shown == event.id) navigator.goBack()
        is ObjectActionEvent.Merged -> if (shown == event.removeId) navigator.goBack()
        is ObjectActionEvent.Copied -> navigator.openDetail(detail(event.id))
        else -> Unit
    }
}

/**
 * The frame of the form of [route] (an object of [entity]): the app menu without the settings
 * and About, whose place the actions on the object take, and the list beside it. A copy opens
 * as [detail] in its place.
 */
@Composable
private fun formFrame(
    navigator: Navigator,
    route: DetailRoute,
    entity: String,
    repository: MasterDataRepository,
    detail: (id: Int) -> DetailRoute,
): FormFrame {
    val menu = rememberAppMenu()
    AppMenuDialogs(menu)
    return FormFrame(
        menuItems = menu.items,
        leaveGuard = navigator.rememberLeaveGuard(route),
        isBesideList = showsDetailBesideList(),
        objectActions = viewModel { ObjectActionsViewModel(entity, repository) },
        onBackClick = navigator::goBack,
        onActionDone = { event ->
            if (event is ObjectActionEvent.Copied) navigator.openDetail(detail(event.id))
        },
    )
}

/** The detail pane beside a list until an item is chosen. */
@Composable
private fun SelectHint(category: MasterDataCategory) {
    DetailPlaceholder(
        icon = painterResource(category.icon),
        text = stringResource(Res.string.objects_select_hint),
    )
}

/** The list a category of Grocy's "Manage master data" menu opens. */
private val MasterDataCategory.route: Route
    get() = when (this) {
        MasterDataCategory.PRODUCTS -> ProductsRoute
        MasterDataCategory.LOCATIONS -> LocationsRoute
        MasterDataCategory.STORES -> StoresRoute
        MasterDataCategory.QUANTITY_UNITS -> QuantityUnitsRoute
        MasterDataCategory.PRODUCT_GROUPS -> ProductGroupsRoute
        MasterDataCategory.CHORES -> ChoresRoute
        MasterDataCategory.BATTERIES -> BatteriesRoute
        MasterDataCategory.TASK_CATEGORIES -> TaskCategoriesRoute
        MasterDataCategory.USERFIELDS -> UserfieldsRoute()
        MasterDataCategory.USERENTITIES -> UserentitiesRoute
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

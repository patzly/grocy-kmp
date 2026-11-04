package com.patrickzedler.grocy.feature.batteries.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.design.component.DetailPlaceholder
import com.patrickzedler.grocy.core.forms.fieldState
import com.patrickzedler.grocy.core.model.scanner.ScannerForm
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.batteries_detail_hint
import com.patrickzedler.grocy.core.resources.ic_rounded_battery_charging_full
import com.patrickzedler.grocy.core.ui.appMenuItems
import com.patrickzedler.grocy.feature.batteries.api.BatteriesJournalRoute
import com.patrickzedler.grocy.feature.batteries.api.BatteriesOverviewRoute
import com.patrickzedler.grocy.feature.batteries.api.BatteryTrackingRoute
import com.patrickzedler.grocy.feature.masterdata.api.BatteriesRoute
import com.patrickzedler.grocy.feature.masterdata.api.BatteryRoute
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPage
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPageRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// Overview, tracking and journal share one list-detail scene
private const val BATTERIES_SCENE = "batteries"

/**
 * Grocy's batteries: the overview with the tracking and the journal beside it where the window is
 * wide enough.
 */
fun EntryProviderScope<NavKey>.batteriesEntries(
    dependencies: BatteriesDependencies,
    navigator: Navigator,
) {
    val masterData = dependencies.masterDataRepository
    val batteries = dependencies.batteriesRepository

    entry<BatteriesOverviewRoute>(
        metadata = listPaneMetadata(BATTERIES_SCENE) {
            DetailPlaceholder(
                icon = painterResource(Res.drawable.ic_rounded_battery_charging_full),
                text = stringResource(Res.string.batteries_detail_hint),
            )
        },
    ) { route ->
        val currentRoute = navigator.currentRoute
        BatteriesOverviewScreen(
            viewModel = viewModel {
                BatteriesOverviewViewModel(
                    masterData,
                    batteries,
                    dependencies.batteriesOverviewOptions,
                )
            },
            overview = viewModel { BatteryOverviewViewModel(masterData, batteries) },
            overviewLinks = BatteryOverviewLinks(
                onJournalClick = { navigator.openDetail(BatteriesJournalRoute(it)) },
                onEditClick = { navigator.navigate(BatteryRoute(it)) },
            ),
            links = BatteriesOverviewLinks(
                onTrackingClick = { navigator.openDetail(BatteryTrackingRoute()) },
                onJournalClick = { navigator.openDetail(BatteriesJournalRoute(it)) },
                // Forms and settings live in scenes of their own
                onEditClick = { navigator.navigate(BatteryRoute(it)) },
                onSettingsClick = {
                    navigator.navigate(GrocySettingsPageRoute(GrocySettingsPage.BATTERIES))
                },
                onManageBatteriesClick = { navigator.navigate(BatteriesRoute) },
            ),
            menuItems = appMenuItems(),
            selectedBatteryId = selectedBatteryOf(currentRoute),
            isBesideDetail = showsDetailBesideList(),
            isShown = isInBatteriesScene(currentRoute),
            onBackClick = { navigator.goBack(from = route) },
        )
    }

    entry<BatteryTrackingRoute>(
        metadata = { route ->
            if (route.inList) detailPaneMetadata(BATTERIES_SCENE) else emptyMap()
        },
    ) { route ->
        BatteryTrackingScreen(
            viewModel = viewModel {
                BatteryTrackingViewModel(
                    presetBatteryId = route.batteryId,
                    masterData = masterData,
                    batteries = batteries,
                    scanner = {
                        dependencies.scannerForms
                            .fieldState(ScannerForm.BATTERY_TRACKING, hasValue = it)
                    },
                )
            },
            menuItems = appMenuItems(),
            isBesideList = route.inList && showsDetailBesideList(),
            onBackClick = navigator::goBack,
            overview = viewModel { BatteryOverviewViewModel(masterData, batteries) },
            overviewLinks = BatteryOverviewLinks(
                onJournalClick = { navigator.navigate(BatteriesJournalRoute(it)) },
                onEditClick = { navigator.navigate(BatteryRoute(it)) },
            ),
        )
    }

    entry<BatteriesJournalRoute>(metadata = detailPaneMetadata(BATTERIES_SCENE)) { route ->
        BatteriesJournalScreen(
            viewModel = viewModel {
                BatteriesJournalViewModel(
                    batteryId = route.batteryId,
                    masterData = masterData,
                    batteries = batteries,
                    optionsRepository = dependencies.batteriesJournalOptions,
                )
            },
            menuItems = appMenuItems(),
            isBesideList = showsDetailBesideList(),
            onBackClick = navigator::goBack,
            overview = viewModel { BatteryOverviewViewModel(masterData, batteries) },
            onEditClick = { navigator.navigate(BatteryRoute(it)) },
        )
    }
}

/** The battery the list marks while [route] shows it beside the list. */
internal fun selectedBatteryOf(route: NavKey): Int? = when (route) {
    is BatteriesJournalRoute -> route.batteryId
    is BatteryTrackingRoute -> route.batteryId
    else -> null
}

/** Whether [route] shows the overview, also beside it, not a page on top of it. */
internal fun isInBatteriesScene(route: NavKey): Boolean =
    route == BatteriesOverviewRoute || route is BatteriesJournalRoute ||
        (route is BatteryTrackingRoute && route.inList)

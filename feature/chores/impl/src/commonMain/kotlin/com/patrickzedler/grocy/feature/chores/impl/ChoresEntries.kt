package com.patrickzedler.grocy.feature.chores.impl

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
import com.patrickzedler.grocy.core.resources.chores_detail_hint
import com.patrickzedler.grocy.core.resources.ic_rounded_cleaning_services
import com.patrickzedler.grocy.core.ui.appMenuItems
import com.patrickzedler.grocy.feature.chores.api.ChoreTrackingRoute
import com.patrickzedler.grocy.feature.chores.api.ChoresJournalRoute
import com.patrickzedler.grocy.feature.chores.api.ChoresOverviewRoute
import com.patrickzedler.grocy.feature.masterdata.api.ChoreRoute
import com.patrickzedler.grocy.feature.masterdata.api.ChoresRoute
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPage
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPageRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// Overview, card, tracking and journal share one list-detail scene
private const val CHORES_SCENE = "chores"

/**
 * Grocy's chores: the overview with a chore's card, the tracking and the journal beside it where
 * the window is wide enough.
 */
fun EntryProviderScope<NavKey>.choresEntries(
    dependencies: ChoresDependencies,
    navigator: Navigator,
) {
    val masterData = dependencies.masterDataRepository
    val chores = dependencies.choresRepository

    entry<ChoresOverviewRoute>(
        metadata = listPaneMetadata(CHORES_SCENE) {
            DetailPlaceholder(
                icon = painterResource(Res.drawable.ic_rounded_cleaning_services),
                text = stringResource(Res.string.chores_detail_hint),
            )
        },
    ) { route ->
        val currentRoute = navigator.currentRoute
        ChoresOverviewScreen(
            viewModel = viewModel {
                ChoresOverviewViewModel(masterData, chores, dependencies.choresOverviewOptions)
            },
            overview = viewModel { ChoreOverviewViewModel(masterData, chores) },
            overviewLinks = ChoreOverviewLinks(
                onJournalClick = { navigator.openDetail(ChoresJournalRoute(it)) },
                onEditClick = { navigator.navigate(ChoreRoute(it)) },
            ),
            links = ChoresOverviewLinks(
                onTrackingClick = { navigator.openDetail(ChoreTrackingRoute()) },
                onJournalClick = { navigator.openDetail(ChoresJournalRoute(it)) },
                // Forms and settings live in scenes of their own
                onEditClick = { navigator.navigate(ChoreRoute(it)) },
                onSettingsClick = {
                    navigator.navigate(GrocySettingsPageRoute(GrocySettingsPage.CHORES))
                },
                onManageChoresClick = { navigator.navigate(ChoresRoute) },
            ),
            menuItems = appMenuItems(),
            selectedChoreId = selectedChoreOf(currentRoute),
            isBesideDetail = showsDetailBesideList(),
            isShown = isInChoresScene(currentRoute),
            onBackClick = { navigator.goBack(from = route) },
        )
    }

    entry<ChoreTrackingRoute>(
        metadata = { route ->
            if (route.inList) detailPaneMetadata(CHORES_SCENE) else emptyMap()
        },
    ) { route ->
        ChoreTrackingScreen(
            viewModel = viewModel {
                ChoreTrackingViewModel(
                    presetChoreId = route.choreId,
                    masterData = masterData,
                    chores = chores,
                    scanner = {
                        dependencies.scannerForms
                            .fieldState(ScannerForm.CHORE_TRACKING, hasValue = it)
                    },
                )
            },
            menuItems = appMenuItems(),
            isBesideList = route.inList && showsDetailBesideList(),
            onBackClick = navigator::goBack,
            overview = viewModel { ChoreOverviewViewModel(masterData, chores) },
            overviewLinks = ChoreOverviewLinks(
                onJournalClick = { navigator.navigate(ChoresJournalRoute(it)) },
                onEditClick = { navigator.navigate(ChoreRoute(it)) },
            ),
        )
    }

    entry<ChoresJournalRoute>(metadata = detailPaneMetadata(CHORES_SCENE)) { route ->
        ChoresJournalScreen(
            viewModel = viewModel {
                ChoresJournalViewModel(
                    choreId = route.choreId,
                    masterData = masterData,
                    chores = chores,
                    optionsRepository = dependencies.choresJournalOptions,
                )
            },
            menuItems = appMenuItems(),
            isBesideList = showsDetailBesideList(),
            onBackClick = navigator::goBack,
            overview = viewModel { ChoreOverviewViewModel(masterData, chores) },
            onEditClick = { navigator.navigate(ChoreRoute(it)) },
        )
    }
}

/** The chore the list marks while [route] shows it beside the list. */
internal fun selectedChoreOf(route: NavKey): Int? = when (route) {
    is ChoresJournalRoute -> route.choreId
    is ChoreTrackingRoute -> route.choreId
    else -> null
}

/** Whether [route] shows the overview, also beside it, not a page on top of it. */
internal fun isInChoresScene(route: NavKey): Boolean =
    route == ChoresOverviewRoute || route is ChoresJournalRoute ||
        (route is ChoreTrackingRoute && route.inList)

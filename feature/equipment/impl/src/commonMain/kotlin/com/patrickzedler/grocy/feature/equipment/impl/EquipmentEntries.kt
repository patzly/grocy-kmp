package com.patrickzedler.grocy.feature.equipment.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.design.component.DetailPlaceholder
import com.patrickzedler.grocy.core.forms.ObjectActionsViewModel
import com.patrickzedler.grocy.core.forms.formFrame
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.equipment_detail_hint
import com.patrickzedler.grocy.core.resources.ic_rounded_build
import com.patrickzedler.grocy.core.ui.appMenuItems
import com.patrickzedler.grocy.feature.equipment.api.EquipmentDetailRoute
import com.patrickzedler.grocy.feature.equipment.api.EquipmentFormRoute
import com.patrickzedler.grocy.feature.equipment.api.EquipmentRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserfieldsRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// The list, a piece's manual and notes and its form share one list-detail scene
private const val EQUIPMENT_SCENE = "equipment"

/** Grocy's equipment: the list with a piece beside it where the window is wide enough. */
fun EntryProviderScope<NavKey>.equipmentEntries(
    dependencies: EquipmentDependencies,
    navigator: Navigator,
) {
    val masterData = dependencies.masterDataRepository
    val equipment = dependencies.equipmentRepository

    entry<EquipmentRoute>(
        metadata = listPaneMetadata(EQUIPMENT_SCENE) {
            DetailPlaceholder(
                icon = painterResource(Res.drawable.ic_rounded_build),
                text = stringResource(Res.string.equipment_detail_hint),
            )
        },
    ) { route ->
        val currentRoute = navigator.currentRoute
        EquipmentScreen(
            viewModel = viewModel {
                EquipmentViewModel(masterData, equipment, dependencies.equipmentOptions)
            },
            objectActions = viewModel {
                ObjectActionsViewModel(MasterDataRepository.EQUIPMENT, masterData)
            },
            links = EquipmentLinks(
                onCreateClick = { navigator.openDetail(EquipmentFormRoute()) },
                onEquipmentClick = { navigator.openDetail(EquipmentDetailRoute(it)) },
                onEditClick = { navigator.openDetail(EquipmentFormRoute(it)) },
                onEquipmentDeleted = { id ->
                    if (selectedEquipmentOf(navigator.currentRoute) == id) navigator.goBack()
                },
            ),
            menuItems = appMenuItems(),
            selectedId = selectedEquipmentOf(currentRoute),
            isBesideDetail = showsDetailBesideList(),
            isShown = isInEquipmentScene(currentRoute),
            onBackClick = { navigator.goBack(from = route) },
        )
    }

    entry<EquipmentDetailRoute>(
        metadata = { route ->
            if (route.inList) detailPaneMetadata(EQUIPMENT_SCENE) else emptyMap()
        },
    ) { route ->
        EquipmentDetailScreen(
            viewModel = viewModel { EquipmentDetailViewModel(route.id, masterData, equipment) },
            isBesideList = route.inList && showsDetailBesideList(),
            onBackClick = navigator::goBack,
            onEditClick = { navigator.navigate(EquipmentFormRoute(route.id)) },
            onFullscreenClick = {
                navigator.navigate(EquipmentDetailRoute(route.id, inList = false))
            },
        )
    }

    entry<EquipmentFormRoute>(metadata = detailPaneMetadata(EQUIPMENT_SCENE)) { route ->
        EquipmentFormScreen(
            viewModel = viewModel { EquipmentFormViewModel(route.id, masterData, equipment) },
            frame = formFrame(
                navigator = navigator,
                route = route,
                entity = MasterDataRepository.EQUIPMENT,
                repository = masterData,
            ) { EquipmentFormRoute(it) },
            onConfigureUserfieldsClick = {
                navigator.navigate(UserfieldsRoute(MasterDataRepository.EQUIPMENT))
            },
        )
    }
}

/** The piece the list marks while [route] shows it beside the list. */
internal fun selectedEquipmentOf(route: NavKey): Int? = when (route) {
    is EquipmentDetailRoute -> route.id
    is EquipmentFormRoute -> route.id
    else -> null
}

/** Whether [route] shows the list, also beside it, not a page on top of it. */
internal fun isInEquipmentScene(route: NavKey): Boolean =
    route == EquipmentRoute || route is EquipmentFormRoute ||
        (route is EquipmentDetailRoute && route.inList)

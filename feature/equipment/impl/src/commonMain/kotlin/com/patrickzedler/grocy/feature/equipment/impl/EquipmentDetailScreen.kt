@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.equipment.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.AppBarAction
import com.patrickzedler.grocy.core.design.component.AppBarIconButton
import com.patrickzedler.grocy.core.design.component.ConnectedButtonGroup
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_retry
import com.patrickzedler.grocy.core.resources.data_load_failed
import com.patrickzedler.grocy.core.resources.data_offline
import com.patrickzedler.grocy.core.resources.equipment_download
import com.patrickzedler.grocy.core.resources.equipment_edit
import com.patrickzedler.grocy.core.resources.equipment_fullscreen
import com.patrickzedler.grocy.core.resources.equipment_manual
import com.patrickzedler.grocy.core.resources.equipment_no_file
import com.patrickzedler.grocy.core.resources.equipment_no_manual
import com.patrickzedler.grocy.core.resources.equipment_no_notes
import com.patrickzedler.grocy.core.resources.equipment_no_preview
import com.patrickzedler.grocy.core.resources.equipment_notes
import com.patrickzedler.grocy.core.resources.equipment_save_failed
import com.patrickzedler.grocy.core.resources.ic_rounded_download
import com.patrickzedler.grocy.core.resources.ic_rounded_edit
import com.patrickzedler.grocy.core.resources.ic_rounded_fullscreen
import com.patrickzedler.grocy.core.ui.AppMenuDialogs
import com.patrickzedler.grocy.core.ui.LocalAppMenuLinks
import com.patrickzedler.grocy.core.ui.PdfDocumentView
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import com.patrickzedler.grocy.core.ui.SavedFile
import com.patrickzedler.grocy.core.ui.rememberAppMenu
import com.patrickzedler.grocy.core.ui.rememberFileSaver
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What Grocy's equipment page shows of a piece: its tabs "Instruction manual", "Notes" and one per
 * file userfield as connected buttons, the file in the viewer of the platform. The app bar saves
 * the file shown, expands to a screen of its own while beside the list and leads to the form.
 */
@Composable
internal fun EquipmentDetailScreen(
    viewModel: EquipmentDetailViewModel,
    isBesideList: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val links = LocalAppMenuLinks.current
    val appMenu = rememberAppMenu(links?.onSettingsClick, links?.onAboutClick)
    var isMenuOpen by remember { mutableStateOf(false) }
    val isPdfCovered = isMenuOpen || appMenu.isDialogOpen ||
        snackbarHostState.currentSnackbarData != null
    val current = state
    val tab = current?.let(viewModel::tabOf)
    val file = viewModel.fileState
    val save = rememberFileSaver(mimeType = PDF) { success ->
        if (!success) {
            scope.launch {
                snackbarHostState.showSnackbar(getString(Res.string.equipment_save_failed))
            }
        }
    }

    RefreshErrorEffect(
        error = refresh.error,
        hasData = current != null,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    LaunchedEffect(tab, current?.equipment) {
        current?.let { viewModel.load(viewModel.tabOf(it), it.equipment) }
    }

    AppMenuDialogs(appMenu)

    SegmentedListScreen(
        title = current?.equipment?.name.orEmpty(),
        modifier = modifier,
        navigationIcon = { if (!isBesideList) NavigateBackButton(onClick = onBackClick) },
        actions = buildList {
            if (file is FileState.Loaded) {
                add(
                    AppBarAction { actionModifier, interactionSource ->
                        AppBarIconButton(
                            painter = painterResource(Res.drawable.ic_rounded_download),
                            contentDescription = stringResource(Res.string.equipment_download),
                            onClick = { save(SavedFile(file.fileName, file.bytes)) },
                            modifier = actionModifier,
                            interactionSource = interactionSource,
                        )
                    },
                )
            }
            if (isBesideList) {
                add(
                    AppBarAction { actionModifier, interactionSource ->
                        AppBarIconButton(
                            painter = painterResource(Res.drawable.ic_rounded_fullscreen),
                            contentDescription = stringResource(Res.string.equipment_fullscreen),
                            onClick = onFullscreenClick,
                            modifier = actionModifier,
                            interactionSource = interactionSource,
                        )
                    },
                )
            }
            add(
                AppBarAction { actionModifier, interactionSource ->
                    AppBarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_edit),
                        contentDescription = stringResource(Res.string.equipment_edit),
                        onClick = onEditClick,
                        modifier = actionModifier,
                        interactionSource = interactionSource,
                    )
                },
            )
        },
        menuItems = appMenu.items,
        onMenuExpandedChange = { isMenuOpen = it },
        pane = ScreenPane.Detail.takeIf { isBesideList },
        snackbarHostState = snackbarHostState,
    ) {
        if (current == null || tab == null) return@SegmentedListScreen
        insetItem(key = "tabs") {
            ConnectedButtonGroup(
                options = current.tabs,
                checked = tab,
                onCheckedChange = viewModel::chooseTab,
                modifier = Modifier.fillMaxWidth(),
                label = { it.title() },
            )
        }
        when (tab) {
            EquipmentTab.Notes -> textItem(
                key = "notes",
                text = { current.notes ?: stringResource(Res.string.equipment_no_notes) },
            )
            else -> fileItem(tab, file, isPdfCovered, viewModel::retry)
        }
    }
}

@Composable
private fun EquipmentTab.title(): String = when (this) {
    EquipmentTab.Manual -> stringResource(Res.string.equipment_manual)
    EquipmentTab.Notes -> stringResource(Res.string.equipment_notes)
    is EquipmentTab.File -> caption
}

private fun LazyListScope.textItem(key: String, text: @Composable () -> String) {
    insetItem(key = key) {
        SegmentedContainer(index = 0, count = 1) {
            Text(
                text = text(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private fun LazyListScope.fileItem(
    tab: EquipmentTab,
    file: FileState,
    isCovered: Boolean,
    onRetryClick: () -> Unit,
) {
    when (file) {
        FileState.None -> textItem("file-none") {
            stringResource(
                if (tab == EquipmentTab.Manual) {
                    Res.string.equipment_no_manual
                } else {
                    Res.string.equipment_no_file
                },
            )
        }
        FileState.Loading -> insetItem(key = "file-loading") {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
        is FileState.Failed -> insetItem(key = "file-failed") {
            FileFailed(file.isOffline, onRetryClick)
        }
        is FileState.Loaded -> if (file.isPdf) {
            pdfItem(file, isCovered)
        } else {
            textItem("file-no-preview") { stringResource(Res.string.equipment_no_preview) }
        }
    }
}

private fun LazyListScope.pdfItem(file: FileState.Loaded, isCovered: Boolean) {
    // As high as the list, the document scrolls on its own; wide like a page of its own
    insetItem(key = "file-${file.fileName}", maxWidth = PdfMaxWidth) {
        PdfItem(file, isCovered)
    }
}

@Composable
private fun LazyItemScope.PdfItem(file: FileState.Loaded, isCovered: Boolean) {
    var failed by remember(file) { mutableStateOf(false) }
    if (failed) {
        SegmentedContainer(index = 0, count = 1) {
            Text(
                text = stringResource(Res.string.equipment_no_preview),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    } else {
        PdfDocumentView(
            bytes = file.bytes,
            isCovered = isCovered,
            onFailed = { failed = true },
            modifier = Modifier
                .fillParentMaxHeight(PdfHeightFraction)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium),
        )
    }
}

@Composable
private fun FileFailed(isOffline: Boolean, onRetryClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    SegmentedContainer(index = 0, count = 1) {
        Text(
            text = stringResource(
                if (isOffline) Res.string.data_offline else Res.string.data_load_failed,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        TextButton(
            onClick = {
                haptic.confirm()
                onRetryClick()
            },
            modifier = Modifier.clickCursor(),
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(stringResource(Res.string.action_retry))
        }
    }
}

private const val PDF = "application/pdf"
private const val PdfHeightFraction = 0.85f
private val PdfMaxWidth = 1200.dp

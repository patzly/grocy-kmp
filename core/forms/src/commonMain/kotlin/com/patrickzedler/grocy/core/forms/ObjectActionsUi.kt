package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_copy
import com.patrickzedler.grocy.core.resources.action_delete
import com.patrickzedler.grocy.core.resources.action_merge
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.data_save_failed_details
import com.patrickzedler.grocy.core.resources.merge_chore_keep
import com.patrickzedler.grocy.core.resources.merge_chore_keep_hint
import com.patrickzedler.grocy.core.resources.merge_chore_remove
import com.patrickzedler.grocy.core.resources.merge_chore_remove_hint
import com.patrickzedler.grocy.core.resources.merge_chore_required
import com.patrickzedler.grocy.core.resources.merge_chores_title
import com.patrickzedler.grocy.core.resources.merge_product_keep
import com.patrickzedler.grocy.core.resources.merge_product_keep_hint
import com.patrickzedler.grocy.core.resources.merge_product_remove
import com.patrickzedler.grocy.core.resources.merge_product_remove_hint
import com.patrickzedler.grocy.core.resources.merge_product_required
import com.patrickzedler.grocy.core.resources.merge_products_title
import com.patrickzedler.grocy.core.resources.merge_same
import com.patrickzedler.grocy.core.resources.objects_action_offline
import com.patrickzedler.grocy.core.resources.objects_delete_barcode
import com.patrickzedler.grocy.core.resources.objects_delete_battery
import com.patrickzedler.grocy.core.resources.objects_delete_chore
import com.patrickzedler.grocy.core.resources.objects_delete_conversion
import com.patrickzedler.grocy.core.resources.objects_delete_failed
import com.patrickzedler.grocy.core.resources.objects_delete_location
import com.patrickzedler.grocy.core.resources.objects_delete_product
import com.patrickzedler.grocy.core.resources.objects_delete_product_group
import com.patrickzedler.grocy.core.resources.objects_delete_product_hint
import com.patrickzedler.grocy.core.resources.objects_delete_quantity_unit
import com.patrickzedler.grocy.core.resources.objects_delete_store
import com.patrickzedler.grocy.core.resources.objects_delete_task_category
import com.patrickzedler.grocy.core.resources.objects_delete_userentity
import com.patrickzedler.grocy.core.resources.objects_delete_userfield
import com.patrickzedler.grocy.core.resources.objects_merge_failed
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** The menu items of the actions on object [id], in Grocy's words. */
@Composable
fun objectActionItems(actions: ObjectActionsViewModel, id: Int): List<AppBarMenuItem> {
    val labels = actions.actions.map { stringResource(it.label) }
    return remember(actions, id, labels) {
        actions.actions.mapIndexed { index, action ->
            AppBarMenuItem(labels[index]) { actions.request(action, id) }
        }
    }
}

private val ObjectAction.label: StringResource
    get() = when (this) {
        ObjectAction.COPY -> Res.string.action_copy
        ObjectAction.MERGE -> Res.string.action_merge
        ObjectAction.DELETE -> Res.string.action_delete
    }

/**
 * The questions of [actions] and its failures as snackbars in [snackbarHostState]; [onDone] gets
 * what succeeded, e.g. to close the form of a deleted object.
 */
@Composable
fun ObjectActionsHost(
    actions: ObjectActionsViewModel,
    snackbarHostState: SnackbarHostState,
    onDone: (ObjectActionEvent) -> Unit,
) {
    val currentOnDone by rememberUpdatedState(onDone)

    LaunchedEffect(actions) {
        actions.events.collect { event ->
            when (event) {
                ObjectActionEvent.Offline ->
                    snackbarHostState.showSnackbar(getString(Res.string.objects_action_offline))
                is ObjectActionEvent.Failed -> snackbarHostState.showSnackbar(
                    listOfNotNull(
                        getString(event.action.failure),
                        event.serverMessage?.let {
                            getString(Res.string.data_save_failed_details, it)
                        },
                    ).joinToString("\n"),
                )
                else -> currentOnDone(event)
            }
        }
    }

    when (val dialog = actions.dialog) {
        is ObjectActionDialog.Delete -> DeleteDialog(
            question = stringResource(deleteQuestionOf(actions.entity), dialog.name),
            hint = if (actions.entity == MasterDataRepository.PRODUCTS) {
                stringResource(Res.string.objects_delete_product_hint)
            } else {
                null
            },
            onDelete = { actions.delete(dialog.id) },
            onDismissRequest = actions::dismissDialog,
        )
        is ObjectActionDialog.Merge -> {
            val candidates by actions.mergeCandidates.collectAsStateWithLifecycle(emptyList())
            MergeDialog(
                texts = if (actions.entity == MasterDataRepository.CHORES) {
                    ChoreMerge
                } else {
                    ProductMerge
                },
                candidates = candidates,
                initialKeepId = dialog.keepId,
                onMerge = actions::merge,
                onDismissRequest = actions::dismissDialog,
            )
        }
        null -> Unit
    }
}

// Grocy explains failed copies with its saving error, a name that exists already
private val ObjectAction.failure: StringResource
    get() = when (this) {
        ObjectAction.COPY -> Res.string.data_save_failed
        ObjectAction.MERGE -> Res.string.objects_merge_failed
        ObjectAction.DELETE -> Res.string.objects_delete_failed
    }

private fun deleteQuestionOf(entity: String): StringResource = when (entity) {
    MasterDataRepository.PRODUCTS -> Res.string.objects_delete_product
    MasterDataRepository.LOCATIONS -> Res.string.objects_delete_location
    MasterDataRepository.STORES -> Res.string.objects_delete_store
    MasterDataRepository.QUANTITY_UNITS -> Res.string.objects_delete_quantity_unit
    MasterDataRepository.PRODUCT_GROUPS -> Res.string.objects_delete_product_group
    MasterDataRepository.CHORES -> Res.string.objects_delete_chore
    MasterDataRepository.BATTERIES -> Res.string.objects_delete_battery
    MasterDataRepository.TASK_CATEGORIES -> Res.string.objects_delete_task_category
    MasterDataRepository.USERENTITIES -> Res.string.objects_delete_userentity
    MasterDataRepository.PRODUCT_BARCODES -> Res.string.objects_delete_barcode
    MasterDataRepository.QU_CONVERSIONS -> Res.string.objects_delete_conversion
    else -> Res.string.objects_delete_userfield
}

/**
 * Grocy's question before deleting, with its warning for products; [confirmText] names what
 * confirming does, e.g. emptying a list.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeleteDialog(
    question: String,
    hint: String?,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
    confirmText: String = stringResource(Res.string.action_delete),
) {
    val haptic = LocalHapticFeedback.current

    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        ScrollableAlertDialogContent(
            title = { Text(question) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDelete()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(confirmText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismissRequest()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
            content = hint?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}

/** The texts of Grocy's merge dialog of products or chores. */
private class MergeTexts(
    val title: StringResource,
    val keep: StringResource,
    val keepHint: StringResource,
    val remove: StringResource,
    val removeHint: StringResource,
    val required: StringResource,
)

private val ProductMerge = MergeTexts(
    title = Res.string.merge_products_title,
    keep = Res.string.merge_product_keep,
    keepHint = Res.string.merge_product_keep_hint,
    remove = Res.string.merge_product_remove,
    removeHint = Res.string.merge_product_remove_hint,
    required = Res.string.merge_product_required,
)

private val ChoreMerge = MergeTexts(
    title = Res.string.merge_chores_title,
    keep = Res.string.merge_chore_keep,
    keepHint = Res.string.merge_chore_keep_hint,
    remove = Res.string.merge_chore_remove,
    removeHint = Res.string.merge_chore_remove_hint,
    required = Res.string.merge_chore_required,
)

/** Grocy's merge dialog: the object to keep, preset, and the one that disappears into it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MergeDialog(
    texts: MergeTexts,
    candidates: List<MergeCandidate>,
    initialKeepId: Int,
    onMerge: (keepId: Int, removeId: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var keepId by rememberSaveable { mutableStateOf<Int?>(initialKeepId) }
    var removeId by rememberSaveable { mutableStateOf<Int?>(null) }
    var keepUnmatched by remember { mutableStateOf(false) }
    var removeUnmatched by remember { mutableStateOf(false) }
    // Errors show once merging was tried, like Grocy's form validation
    var tried by rememberSaveable { mutableStateOf(false) }
    val options = remember(candidates) { candidates.map { DropdownOption(it.id, it.name) } }
    val error = mergeErrorOf(keepId, removeId)
    val required = stringResource(texts.required)
    val keepName = candidates.firstOrNull { it.id == keepId }?.name.orEmpty()

    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        ScrollableAlertDialogContent(
            title = { Text(stringResource(texts.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        tried = true
                        val keep = keepId
                        val remove = removeId
                        if (error == null && !keepUnmatched && !removeUnmatched &&
                            keep != null && remove != null
                        ) {
                            onMerge(keep, remove)
                        }
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_merge))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismissRequest()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                DropdownItem(
                    label = stringResource(texts.keep),
                    options = options,
                    selectedId = keepId,
                    onSelectedIdChange = { keepId = it },
                    onUnmatchedChange = { keepUnmatched = it },
                    index = 0,
                    count = 2,
                    hint = stringResource(texts.keepHint),
                    error = required.takeIf { tried && keepId == null && !keepUnmatched },
                )
                DropdownItem(
                    label = stringResource(texts.remove),
                    options = options,
                    selectedId = removeId,
                    onSelectedIdChange = { removeId = it },
                    onUnmatchedChange = { removeUnmatched = it },
                    index = 1,
                    count = 2,
                    hint = stringResource(texts.removeHint),
                    error = when {
                        !tried || removeUnmatched -> null
                        removeId == null -> required
                        error == MergeError.SAME -> stringResource(Res.string.merge_same, keepName)
                        else -> null
                    },
                )
            }
        }
    }
}

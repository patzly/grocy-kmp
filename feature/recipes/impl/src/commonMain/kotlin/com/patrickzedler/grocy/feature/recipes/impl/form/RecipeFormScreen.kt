@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.recipes.impl.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.SupportingLines
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.forms.DeleteDialog
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FormFrame
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormLoad
import com.patrickzedler.grocy.core.forms.ObjectFormScreen
import com.patrickzedler.grocy.core.forms.PictureDialog
import com.patrickzedler.grocy.core.forms.ProductThumbnail
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.model.recipes.recipeGrocycode
import com.patrickzedler.grocy.core.productcard.GrocycodeItem
import com.patrickzedler.grocy.core.productcard.rememberGrocycodeDownload
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_add
import com.patrickzedler.grocy.core.resources.action_delete
import com.patrickzedler.grocy.core.resources.grocycode_recipe
import com.patrickzedler.grocy.core.resources.grocycode_title
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.ic_rounded_image
import com.patrickzedler.grocy.core.resources.objects_delete_failed
import com.patrickzedler.grocy.core.resources.product_name
import com.patrickzedler.grocy.core.resources.product_name_required
import com.patrickzedler.grocy.core.resources.product_picture
import com.patrickzedler.grocy.core.resources.product_picture_none
import com.patrickzedler.grocy.core.resources.product_picture_will_delete
import com.patrickzedler.grocy.core.resources.product_picture_will_upload
import com.patrickzedler.grocy.core.resources.products_configure_userfields
import com.patrickzedler.grocy.core.resources.recipe_create
import com.patrickzedler.grocy.core.resources.recipe_edit
import com.patrickzedler.grocy.core.resources.recipe_included_recipes
import com.patrickzedler.grocy.core.resources.recipe_ingredient_delete_question
import com.patrickzedler.grocy.core.resources.recipe_ingredient_variable_amount
import com.patrickzedler.grocy.core.resources.recipe_ingredients_list
import com.patrickzedler.grocy.core.resources.recipe_nesting_delete_question
import com.patrickzedler.grocy.core.resources.recipe_not_check_shopping_list
import com.patrickzedler.grocy.core.resources.recipe_not_check_shopping_list_hint
import com.patrickzedler.grocy.core.resources.recipe_preparation
import com.patrickzedler.grocy.core.resources.recipe_produces_product
import com.patrickzedler.grocy.core.resources.recipe_produces_product_hint
import com.patrickzedler.grocy.core.resources.recipe_save_hint
import com.patrickzedler.grocy.core.resources.recipe_servings
import com.patrickzedler.grocy.core.resources.recipe_servings_hint
import com.patrickzedler.grocy.core.resources.recipes_servings
import com.patrickzedler.grocy.feature.recipes.impl.actions.RecipeActionsHost
import com.patrickzedler.grocy.feature.recipes.impl.actions.RecipeActionsViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's recipe form, every field in its order: name, servings, the shopping list check, the
 * produced product, the preparation and the userfields; then Grocy's right column of a saved
 * recipe: its ingredients and included recipes (each edited in a dialog, removed through its
 * menu), its picture and its grocycode.
 */
@Composable
internal fun RecipeFormScreen(
    viewModel: RecipeFormViewModel,
    frame: FormFrame,
    /** Prints the grocycode on the label printer and shows its messages. */
    actions: RecipeActionsViewModel,
    onConfigureUserfieldsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    RecipeActionsHost(actions, snackbarHostState) {}
    val related by viewModel.related.collectAsStateWithLifecycle()
    var showPictureDialog by rememberSaveable { mutableStateOf(false) }
    val downloadGrocycode = rememberGrocycodeDownload(snackbarHostState)

    RelatedDialogs(viewModel, snackbarHostState)
    val form = (viewModel.load as? ObjectFormLoad.Ready)?.form
    if (showPictureDialog && form != null) {
        PictureDialog(
            savedFileName = form.original?.pictureFileName,
            change = form.picture,
            loadSavedPicture = {
                form.original?.pictureFileName?.let { viewModel.picture(it) }
            },
            onChange = { form.picture = it },
            onDismissRequest = { showPictureDialog = false },
        )
    }

    ObjectFormScreen(
        title = stringResource(
            if (viewModel.isNew) Res.string.recipe_create else Res.string.recipe_edit,
        ),
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        toolbarActions = {
            FloatingToolbarIconButton(
                painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                contentDescription = stringResource(Res.string.products_configure_userfields),
                onClick = onConfigureUserfieldsClick,
            )
        },
    ) { state ->
        generalGroup(state, viewModel)
        // After the userfields, like Grocy's right column; only a saved recipe has them
        relatedGroups(viewModel, related)
        pictureGroup(state, viewModel::picture) { showPictureDialog = true }
        if (!viewModel.isNew) {
            val config = viewModel.config
            val recipeId = viewModel.objectId
            segmentedGroup(key = "grocycode", title = Res.string.grocycode_title) {
                GrocycodeItem(
                    code = recipeGrocycode(recipeId),
                    objectName = stringResource(Res.string.grocycode_recipe),
                    twoDimensional = config.grocycode2D,
                    onDownloadClick = {
                        downloadGrocycode(recipeGrocycode(recipeId), config.grocycode2D)
                    },
                    onPrintClick = if (config.labelPrinter && config.labelPrinterRunsOnServer) {
                        { actions.print(recipeId) }
                    } else {
                        null
                    },
                    index = 0,
                    count = 1,
                )
            }
        }
    }
}

private fun LazyListScope.generalGroup(form: RecipeFormState, viewModel: RecipeFormViewModel) {
    segmentedGroup(key = "general") {
        val errors = viewModel.errors
        GroupItems(
            { index, count ->
                TextItem(
                    state = form.name,
                    label = stringResource(Res.string.product_name),
                    error = fieldErrorText(
                        errors[ObjectField.NAME],
                        Res.string.product_name_required,
                    ),
                    index = index,
                    count = count,
                )
            },
            { index, count ->
                TextItem(
                    state = form.baseServings,
                    label = stringResource(Res.string.recipe_servings),
                    hint = stringResource(Res.string.recipe_servings_hint),
                    error = fieldErrorText(errors[ObjectField.AMOUNT]),
                    keyboardType = KeyboardType.Decimal,
                    stepper = NumberStepper.Positive,
                    index = index,
                    count = count,
                )
            },
            if (viewModel.config.shoppingList) {
                { index, count ->
                    SegmentedSwitchItem(
                        headline = stringResource(Res.string.recipe_not_check_shopping_list),
                        supporting = stringResource(Res.string.recipe_not_check_shopping_list_hint),
                        checked = form.notCheckShoppingList,
                        onCheckedChange = { form.notCheckShoppingList = it },
                        index = index,
                        count = count,
                    )
                }
            } else {
                null
            },
            if (viewModel.config.stock) {
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.recipe_produces_product),
                        options = form.products.map { DropdownOption(it.id, it.name) },
                        selectedId = form.productId,
                        onSelectedIdChange = { form.productId = it },
                        onUnmatchedChange = { form.isProductUnmatched = it },
                        hint = stringResource(Res.string.recipe_produces_product_hint),
                        optional = true,
                        index = index,
                        count = count,
                    )
                }
            } else {
                null
            },
            { index, count ->
                TextItem(
                    state = form.preparation,
                    label = stringResource(Res.string.recipe_preparation),
                    multiLine = true,
                    index = index,
                    count = count,
                )
            },
        )
    }
}

private fun LazyListScope.relatedGroups(viewModel: RecipeFormViewModel, related: RecipeRelated) {
    val canAdd = !viewModel.isNew
    segmentedGroup(key = "ingredients", title = Res.string.recipe_ingredients_list) {
        val count = related.ingredients.size + 1
        val delete = stringResource(Res.string.action_delete)
        val variable = stringResource(Res.string.recipe_ingredient_variable_amount)
        related.ingredients.forEachIndexed { index, item ->
            RelatedItem(
                text = item.text,
                supportingLines = listOfNotNull(
                    item.group,
                    variable.takeIf { item.isVariableAmount },
                    item.note,
                ),
                menuItems = listOf(
                    AppBarMenuItem(delete) {
                        viewModel.requestDelete(RelatedDelete.Ingredient(item.id, item.text))
                    },
                ),
                index = index,
                count = count,
                onClick = { viewModel.openIngredient(item.id) },
            )
        }
        AddItem(
            enabled = canAdd,
            index = count - 1,
            count = count,
            onClick = { viewModel.openIngredient() },
        )
    }
    segmentedGroup(key = "nestings", title = Res.string.recipe_included_recipes) {
        val count = related.nestings.size + 1
        val delete = stringResource(Res.string.action_delete)
        related.nestings.forEachIndexed { index, item ->
            val servings = pluralStringResource(Res.plurals.recipes_servings, 2, item.servings)
            RelatedItem(
                text = item.name,
                supportingLines = listOf(servings),
                menuItems = listOf(
                    AppBarMenuItem(delete) {
                        viewModel.requestDelete(RelatedDelete.Nesting(item.id, item.name))
                    },
                ),
                index = index,
                count = count,
                onClick = { viewModel.openNesting(item.id) },
            )
        }
        AddItem(
            enabled = canAdd,
            index = count - 1,
            count = count,
            onClick = { viewModel.openNesting() },
        )
    }
}

private fun LazyListScope.pictureGroup(
    form: RecipeFormState,
    loadPicture: suspend (String) -> ByteArray?,
    onClick: () -> Unit,
) {
    segmentedGroup(key = "picture", title = Res.string.product_picture) {
        val haptic = LocalHapticFeedback.current
        val fileName = form.original?.pictureFileName
        val supporting = when (form.picture) {
            PictureChange.Delete -> stringResource(Res.string.product_picture_will_delete)
            is PictureChange.Replace -> stringResource(Res.string.product_picture_will_upload)
            // A saved picture needs no note, the item shows it
            PictureChange.Keep -> if (fileName == null) {
                stringResource(Res.string.product_picture_none)
            } else {
                null
            }
        }
        SegmentedListItem(
            onClick = {
                haptic.confirm()
                onClick()
            },
            modifier = Modifier.fillMaxWidth().clickCursor(),
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            colors = SegmentedListDefaults.colors(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = {
                LeadingTrailingContentWrapper {
                    Icon(painterResource(Res.drawable.ic_rounded_image), contentDescription = null)
                }
            },
            supportingContent = supporting?.let { { Text(it) } },
            // The picture the recipe will have, so it shows without opening the dialog
            trailingContent = when (val change = form.picture) {
                is PictureChange.Replace -> {
                    { ProductThumbnail(key = change, load = { change.bytes }) }
                }
                PictureChange.Keep -> fileName?.let { name ->
                    { ProductThumbnail(fileName = name, load = loadPicture) }
                }
                PictureChange.Delete -> null
            },
        ) {
            Text(stringResource(Res.string.product_picture))
        }
    }
}

/** An ingredient or included recipe: a tap edits it, its menu removes it. */
@Composable
private fun RelatedItem(
    text: String,
    supportingLines: List<String>,
    menuItems: List<AppBarMenuItem>,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        modifier = Modifier.fillMaxWidth().clickCursor(),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = if (supportingLines.isEmpty()) {
            null
        } else {
            { SupportingLines(supportingLines) }
        },
        trailingContent = { ListItemOverflowMenu(groups = listOf(menuItems)) },
    ) {
        Text(text)
    }
}

/** Grocy's "Add" below a table; off with its explanation until the recipe is saved. */
@Composable
private fun AddItem(enabled: Boolean, index: Int, count: Int, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val icon: Painter = painterResource(Res.drawable.ic_rounded_add)
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().clickCursor(enabled = enabled),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper { Icon(icon, contentDescription = null) }
        },
        supportingContent = if (enabled) {
            null
        } else {
            { Text(stringResource(Res.string.recipe_save_hint)) }
        },
    ) {
        Text(stringResource(Res.string.action_add))
    }
}

/** The ingredient and included recipe dialogs, Grocy's questions before removing them. */
@Composable
private fun RelatedDialogs(viewModel: RecipeFormViewModel, snackbarHostState: SnackbarHostState) {
    viewModel.ingredientDialog?.let { state ->
        IngredientDialog(
            state = state,
            onSave = viewModel::saveIngredient,
            onDismissRequest = viewModel::dismissIngredient,
        )
    }
    viewModel.nestingDialog?.let { state ->
        NestingDialog(
            state = state,
            onSave = viewModel::saveNesting,
            onDismissRequest = viewModel::dismissNesting,
        )
    }
    viewModel.relatedDelete?.let { delete ->
        val question: StringResource = when (delete) {
            is RelatedDelete.Ingredient -> Res.string.recipe_ingredient_delete_question
            is RelatedDelete.Nesting -> Res.string.recipe_nesting_delete_question
        }
        val name = when (delete) {
            is RelatedDelete.Ingredient -> delete.name
            is RelatedDelete.Nesting -> delete.name
        }
        DeleteDialog(
            question = stringResource(question, name),
            hint = null,
            onDelete = viewModel::confirmDelete,
            onDismissRequest = viewModel::dismissDelete,
        )
    }
    val deleteError = viewModel.relatedDeleteError
    LaunchedEffect(deleteError) {
        deleteError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(deleteError.actionMessage(Res.string.objects_delete_failed))
        viewModel.onDeleteErrorShown()
    }
}

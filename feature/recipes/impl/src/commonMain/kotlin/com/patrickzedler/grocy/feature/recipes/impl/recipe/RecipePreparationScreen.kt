package com.patrickzedler.grocy.feature.recipes.impl.recipe

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.AppBarIconButton
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.showsSecondColumn
import com.patrickzedler.grocy.core.productcard.LabelPrintEffect
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.ic_rounded_close
import com.patrickzedler.grocy.core.resources.recipe_keep_screen_on
import com.patrickzedler.grocy.core.resources.recipe_preparation_mode
import com.patrickzedler.grocy.core.ui.KeepScreenOn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The preparation mode: the recipe card for cooking in a screen of its own with a close button,
 * without toolbar. Wide screens show the picture, servings and ingredients on the left and the
 * preparation on the right, each scrolling on its own; narrower ones show the card's order.
 * The overflow menu only holds "Keep screen on", on whenever the mode opens.
 */
@Composable
internal fun RecipePreparationScreen(
    viewModel: RecipeViewModel,
    stockPageActions: StockPageActions,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val current = state

    KeepScreenOn(enabled = viewModel.keepsScreenOn)
    RecipeEvents(viewModel, snackbarHostState)
    LabelPrintEffect(stockPageActions.labels, snackbarHostState)
    RecipeDialogs(viewModel, current)

    BoxWithConstraints(modifier) {
        val isSplit = showsSecondColumn(maxWidth)
        SegmentedListScreen(
            title = stringResource(Res.string.recipe_preparation_mode),
            navigationIcon = {
                AppBarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_close),
                    contentDescription = stringResource(Res.string.action_close),
                    onClick = onCloseClick,
                )
            },
            actionMenuItems = listOf(
                AppBarMenuItem(
                    text = stringResource(Res.string.recipe_keep_screen_on),
                    checked = viewModel.keepsScreenOn,
                    onClick = { viewModel.setKeepScreenOn(!viewModel.keepsScreenOn) },
                ),
            ),
            snackbarHostState = snackbarHostState,
            secondContent = if (isSplit) {
                {
                    current?.sections?.forEachIndexed { index, section ->
                        recipePreparation(section, index)
                    }
                }
            } else {
                null
            },
        ) {
            if (current == null) return@SegmentedListScreen
            recipeHeader(current, viewModel)
            current.sections.forEachIndexed { index, section ->
                recipeIngredients(
                    section = section,
                    index = index,
                    state = current,
                    viewModel = viewModel,
                    onIngredientClick = stockPageActions.overview::show,
                )
                // Beside the ingredients on wide screens, after each recipe's otherwise
                if (!isSplit) recipePreparation(section, index)
            }
        }
    }

    IngredientOverviewHost(stockPageActions, snackbarHostState, current)
}

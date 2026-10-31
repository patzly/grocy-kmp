package com.patrickzedler.grocy.feature.recipes.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.design.component.DetailPlaceholder
import com.patrickzedler.grocy.core.forms.formFrame
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.productcard.ProductLinks
import com.patrickzedler.grocy.core.productcard.rememberStockPageActions
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_menu_book
import com.patrickzedler.grocy.core.resources.recipes_select_hint
import com.patrickzedler.grocy.core.ui.appMenuItems
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.masterdata.api.UserfieldsRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipeFormRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipeRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipesRoute
import com.patrickzedler.grocy.feature.recipes.impl.actions.RecipeActionEvent
import com.patrickzedler.grocy.feature.recipes.impl.actions.RecipeActionsViewModel
import com.patrickzedler.grocy.feature.recipes.impl.form.RecipeFormScreen
import com.patrickzedler.grocy.feature.recipes.impl.form.RecipeFormViewModel
import com.patrickzedler.grocy.feature.recipes.impl.list.RecipesLinks
import com.patrickzedler.grocy.feature.recipes.impl.list.RecipesScreen
import com.patrickzedler.grocy.feature.recipes.impl.list.RecipesViewModel
import com.patrickzedler.grocy.feature.recipes.impl.recipe.RecipeLinks
import com.patrickzedler.grocy.feature.recipes.impl.recipe.RecipeScreen
import com.patrickzedler.grocy.feature.recipes.impl.recipe.RecipeViewModel
import com.patrickzedler.grocy.feature.stock.api.StockEntriesRoute
import com.patrickzedler.grocy.feature.stock.api.StockJournalRoute
import com.patrickzedler.grocy.feature.stock.api.StockJournalSummaryRoute
import com.patrickzedler.grocy.feature.stock.api.bookingRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// List, recipes and their forms share one list-detail scene
private const val RECIPES_SCENE = "recipes"

/**
 * Grocy's recipes with their recipe cards and forms, which open beside the list where the window
 * is wide enough; a form opened from a card takes its place and back returns to the card.
 */
fun EntryProviderScope<NavKey>.recipesEntries(
    dependencies: RecipesDependencies,
    navigator: Navigator,
) {
    val masterData = dependencies.masterDataRepository
    val recipes = dependencies.recipesRepository

    entry<RecipesRoute>(
        metadata = listPaneMetadata(RECIPES_SCENE) {
            DetailPlaceholder(
                icon = painterResource(Res.drawable.ic_rounded_menu_book),
                text = stringResource(Res.string.recipes_select_hint),
            )
        },
    ) { route ->
        RecipesScreen(
            viewModel = viewModel {
                RecipesViewModel(
                    recipes = recipes,
                    masterData = masterData,
                    optionsRepository = dependencies.recipesOptions,
                    search = route.search,
                )
            },
            actions = viewModel { RecipeActionsViewModel(recipes, masterData) },
            links = RecipesLinks(
                onRecipeClick = { navigator.openDetail(RecipeRoute(it)) },
                onEditClick = { navigator.openDetail(RecipeFormRoute(it)) },
                onAddClick = { navigator.openDetail(RecipeFormRoute()) },
                onActionDone = { event ->
                    when (event) {
                        // Grocy shows the copy
                        is RecipeActionEvent.Copied -> navigator.openDetail(RecipeRoute(event.id))
                        is RecipeActionEvent.Deleted ->
                            if (shownRecipeId(navigator.currentRoute) == event.id) {
                                navigator.goBack()
                            }
                        else -> Unit
                    }
                },
            ),
            menuItems = appMenuItems(),
            onBackClick = { navigator.goBack(from = route) },
            selectedId = shownRecipeId(navigator.currentRoute),
            isBesideDetail = showsDetailBesideList(),
        )
    }

    entry<RecipeRoute>(metadata = detailPaneMetadata(RECIPES_SCENE)) { route ->
        val language = appLanguage()
        RecipeScreen(
            viewModel = viewModel {
                RecipeViewModel(
                    recipeId = route.id,
                    recipes = recipes,
                    masterData = masterData,
                    shoppingLists = dependencies.shoppingListRepository,
                    language = language,
                )
            },
            actions = viewModel { RecipeActionsViewModel(recipes, masterData) },
            stockPageActions = rememberStockPageActions(
                masterData = masterData,
                stock = dependencies.stockRepository,
                shoppingLists = dependencies.shoppingListRepository,
                // The product's pages open on their own, the recipe stays below them
                links = ProductLinks(
                    onEditClick = { navigator.navigate(ProductRoute(it)) },
                    onStockEntriesClick = { navigator.navigate(StockEntriesRoute(it)) },
                    onStockJournalClick = { navigator.navigate(StockJournalRoute(it)) },
                    onStockJournalSummaryClick = {
                        navigator.navigate(StockJournalSummaryRoute(it))
                    },
                    onBookingClick = { id, kind -> navigator.navigate(bookingRoute(kind, id)) },
                ),
            ),
            links = RecipeLinks(
                // Takes the card's place beside the list, back returns to the card
                onEditClick = { navigator.navigate(RecipeFormRoute(it)) },
                onActionDone = { event ->
                    when (event) {
                        is RecipeActionEvent.Copied -> navigator.openDetail(RecipeRoute(event.id))
                        is RecipeActionEvent.Deleted -> navigator.goBack()
                        else -> Unit
                    }
                },
            ),
            menuItems = appMenuItems(),
            isBesideList = showsDetailBesideList(),
            onBackClick = navigator::goBack,
        )
    }

    entry<RecipeFormRoute>(metadata = detailPaneMetadata(RECIPES_SCENE)) { route ->
        val language = appLanguage()
        RecipeFormScreen(
            viewModel = viewModel {
                RecipeFormViewModel(
                    id = route.id,
                    repository = masterData,
                    recipes = recipes,
                    language = language,
                )
            },
            frame = formFrame(
                navigator = navigator,
                route = route,
                entity = RecipesRepository.RECIPES,
                repository = masterData,
            ) { RecipeFormRoute(it) },
            actions = viewModel { RecipeActionsViewModel(recipes, masterData) },
            onConfigureUserfieldsClick = {
                navigator.navigate(UserfieldsRoute(RecipesRepository.RECIPES))
            },
        )
    }
}

/** The recipe shown beside the list, as card or form; null for none or a new recipe. */
private fun shownRecipeId(route: NavKey): Int? = when (route) {
    is RecipeRoute -> route.id
    is RecipeFormRoute -> route.id
    else -> null
}

// The app language, a change restarts the app, so ViewModels keep it
@Composable
private fun appLanguage(): String = LocalLocale.current.toLanguageTag()

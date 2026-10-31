package com.patrickzedler.grocy.feature.recipes.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.settings.RecipesOptionsRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository

/** What Grocy's recipes need from the app. */
interface RecipesDependencies {
    val masterDataRepository: MasterDataRepository

    /** The recipes, their ingredients and what the server computed for them. */
    val recipesRepository: RecipesRepository

    /** The product overview of the ingredients and its labels. */
    val stockRepository: StockRepository

    /** The product overview's "Add to shopping list" and the lists after adding missing ones. */
    val shoppingListRepository: ShoppingListRepository

    /** Fields, grouping, sort and view of the recipes, read at app start. */
    val recipesOptions: RecipesOptionsRepository
}

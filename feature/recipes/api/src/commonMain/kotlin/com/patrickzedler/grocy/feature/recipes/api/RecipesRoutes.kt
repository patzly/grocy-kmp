package com.patrickzedler.grocy.feature.recipes.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Grocy's "Recipes", searched for [search] if given, like Grocy's "Search for recipes" of the
 * stock overview; recipes and their forms open beside it where the window is wide enough.
 */
@Serializable
@SerialName("recipes")
data class RecipesRoute(val search: String? = null) : Route

/** Grocy's recipe card of recipe [id]: picture, servings, ingredients and preparation. */
@Serializable
@SerialName("recipes/recipe")
data class RecipeRoute(val id: Int) : DetailRoute

/** Grocy's recipe form: edits recipe [id], or creates a recipe without. */
@Serializable
@SerialName("recipes/edit")
data class RecipeFormRoute(val id: Int? = null) : DetailRoute

val recipesRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(RecipesRoute::class, RecipesRoute.serializer())
        subclass(RecipeRoute::class, RecipeRoute.serializer())
        subclass(RecipeFormRoute::class, RecipeFormRoute.serializer())
    }
}

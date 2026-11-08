package com.patrickzedler.grocy.apps.wear

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.LayoutElementBuilders.FontSetting
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.layout.androidImageResource
import androidx.wear.protolayout.layout.imageResource
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.graphicDataCard
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.iconEdgeButton
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.tile
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.shopping_list_status_done
import com.patrickzedler.grocy.core.resources.shopping_list_status_undone
import com.patrickzedler.grocy.core.resources.wear_list_empty
import com.patrickzedler.grocy.core.resources.wear_login_waiting
import com.patrickzedler.grocy.core.wear.WearPalette
import com.patrickzedler.grocy.core.wear.WearTileSummary
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * The tile: how much of the shopping list is left and what comes next, a tap opens the list.
 * Takes the colors of the watch face where the system offers them, otherwise the app's green.
 * Tiles cannot load fonts, so it asks the system's font for its rounded form.
 */
class ShoppingListTileService : Material3TileService(
    allowDynamicTheme = true,
    defaultColorScheme = GrocyTileColorScheme,
) {
    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val graph = (application as WearApplication).graph
        val summary = graph.tileSummary.first()
        // The stored list answers at once, a change arrives through the graph's update request
        if (summary != null) graph.appScope.launch { graph.shoppingList.refreshForTile() }
        val texts = TileTexts(
            title = summary?.listName?.ifEmpty { null } ?: getString(Res.string.shopping_list_list),
            headline = summary?.let { headlineOf(it) },
            body = when {
                summary == null -> getString(Res.string.wear_login_waiting)
                summary.nextItems.isNotEmpty() -> summary.nextItems.joinToString(", ")
                else -> null
            },
            open = getString(Res.string.shopping_list_list),
        )
        return tile(
            timeline = Timeline.fromLayoutElement(layout(summary, texts)),
            freshness = FRESHNESS,
        )
    }

    private suspend fun headlineOf(summary: WearTileSummary): String = when {
        summary.total == 0 -> getString(Res.string.wear_list_empty)
        summary.left == 0 -> getString(Res.string.shopping_list_status_done, summary.total)
        else -> getString(Res.string.shopping_list_status_undone, summary.left)
    }

    private fun MaterialScope.layout(summary: WearTileSummary?, texts: TileTexts): LayoutElement {
        val open: Clickable = clickable(
            action = launchAction(ComponentName(packageName, MainActivity::class.java.name)),
        )
        return primaryLayout(
            titleSlot = { roundedText(texts.title, Typography.TITLE_SMALL) },
            mainSlot = {
                if (summary == null || summary.total == 0) {
                    roundedText(texts.headline ?: texts.body.orEmpty(), Typography.BODY_MEDIUM, 3)
                } else {
                    graphicDataCard(
                        onClick = open,
                        title = { roundedText(texts.headline.orEmpty(), Typography.TITLE_MEDIUM) },
                        content = texts.body?.let {
                            { roundedText(it, Typography.BODY_SMALL, 2) }
                        },
                        graphic = {
                            circularProgressIndicator(
                                staticProgress = 1f - summary.left.toFloat() / summary.total,
                            )
                        },
                    )
                }
            },
            bottomSlot = {
                iconEdgeButton(
                    onClick = open,
                    modifier = LayoutModifier.contentDescription(texts.open),
                ) {
                    icon(imageResource(androidImageResource(R.drawable.ic_tile_shopping_cart)))
                }
            },
        )
    }

    private fun MaterialScope.roundedText(
        value: String,
        typography: Int,
        maxLines: Int = 1,
    ): LayoutElement = text(
        text = value.layoutString,
        typography = typography,
        maxLines = maxLines,
        settings = listOf(FontSetting.roundness(100)),
    )

    private class TileTexts(
        val title: String,
        val headline: String?,
        val body: String?,
        val open: String,
    )

    private companion object {
        // A changed list requests an update by itself, this is for changes made elsewhere
        val FRESHNESS = 15.minutes
    }
}

private val GrocyTileColorScheme = ColorScheme(
    primary = WearPalette.primary.toInt().argb,
    primaryDim = WearPalette.primaryDim.toInt().argb,
    primaryContainer = WearPalette.primaryContainer.toInt().argb,
    onPrimary = WearPalette.onPrimary.toInt().argb,
    onPrimaryContainer = WearPalette.onPrimaryContainer.toInt().argb,
    secondary = WearPalette.secondary.toInt().argb,
    secondaryDim = WearPalette.secondaryDim.toInt().argb,
    secondaryContainer = WearPalette.secondaryContainer.toInt().argb,
    onSecondary = WearPalette.onSecondary.toInt().argb,
    onSecondaryContainer = WearPalette.onSecondaryContainer.toInt().argb,
    tertiary = WearPalette.tertiary.toInt().argb,
    tertiaryDim = WearPalette.tertiaryDim.toInt().argb,
    tertiaryContainer = WearPalette.tertiaryContainer.toInt().argb,
    onTertiary = WearPalette.onTertiary.toInt().argb,
    onTertiaryContainer = WearPalette.onTertiaryContainer.toInt().argb,
    surfaceContainerLow = WearPalette.surfaceContainerLow.toInt().argb,
    surfaceContainer = WearPalette.surfaceContainer.toInt().argb,
    surfaceContainerHigh = WearPalette.surfaceContainerHigh.toInt().argb,
    onSurface = WearPalette.onSurface.toInt().argb,
    onSurfaceVariant = WearPalette.onSurfaceVariant.toInt().argb,
    outline = WearPalette.outline.toInt().argb,
    outlineVariant = WearPalette.outlineVariant.toInt().argb,
    background = WearPalette.background.toInt().argb,
    onBackground = WearPalette.onBackground.toInt().argb,
    error = WearPalette.error.toInt().argb,
    errorDim = WearPalette.errorDim.toInt().argb,
    errorContainer = WearPalette.errorContainer.toInt().argb,
    onError = WearPalette.onError.toInt().argb,
    onErrorContainer = WearPalette.onErrorContainer.toInt().argb,
)

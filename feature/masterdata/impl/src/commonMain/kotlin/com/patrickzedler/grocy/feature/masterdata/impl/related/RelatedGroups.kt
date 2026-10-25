package com.patrickzedler.grocy.feature.masterdata.impl.related

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.design.withTabularNumbers
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_add
import com.patrickzedler.grocy.core.resources.action_delete
import com.patrickzedler.grocy.core.resources.barcode_last_price
import com.patrickzedler.grocy.core.resources.conversion_show_resolved
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_conversion_path
import com.patrickzedler.grocy.core.resources.product_barcodes
import com.patrickzedler.grocy.feature.masterdata.impl.barcode.BarcodeItem
import com.patrickzedler.grocy.feature.masterdata.impl.conversion.ConversionEquation
import com.patrickzedler.grocy.feature.masterdata.impl.conversion.ConversionItem
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's barcode table of a product: each barcode edits on a tap, "Add" creates one. Without
 * [canAdd] (a product not saved yet) "Add" explains with [disabledHint] why it is off.
 */
internal fun LazyListScope.barcodesGroup(
    items: List<BarcodeItem>,
    canAdd: Boolean,
    disabledHint: StringResource,
    onBarcodeClick: (id: Int) -> Unit,
    onDeleteClick: (id: Int) -> Unit,
    onAddClick: () -> Unit,
) {
    segmentedGroup(key = "barcodes", title = Res.string.product_barcodes) {
        val count = items.size + 1
        val delete = stringResource(Res.string.action_delete)
        val lastPrice = Res.string.barcode_last_price
        items.forEachIndexed { index, item ->
            val price = item.lastPrice?.let { stringResource(lastPrice, it) }
            RelatedObjectItem(
                supportingLines = listOfNotNull(item.amount, item.storeName, price, item.note),
                menuItems = listOf(AppBarMenuItem(delete) { onDeleteClick(item.id) }),
                index = index,
                count = count,
                onClick = { onBarcodeClick(item.id) },
            ) {
                // Barcodes are digits mostly, which then line up
                Text(item.barcode, style = LocalTextStyle.current.withTabularNumbers())
            }
        }
        GroupActionItem(
            text = stringResource(Res.string.action_add),
            icon = painterResource(Res.drawable.ic_rounded_add),
            index = count - 1,
            count = count,
            onClick = onAddClick,
            supporting = if (canAdd) null else stringResource(disabledHint),
            enabled = canAdd,
        )
    }
}

/**
 * Grocy's table of a product's conversion overrides or a unit's default conversions, titled
 * [title]; [onShowResolvedClick] adds "Show resolved conversions" for products.
 */
internal fun LazyListScope.conversionsGroup(
    title: StringResource,
    items: List<ConversionItem>,
    canAdd: Boolean,
    disabledHint: StringResource,
    onConversionClick: (id: Int) -> Unit,
    onDeleteClick: (id: Int) -> Unit,
    onAddClick: () -> Unit,
    onShowResolvedClick: (() -> Unit)? = null,
) {
    segmentedGroup(key = "conversions", title = title) {
        val actionCount = if (onShowResolvedClick != null && canAdd) 2 else 1
        val count = items.size + actionCount
        val delete = stringResource(Res.string.action_delete)
        items.forEachIndexed { index, item ->
            RelatedObjectItem(
                supportingLines = emptyList(),
                menuItems = listOf(AppBarMenuItem(delete) { onDeleteClick(item.id) }),
                index = index,
                count = count,
                onClick = { onConversionClick(item.id) },
            ) {
                ConversionEquation(item.meaning)
            }
        }
        GroupActionItem(
            text = stringResource(Res.string.action_add),
            icon = painterResource(Res.drawable.ic_rounded_add),
            index = items.size,
            count = count,
            onClick = onAddClick,
            supporting = if (canAdd) null else stringResource(disabledHint),
            enabled = canAdd,
        )
        if (onShowResolvedClick != null && canAdd) {
            GroupActionItem(
                text = stringResource(Res.string.conversion_show_resolved),
                icon = painterResource(Res.drawable.ic_rounded_conversion_path),
                index = items.size + 1,
                count = count,
                onClick = onShowResolvedClick,
            )
        }
    }
}

package com.patrickzedler.grocy.core.productcard

import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.stock_consume
import com.patrickzedler.grocy.core.resources.stock_inventory
import com.patrickzedler.grocy.core.resources.stock_purchase
import com.patrickzedler.grocy.core.resources.stock_transfer
import org.jetbrains.compose.resources.StringResource

/** Grocy's four stock booking pages. */
enum class BookingKind { PURCHASE, CONSUME, TRANSFER, INVENTORY }

/** The title of the page of [this] kind, like Grocy's. */
val BookingKind.title: StringResource
    get() = when (this) {
        BookingKind.PURCHASE -> Res.string.stock_purchase
        BookingKind.CONSUME -> Res.string.stock_consume
        BookingKind.TRANSFER -> Res.string.stock_transfer
        BookingKind.INVENTORY -> Res.string.stock_inventory
    }

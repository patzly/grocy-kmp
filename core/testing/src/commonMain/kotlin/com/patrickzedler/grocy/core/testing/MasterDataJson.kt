package com.patrickzedler.grocy.core.testing

/**
 * Master data of [FakeGrocyServer], shaped like Grocy's API answers (numbers partly as strings,
 * as some Grocy versions send them):
 * - Milk (1, group Dairy, Fridge, in stock), Bread (2, no group, Pantry, out of stock), Old
 *   cheese (3, disabled, Dairy), Yoghurt (4, sub product of Milk, Fridge)
 * - quantity units Piece (1) and Pack (2), store Supermarket (1), barcode 4006381333931 of Milk
 * - the product overview of Milk with two purchase prices
 */
object MasterDataJson {

    const val CREATED_ID = 99

    const val PRODUCTS =
        """[""" +
            """{"id":1,"name":"Milk","active":1,"product_group_id":1,"location_id":1,""" +
            """"qu_id_stock":1,"qu_id_purchase":2,"qu_id_consume":1,"qu_id_price":2,""" +
            """"min_stock_amount":"2","userfields":{"brand":"Acme"}},""" +
            """{"id":2,"name":"Bread","active":"1","location_id":2,"qu_id_stock":1,""" +
            """"qu_id_purchase":1,"qu_id_consume":1,"qu_id_price":1,"min_stock_amount":0,""" +
            """"picture_file_name":"x1bread.jpg"},""" +
            """{"id":3,"name":"Old cheese","active":0,"product_group_id":1,"location_id":1,""" +
            """"qu_id_stock":1,"qu_id_purchase":1,"qu_id_consume":1,"qu_id_price":1},""" +
            """{"id":4,"name":"Yoghurt","active":1,"parent_product_id":"1","location_id":1,""" +
            """"qu_id_stock":1,"qu_id_purchase":1,"qu_id_consume":1,"qu_id_price":1}""" +
            """]"""

    const val CREATED_PRODUCT =
        """{"id":$CREATED_ID,"name":"Created","active":1,"location_id":1,"qu_id_stock":1,""" +
            """"qu_id_purchase":1,"qu_id_consume":1,"qu_id_price":1}"""

    const val PRODUCT_GROUPS =
        """[{"id":1,"name":"Dairy","active":1},{"id":2,"name":"Unused","active":0}]"""

    const val LOCATIONS =
        """[{"id":1,"name":"Fridge","is_freezer":0,"active":1},""" +
            """{"id":2,"name":"Pantry","is_freezer":"0","active":1}]"""

    const val QUANTITY_UNITS =
        """[{"id":1,"name":"Piece","name_plural":"Pieces","active":1},""" +
            """{"id":2,"name":"Pack","name_plural":"Packs","active":1}]"""

    const val STORES = """[{"id":1,"name":"Supermarket","active":1}]"""

    const val PRODUCT_BARCODES =
        """[{"id":1,"product_id":1,"barcode":"4006381333931","qu_id":2,"amount":"1"}]"""

    const val STOCK = """[{"product_id":"1","amount":"3","amount_aggregated":"3"}]"""

    const val USERFIELDS =
        """[{"id":1,"entity":"products","name":"brand","caption":"Brand",""" +
            """"type":"text-single-line","show_as_column_in_tables":1}]"""

    const val SERVER_CONFIG =
        """{"FEATURE_FLAG_STOCK":true,"FEATURE_FLAG_STOCK_PRICE_TRACKING":true,""" +
            """"FEATURE_FLAG_STOCK_LOCATION_TRACKING":true,"ENERGY_UNIT":"kcal"}"""

    const val USER_SETTINGS =
        """{"stock_decimal_places_amounts":"2","product_presets_location_id":"2",""" +
            """"product_presets_qu_id":"1","product_presets_product_group_id":-1}"""

    /** `/stock/products/1`: 3 pieces of Milk, 1 opened, a pack of 6 pieces costs 6. */
    const val MILK_DETAILS =
        """{"product":{"id":1,"name":"Milk"},"last_purchased":"2026-10-01",""" +
            """"last_used":"2026-10-20","stock_amount":"3","stock_amount_opened":"1",""" +
            """"stock_amount_aggregated":"3","stock_amount_opened_aggregated":"1",""" +
            """"is_aggregated_amount":"0","stock_value":"3","last_price":"1",""" +
            """"avg_price":"0.9","qu_conversion_factor_price_to_stock":"6",""" +
            """"average_shelf_life_days":"12","spoil_rate_percent":"10"}"""

    const val MILK_PRICE_HISTORY =
        """[{"date":"2026-10-01 00:00:00","price":"1",""" +
            """"shopping_location":{"id":1,"name":"Supermarket"}},""" +
            """{"date":"2026-09-01","price":"0.8","shopping_location":null}]"""

    /** Paths and answers [FakeGrocyServer] serves from the start. */
    val defaults: Map<String, String> = mapOf(
        "/api/objects/products" to PRODUCTS,
        "/api/objects/product_groups" to PRODUCT_GROUPS,
        "/api/objects/locations" to LOCATIONS,
        "/api/objects/quantity_units" to QUANTITY_UNITS,
        "/api/objects/shopping_locations" to STORES,
        "/api/objects/product_barcodes" to PRODUCT_BARCODES,
        "/api/stock" to STOCK,
        "/api/objects/userfields" to USERFIELDS,
        "/api/system/config" to SERVER_CONFIG,
        "/api/user/settings" to USER_SETTINGS,
        "/api/stock/products/1" to MILK_DETAILS,
        "/api/stock/products/1/price-history" to MILK_PRICE_HISTORY,
    )
}

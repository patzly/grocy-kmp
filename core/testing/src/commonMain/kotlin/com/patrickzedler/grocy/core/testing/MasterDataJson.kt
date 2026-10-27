package com.patrickzedler.grocy.core.testing

/**
 * Master data of [FakeGrocyServer], shaped like Grocy's API answers (numbers partly as strings,
 * as some Grocy versions send them):
 * - Milk (1, group Dairy, Fridge, in stock), Bread (2, no group, Pantry, out of stock), Old
 *   cheese (3, disabled, Dairy), Yoghurt (4, sub product of Milk, Fridge, 1 below its min.
 *   stock amount)
 * - quantity units Piece (1) and Pack (2), store Supermarket (1), barcode 4006381333931 of Milk
 * - the product overview of Milk with two purchase prices
 * - chores Vacuum (1, weekly, assigned to Alice) and Old chore (2, disabled), battery Remote
 *   (1), task category Garden (1), userentity Books (1), users Alice (1) and Bob (2)
 * - the default conversion 1 Pack = 6 Pieces and what Grocy derives from it for Milk
 * - shopping lists Shopping list (1) and Hardware store (2), the item of Milk (5) on the first
 * - Milk's stock entries: 2 pieces in the Fridge (1) and 1 opened piece (2); its journal: a
 *   purchase, a consumption and an undone consumption; its last purchase (price 1) and average
 *   price (0.9)
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
            """"qu_id_stock":1,"qu_id_purchase":1,"qu_id_consume":1,"qu_id_price":1,""" +
            """"min_stock_amount":"1"}""" +
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

    const val CHORES =
        """[{"id":1,"name":"Vacuum","description":"Living room","active":1,""" +
            """"period_type":"weekly","period_days":"0","period_interval":"2",""" +
            """"period_config":"monday,friday","start_date":"2026-01-01 08:00:00",""" +
            """"assignment_type":"in-alphabetical-order","assignment_config":"1",""" +
            """"track_date_only":"0","rollover":0,"consume_product_on_execution":0,""" +
            """"userfields":{"room":"Living room"}},""" +
            """{"id":2,"name":"Old chore","active":0,"period_type":"manually"}]"""

    const val BATTERIES =
        """[{"id":1,"name":"Remote","used_in":"Living room","charge_interval_days":"30",""" +
            """"active":1}]"""

    const val TASK_CATEGORIES = """[{"id":1,"name":"Garden","active":"1"}]"""

    const val USERENTITIES =
        """[{"id":1,"name":"books","caption":"Books","show_in_sidebar_menu":1}]"""

    const val USERS =
        """[{"id":1,"username":"alice","display_name":"Alice"},""" +
            """{"id":2,"username":"bob","display_name":"Bob"}]"""

    const val PRODUCT_BARCODES =
        """[{"id":1,"product_id":1,"barcode":"4006381333931","qu_id":2,"amount":"1"}]"""

    const val STOCK =
        """[{"product_id":"1","amount":"3","amount_aggregated":"3","value":"3",""" +
            """"best_before_date":"2026-10-20","amount_opened":"1",""" +
            """"amount_opened_aggregated":"1","is_aggregated_amount":"0","due_type":"1"}]"""

    /** `/stock/volatile`, of which the app reads the products below their min. stock amount. */
    const val VOLATILE_STOCK =
        """{"due_products":[],"overdue_products":[],"expired_products":[],""" +
            """"missing_products":[{"id":4,"name":"Yoghurt","amount_missing":"1",""" +
            """"is_partly_in_stock":0}]}"""

    const val LAST_PURCHASES =
        """[{"id":1,"product_id":"1","amount":"3","best_before_date":"2026-10-30",""" +
            """"purchased_date":"2026-10-01","location_id":1,"shopping_location_id":1,""" +
            """"price":"1"}]"""

    const val AVERAGE_PRICES = """[{"id":1,"product_id":1,"price":"0.9"}]"""

    const val STOCK_LOCATIONS =
        """[{"id":1,"product_id":"1","amount":"3","location_id":"1",""" +
            """"location_name":"Fridge","location_is_freezer":0}]"""

    const val USERFIELDS =
        """[{"id":1,"entity":"products","name":"brand","caption":"Brand",""" +
            """"type":"text-single-line","show_as_column_in_tables":1},""" +
            """{"id":2,"entity":"chores","name":"room","caption":"Room",""" +
            """"type":"text-single-line","sort_number":"1"}]"""

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

    const val QU_CONVERSIONS =
        """[{"id":1,"from_qu_id":2,"to_qu_id":1,"factor":"6","product_id":null}]"""

    const val RESOLVED_QU_CONVERSIONS =
        """[{"id":1,"product_id":1,"from_qu_id":"1","to_qu_id":"1","factor":"1"},""" +
            """{"id":2,"product_id":1,"from_qu_id":1,"to_qu_id":2,"factor":"0.1666666667"},""" +
            """{"id":3,"product_id":1,"from_qu_id":2,"to_qu_id":1,"factor":6},""" +
            """{"id":4,"product_id":1,"from_qu_id":2,"to_qu_id":2,"factor":1},""" +
            """{"id":5,"product_id":null,"from_qu_id":2,"to_qu_id":1,"factor":6}]"""

    const val SHOPPING_LISTS =
        """[{"id":1,"name":"Shopping list"},{"id":"2","name":"Hardware store"}]"""

    const val SHOPPING_LIST_ITEMS = """[{"id":5,"product_id":"1","shopping_list_id":1}]"""

    const val STOCK_ENTRIES =
        """[{"id":1,"product_id":1,"amount":"2","best_before_date":"2026-10-30",""" +
            """"purchased_date":"2026-10-01","stock_id":"6f1a2b","price":"1","open":0,""" +
            """"location_id":1,"shopping_location_id":1,""" +
            """"row_created_timestamp":"2026-10-01 10:00:00","userfields":null},""" +
            """{"id":2,"product_id":"1","amount":1,"best_before_date":"2026-10-20",""" +
            """"purchased_date":"2026-09-20","stock_id":"6f1a2c","price":null,"open":"1",""" +
            """"opened_date":"2026-10-21","location_id":"1","note":"Half full",""" +
            """"row_created_timestamp":"2026-09-20 09:00:00"}]"""

    const val STOCK_LOG =
        """[{"id":3,"product_id":1,"amount":"-1","transaction_type":"consume",""" +
            """"row_created_timestamp":"2026-10-22 18:00:00","spoiled":"1","undone":1,""" +
            """"undone_timestamp":"2026-10-22 18:05:00","location_id":1,"user_id":2},""" +
            """{"id":2,"product_id":1,"amount":-1,"transaction_type":"consume",""" +
            """"row_created_timestamp":"2026-10-21 08:00:00","spoiled":0,"undone":0,""" +
            """"location_id":1,"user_id":1},""" +
            """{"id":1,"product_id":"1","amount":"3","transaction_type":"purchase",""" +
            """"row_created_timestamp":"2026-10-01 10:00:00","spoiled":0,"undone":"0",""" +
            """"location_id":"1","user_id":"1","stock_id":"6f1a2b","note":"Weekly"}]"""

    /** The transaction of [BOOKING]. */
    const val TRANSACTION_ID = "t10"

    /** The bookings Grocy answers a consumption, opening or edit of a stock entry with. */
    const val BOOKING = """[{"id":10,"product_id":1,"amount":"-2","transaction_type":"consume",""" +
        """"transaction_id":"$TRANSACTION_ID"}]"""

    /** Paths and answers [FakeGrocyServer] serves from the start. */
    val defaults: Map<String, String> = mapOf(
        "/api/objects/products" to PRODUCTS,
        "/api/objects/product_groups" to PRODUCT_GROUPS,
        "/api/objects/locations" to LOCATIONS,
        "/api/objects/quantity_units" to QUANTITY_UNITS,
        "/api/objects/shopping_locations" to STORES,
        "/api/objects/product_barcodes" to PRODUCT_BARCODES,
        "/api/stock" to STOCK,
        "/api/stock/volatile" to VOLATILE_STOCK,
        "/api/objects/products_last_purchased" to LAST_PURCHASES,
        "/api/objects/products_average_price" to AVERAGE_PRICES,
        "/api/objects/stock_current_locations" to STOCK_LOCATIONS,
        "/api/objects/chores" to CHORES,
        "/api/objects/batteries" to BATTERIES,
        "/api/objects/task_categories" to TASK_CATEGORIES,
        "/api/objects/userentities" to USERENTITIES,
        "/api/users" to USERS,
        "/api/objects/userfields" to USERFIELDS,
        "/api/system/config" to SERVER_CONFIG,
        "/api/user/settings" to USER_SETTINGS,
        "/api/stock/products/1" to MILK_DETAILS,
        "/api/stock/products/1/price-history" to MILK_PRICE_HISTORY,
        "/api/objects/quantity_unit_conversions" to QU_CONVERSIONS,
        "/api/objects/quantity_unit_conversions_resolved" to RESOLVED_QU_CONVERSIONS,
        "/api/objects/shopping_lists" to SHOPPING_LISTS,
        "/api/objects/shopping_list" to SHOPPING_LIST_ITEMS,
        "/api/objects/stock" to STOCK_ENTRIES,
        "/api/objects/stock_log" to STOCK_LOG,
    )
}

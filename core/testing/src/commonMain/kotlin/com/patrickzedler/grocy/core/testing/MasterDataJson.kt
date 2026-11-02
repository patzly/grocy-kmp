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
 * - shopping lists Shopping list (1, with notes) and Hardware store (2), on the first the item
 *   of Milk (5: 6 pieces shown as Pack, note "Low fat") and the done note item Batteries (6)
 * - Milk's stock entries: 2 pieces in the Fridge (1) and 1 opened piece (2); its journal: a
 *   purchase, a consumption and an undone consumption; its last purchase (price 1) and average
 *   price (0.9)
 * - recipes Pancakes (1, 4 servings scaled to 2, with picture, 2 pieces of Milk and Bread in
 *   the group "Topping", not enough in stock but on the shopping list) and Soup (2, 1 serving,
 *   includes 2 servings of Pancakes, enough in stock), meal plan sections Breakfast (1, at
 *   "7:00") and the default one
 * - the meal plan of the week from Monday 2026-11-02 (Grocy's week "2026-44"): 2 servings of
 *   Pancakes for breakfast (1) and 2 pieces of Milk (2) on Monday, the done note "Eat out" (3)
 *   on Tuesday; the recipes Grocy keeps for it: the week (-1), Monday (-2), the Pancakes entry
 *   (-3, not enough in stock but on the shopping list) and Tuesday (-4), all served together
 *   with the normal recipes by `/objects/recipes`, which the client filters by type
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

    /** `/chores`: Vacuum is assigned to Alice and rescheduled, Old chore has no schedule. */
    const val CURRENT_CHORES =
        """[{"chore_id":1,"chore_name":"Vacuum","last_tracked_time":"2026-10-20 08:00:00",""" +
            """"next_estimated_execution_time":"2026-11-02 08:00:00","track_date_only":"0",""" +
            """"next_execution_assigned_to_user_id":"1","is_rescheduled":"1",""" +
            """"is_reassigned":"0"},""" +
            """{"chore_id":3,"chore_name":"Water plants","track_date_only":1,""" +
            """"next_estimated_execution_time":null,"last_tracked_time":null,""" +
            """"next_execution_assigned_to_user_id":null,"is_rescheduled":0,""" +
            """"is_reassigned":0}]"""

    /** `/chores/1`. */
    const val VACUUM_DETAILS =
        """{"last_tracked":"2026-10-20 08:00:00","tracked_count":"4",""" +
            """"last_done_by":{"id":1,"username":"alice","display_name":"Alice"},""" +
            """"average_execution_frequency_hours":"72.5"}"""

    /** The chores journal: an execution of Vacuum, a late one and an undone skipped one. */
    const val CHORES_LOG =
        """[{"id":12,"chore_id":1,"tracked_time":"2026-10-20 08:00:00",""" +
            """"scheduled_execution_time":"2026-10-20 08:00:00","done_by_user_id":1,""" +
            """"skipped":0,"undone":0,"row_created_timestamp":"2026-10-20 08:01:00",""" +
            """"userfields":{"mood":"Good"}},""" +
            """{"id":11,"chore_id":1,"tracked_time":"2026-10-10 09:00:00",""" +
            """"scheduled_execution_time":"2026-10-10 08:00:00","done_by_user_id":2,""" +
            """"skipped":0,"undone":0,"row_created_timestamp":"2026-10-10 09:00:00"},""" +
            """{"id":10,"chore_id":3,"tracked_time":"2026-10-01",""" +
            """"scheduled_execution_time":null,"skipped":1,"undone":1,""" +
            """"undone_timestamp":"2026-10-02 10:00:00",""" +
            """"row_created_timestamp":"2026-10-01 12:00:00"}]"""

    /** What tracking a chore answers: the execution as stored. */
    const val TRACKED_CHORE =
        """{"id":13,"chore_id":1,"tracked_time":"2026-11-02 08:00:00",""" +
            """"scheduled_execution_time":"2026-11-02 08:00:00","done_by_user_id":1,""" +
            """"skipped":0,"undone":0,"row_created_timestamp":"2026-11-02 09:00:00"}"""

    const val BATTERIES =
        """[{"id":1,"name":"Remote","used_in":"Living room","charge_interval_days":"30",""" +
            """"active":1}]"""

    const val TASK_CATEGORIES = """[{"id":1,"name":"Garden","active":"1"}]"""

    /** Mow the lawn (due, in Garden, assigned to Alice) */
    private const val MOW_TASK =
        """{"id":1,"name":"Mow the lawn","description":"Mind the roses",""" +
            """"due_date":"2026-11-01","done":0,"done_timestamp":null,"category_id":1,""" +
            """"assigned_to_user_id":1,"userfields":{"area":"Back"}}"""

    /** Old task, done on 2026-10-01. */
    private const val OLD_TASK =
        """{"id":3,"name":"Old task","description":null,"due_date":"2026-09-30","done":1,""" +
            """"done_timestamp":"2026-10-01 10:00:00","category_id":null,""" +
            """"assigned_to_user_id":null}"""

    /** `objects/tasks`: Mow the lawn, Buy seeds (no date, category or user), a done one. */
    const val TASKS =
        "[$MOW_TASK," +
            """{"id":2,"name":"Buy seeds","description":"","due_date":"","done":"0",""" +
            """"category_id":"","assigned_to_user_id":""},""" +
            "$OLD_TASK]"

    /** Mow the lawn after "Mark task as completed". */
    const val COMPLETED_TASK =
        """{"id":1,"name":"Mow the lawn","due_date":"2026-11-01","done":1,""" +
            """"done_timestamp":"2026-11-02 12:00:00","category_id":1,""" +
            """"assigned_to_user_id":1}"""

    /** Old task after "Undo task". */
    const val UNDONE_TASK =
        """{"id":3,"name":"Old task","due_date":"2026-09-30","done":0,""" +
            """"done_timestamp":null,"category_id":null,"assigned_to_user_id":null}"""

    /** A task as the server stores a new one. */
    const val CREATED_TASK =
        """{"id":$CREATED_ID,"name":"Created task","due_date":"2026-11-05","done":0,""" +
            """"category_id":1,"assigned_to_user_id":2}"""

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
        """[{"id":1,"name":"Shopping list","description":"<p>Bring bags</p>"},""" +
            """{"id":"2","name":"Hardware store","description":null}]"""

    const val SHOPPING_LIST_ITEMS =
        """[{"id":5,"product_id":"1","shopping_list_id":1,"note":"Low fat","amount":"6",""" +
            """"qu_id":"2","done":0,"userfields":null},""" +
            """{"id":6,"product_id":null,"shopping_list_id":"1","note":"Batteries",""" +
            """"amount":"2","qu_id":null,"done":"1","userfields":null}]"""

    /** `/objects/shopping_list/5`, the item of Milk. */
    const val MILK_SHOPPING_LIST_ITEM =
        """{"id":5,"product_id":"1","shopping_list_id":1,"note":"Low fat","amount":"6",""" +
            """"qu_id":"2","done":0,"userfields":null}"""

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

    const val RECIPES =
        """[{"id":1,"name":"Pancakes","description":"<p>Mix and fry</p>",""" +
            """"picture_file_name":"pancakes.jpg","base_servings":"4",""" +
            """"desired_servings":"2","not_check_shoppinglist":"0","type":"normal",""" +
            """"product_id":null,"userfields":{"vegan":"0"}},""" +
            """{"id":"2","name":"Soup","description":null,"picture_file_name":null,""" +
            """"base_servings":1,"desired_servings":1,"not_check_shoppinglist":1,""" +
            """"type":"normal","product_id":"4","userfields":null},""" +
            """{"id":-1,"name":"2026-44","type":"mealplan-week"},""" +
            """{"id":-2,"name":"2026-11-02","type":"mealplan-day"},""" +
            """{"id":-3,"name":"2026-11-02#1","type":"mealplan-shadow"},""" +
            """{"id":-4,"name":"2026-11-03","type":"mealplan-day"}]"""

    /** `/objects/recipes/1`, Pancakes. */
    const val PANCAKES =
        """{"id":1,"name":"Pancakes","description":"<p>Mix and fry</p>",""" +
            """"picture_file_name":"pancakes.jpg","base_servings":"4",""" +
            """"desired_servings":"2","not_check_shoppinglist":"0","type":"normal",""" +
            """"product_id":null,"userfields":{"vegan":"0"}}"""

    /** `/recipes/fulfillment`, a meal plan entry's recipe (-1) among them like Grocy has it. */
    const val RECIPES_FULFILLMENT =
        """[{"id":1,"recipe_id":"1","need_fulfilled":"0",""" +
            """"need_fulfilled_with_shopping_list":"1","missing_products_count":"1",""" +
            """"costs":"2.5","costs_per_serving":"1.25","calories":"350","due_score":"11",""" +
            """"product_names_comma_separated":"Milk,Bread","prices_incomplete":"1"},""" +
            """{"id":1,"recipe_id":2,"need_fulfilled":1,""" +
            """"need_fulfilled_with_shopping_list":1,"missing_products_count":0,"costs":0,""" +
            """"costs_per_serving":0,"calories":0,"due_score":0,""" +
            """"product_names_comma_separated":null,"prices_incomplete":0},""" +
            """{"id":1,"recipe_id":-1,"need_fulfilled":0,""" +
            """"need_fulfilled_with_shopping_list":0,"missing_products_count":1,""" +
            """"costs":"7.5","calories":"900","prices_incomplete":0},""" +
            """{"recipe_id":"-2","need_fulfilled":"0","need_fulfilled_with_shopping_list":"0",""" +
            """"missing_products_count":"1","costs":"7.5","calories":"900"},""" +
            """{"recipe_id":-3,"need_fulfilled":0,"need_fulfilled_with_shopping_list":1,""" +
            """"missing_products_count":1,"costs":5,"calories":700},""" +
            """{"recipe_id":-4,"need_fulfilled":1,"need_fulfilled_with_shopping_list":1,""" +
            """"missing_products_count":0,"costs":0,"calories":0}]"""

    const val RECIPE_INGREDIENTS =
        """[{"id":1,"recipe_id":1,"product_id":1,"amount":"4","qu_id":1,"note":"Cold",""" +
            """"only_check_single_unit_in_stock":"0","ingredient_group":null,""" +
            """"not_check_stock_fulfillment":0,"variable_amount":null,"price_factor":"1",""" +
            """"round_up":"0"},""" +
            """{"id":2,"recipe_id":"1","product_id":"2","amount":1,"qu_id":"1","note":null,""" +
            """"only_check_single_unit_in_stock":0,"ingredient_group":"Topping",""" +
            """"not_check_stock_fulfillment":0,"variable_amount":"a slice",""" +
            """"price_factor":1,"round_up":0}]"""

    /** `/objects/recipes_pos/1`, the Milk of Pancakes. */
    const val PANCAKES_MILK =
        """{"id":1,"recipe_id":1,"product_id":1,"amount":"4","qu_id":1,"note":"Cold",""" +
            """"only_check_single_unit_in_stock":"0","ingredient_group":null,""" +
            """"not_check_stock_fulfillment":0,"variable_amount":null,"price_factor":"1",""" +
            """"round_up":"0"}"""

    const val RECIPE_NESTINGS = """[{"id":1,"recipe_id":2,"includes_recipe_id":1,"servings":"2"}]"""

    /** `/objects/recipes_pos_resolved`: Pancakes' ingredients for Pancakes and for Soup. */
    const val RESOLVED_RECIPE_INGREDIENTS =
        """[{"recipe_id":1,"recipe_pos_id":1,"product_id":1,"recipe_amount":"2",""" +
            """"stock_amount":"3","need_fulfilled":"1","missing_amount":"0",""" +
            """"amount_on_shopping_list":"6","need_fulfilled_with_shopping_list":"1",""" +
            """"qu_id":1,"costs":"2","is_nested_recipe_pos":"0","ingredient_group":null,""" +
            """"product_group":"Dairy","id":1,"recipe_type":"normal","child_recipe_id":1,""" +
            """"note":"Cold","recipe_variable_amount":null,""" +
            """"only_check_single_unit_in_stock":"0","calories":"100",""" +
            """"product_active":1,"due_score":"1","product_id_effective":1,""" +
            """"product_name":"Milk"},""" +
            """{"recipe_id":"1","recipe_pos_id":"2","product_id":2,"recipe_amount":0.5,""" +
            """"stock_amount":0,"need_fulfilled":0,"missing_amount":0.5,""" +
            """"amount_on_shopping_list":0,"need_fulfilled_with_shopping_list":0,""" +
            """"qu_id":1,"costs":0.5,"is_nested_recipe_pos":0,"ingredient_group":"Topping",""" +
            """"product_group":null,"id":2,"recipe_type":"normal","child_recipe_id":1,""" +
            """"note":null,"recipe_variable_amount":"a slice",""" +
            """"only_check_single_unit_in_stock":0,"calories":250,"product_active":1,""" +
            """"due_score":10,"product_id_effective":2,"product_name":"Bread"},""" +
            """{"recipe_id":2,"recipe_pos_id":1,"product_id":1,"recipe_amount":1,""" +
            """"stock_amount":3,"need_fulfilled":1,"missing_amount":0,""" +
            """"amount_on_shopping_list":6,"need_fulfilled_with_shopping_list":1,"qu_id":1,""" +
            """"costs":1,"is_nested_recipe_pos":1,"ingredient_group":null,""" +
            """"product_group":"Dairy","id":1,"recipe_type":"normal","child_recipe_id":1,""" +
            """"note":"Cold","recipe_variable_amount":null,""" +
            """"only_check_single_unit_in_stock":0,"calories":50,"product_active":1,""" +
            """"due_score":1,"product_id_effective":1,"product_name":"Milk"}]"""

    const val MEAL_PLAN_SECTIONS =
        """[{"id":"-1","name":"","sort_number":"-1","time_info":null},""" +
            """{"id":1,"name":"Breakfast","sort_number":"1","time_info":"7:00"}]"""

    const val MEAL_PLAN =
        """[{"id":1,"day":"2026-11-02","type":"recipe","recipe_id":"1",""" +
            """"recipe_servings":"2","note":null,"product_id":null,"product_amount":"0",""" +
            """"product_qu_id":null,"section_id":"1","done":"0"},""" +
            """{"id":"2","day":"2026-11-02","type":"product","recipe_id":null,""" +
            """"recipe_servings":1,"note":"","product_id":1,"product_amount":2,""" +
            """"product_qu_id":1,"section_id":-1,"done":0},""" +
            """{"id":3,"day":"2026-11-03","type":"note","recipe_id":null,""" +
            """"recipe_servings":1,"note":"Eat out","product_id":null,"product_amount":0,""" +
            """"product_qu_id":null,"section_id":-1,"done":1}]"""

    /** `/objects/meal_plan/99`, an entry the server just created. */
    const val CREATED_MEAL_PLAN_ENTRY =
        """{"id":$CREATED_ID,"day":"2026-11-04","type":"note","note":"Leftovers",""" +
            """"section_id":-1,"done":0}"""

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
        "/api/chores" to CURRENT_CHORES,
        "/api/chores/1" to VACUUM_DETAILS,
        "/api/objects/chores_log" to CHORES_LOG,
        "/api/objects/chores_log/13" to TRACKED_CHORE,
        "/api/objects/batteries" to BATTERIES,
        "/api/objects/task_categories" to TASK_CATEGORIES,
        "/api/objects/tasks" to TASKS,
        "/api/objects/tasks/1" to MOW_TASK,
        "/api/objects/tasks/3" to OLD_TASK,
        "/api/objects/tasks/$CREATED_ID" to CREATED_TASK,
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
        "/api/objects/shopping_list/5" to MILK_SHOPPING_LIST_ITEM,
        "/api/objects/shopping_lists/1" to
            """{"id":1,"name":"Shopping list","description":"<p>Bring bags</p>"}""",
        "/api/objects/shopping_lists/2" to """{"id":2,"name":"Hardware store"}""",
        "/api/objects/stock" to STOCK_ENTRIES,
        "/api/objects/stock_log" to STOCK_LOG,
        "/api/objects/recipes" to RECIPES,
        "/api/objects/recipes/1" to PANCAKES,
        "/api/recipes/fulfillment" to RECIPES_FULFILLMENT,
        "/api/objects/recipes_pos" to RECIPE_INGREDIENTS,
        "/api/objects/recipes_pos/1" to PANCAKES_MILK,
        "/api/objects/recipes_nestings" to RECIPE_NESTINGS,
        "/api/objects/recipes_nestings/1" to
            """{"id":1,"recipe_id":2,"includes_recipe_id":1,"servings":"2"}""",
        "/api/objects/recipes_pos_resolved" to RESOLVED_RECIPE_INGREDIENTS,
        "/api/objects/meal_plan_sections" to MEAL_PLAN_SECTIONS,
        "/api/objects/meal_plan_sections/$CREATED_ID" to
            """{"id":$CREATED_ID,"name":"Dinner","sort_number":3,"time_info":"19:00"}""",
        "/api/objects/meal_plan" to MEAL_PLAN,
        "/api/objects/meal_plan/$CREATED_ID" to CREATED_MEAL_PLAN_ENTRY,
    )
}

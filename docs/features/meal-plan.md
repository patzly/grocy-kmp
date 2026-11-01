# Meal plan

Grocy's meal plan page (`/mealplan`) and its sections (`/mealplansections`), part of
`feature:recipes` (Grocy keeps it with the recipes and behind `FEATURE_FLAG_RECIPES_MEALPLAN`).

- Routes (`feature:recipes:api`): `MealPlanRoute(start)` (Grocy's `?start=`, the week of that
  day, today's without) and `MealPlanSectionsRoute`, both screens of their own: the week uses
  the whole window, no list-detail scene. The start screen opens it from its section "Meal
  plan" in the recipes group (with the recipes and meal plan features), which lists what is
  planned for today (`mealPlanTodayOf`) or "Nothing planned for today".
- Data (`MealPlanRepository` of `core:data`, database version 10): every entry of `meal_plan`
  (recipe with servings, product with its amount in the stock unit and the unit it was entered
  in, or note; section, done) and the sections (`meal_plan_sections` with `time_info`). Grocy
  computes costs, energy and fulfillment through recipes its triggers keep by themselves: one
  per day (`mealplan-day`, "YYYY-MM-DD"), per week (`mealplan-week`, SQLite's `%Y-%W`, Monday
  to Sunday) and a shadow recipe per planned recipe (`mealplan-shadow`, "YYYY-MM-DD#<entry
  id>", at the entry's servings). `RecipesRepository.refresh` downloads them
  (`objects/recipes?query[]=type!=normal`) with their `recipes/fulfillment` rows into
  `meal_plan_recipes`; nothing is computed locally. Every change is online only and downloads
  entries and those recipes again (`afterChange`, a failed download never fails the change).
  Normal recipe lists filter the type on the client too.
- Weeks (`mealPlanWeekStart`): Grocy's `MEAL_PLAN_FIRST_DAY_OF_WEEK` (0 Sunday to 6, -1
  today's weekday), else `CALENDAR_FIRST_DAY_OF_WEEK`, else the language's first day
  (`localeFirstDayOfWeek`: ICU on Android, `Intl.Locale.getWeekInfo` in browsers, Monday
  without). The week recipe is the one of the shown week's middle day, which shares most days
  with Grocy's Monday weeks. The pure `buildMealPlanWeek` builds the week: per day its entries
  by section order (Grocy's default section first), then as planned; entries of deleted
  recipes or products are left out like Grocy does, entries of a deleted section show in the
  default one.
- Screen (`MealPlanScreen`, `MealPlanViewModel`): the week item first (date range as heading,
  "Week costs" with price tracking, with the stock feature Grocy's "Put missing products on
  shopping list" of the week, off while the shopping list covers it, and "Consume all
  ingredients needed by this weeks recipes or products" after a question). The floating
  toolbar: previous week, today (off in the current week), next week; "Add recipe" as FAB, on
  today in the current week, else on the week's first day (`addDayOf`).
  - Narrow windows (below `showsWeekGrid`, at least 136.dp per day beside the margins: phones,
    foldables, tablets held upright): the days below each other, each a group of its header
    (weekday as heading, date, Grocy's day costs and "900 kcal per day" as chips, "Nothing
    planned"; today in primary container with "Today") and its entries; the section ("Breakfast
    · 7:00") is each entry's overline. The current week opens at today
    (`agendaIndexOfDay`), other weeks at their start.
  - Wide windows (tablets in landscape, desktops): Grocy's grid over the whole width, a column
    per day; the days' headers in one row as high as the highest, then per used section its
    name and each day's entries as cards (picture across the top, name, chips, menu in the
    bottom end corner).
  - Entries: the name, crossed out and muted once done; chips: a recipe's servings, its status
    like the recipes list (with the stock feature), costs (price tracking) and energy per
    serving; a product's amount in its stock unit, "Enough in stock"/"Not enough in stock"
    (aggregated stock), costs by its last price and its energy; the picture (recipe or product)
    before the menu. A tap shows the recipe at the planned servings (Grocy's "Display recipe"
    sets them as desired servings first, offline it opens as it is), the product overview or,
    for notes, their dialog. Swiping a row towards the end marks it done or undone, also in
    the menu and as accessibility action. The menu: Edit, Delete (at once like Grocy, the
    message offers "Undo", which creates it again); for recipes "Consume all ingredients needed
    by this recipe" (Grocy's question, consumes the shadow recipe, then the entry is done) and
    "Put missing products on shopping list" (the shadow recipe's missing products at the
    planned servings, without changing the recipe; off while covered); for products "Consume 2
    Pieces of Milk" (off while not enough, then done; "Undo" undoes the booking) and "Add to
    shopping list" (the product overview's dialog with the planned amount in the stock unit;
    off while enough is in stock); Mark this item as done/undone.
  - Each day's add button (`ListItemMenuButton`): Add recipe, Add product, Add note, then Copy
    this day (the day to copy to, every entry of the server's day once more, not done).
- Dialogs (`MealPlanDialogs.kt`, `MealPlanEntryDialogState`): "Add/Edit meal plan entry" with
  the type below the title; the day (date picker), then a recipe (it brings its base servings
  like Grocy's picker) and its servings, a product and its amount in one of its units (kept in
  the stock unit, the entered unit as `product_qu_id`; edits show the stock unit like Grocy), or
  a note; then the section ("None" for the default one).
- Sections (`MealPlanSectionsScreen`, `MealPlanSectionsViewModel`): "Configure sections" of the
  app bar's menu; Grocy's table without its internal default section: name, sort number and
  time; a tap edits in Grocy's form as dialog (name required, whole sort number from 0 with
  Grocy's hint, time with Material's time picker, "HH:MM" like Grocy's time input), the menu
  deletes after Grocy's question, "Add" creates one.
- Not taken over: Grocy's "Print" (the browser's print of the page).

# Recipes

Grocy's recipes page, its recipe card and its recipe form (`feature:recipes`).

- Routes (`feature:recipes:api`): `RecipesRoute(search)` (the list, a list-detail scene of its
  own like the shopping list, opened from the start screen's section and from the stock
  overview's "Search for recipes" with the product's name, Grocy's `/recipes?search=`),
  `RecipeRoute(id)` (the card) and `RecipeFormRoute(id)` (edit, create without id), both
  `DetailRoute`s beside the list from Material's expanded width on. The list opens cards and,
  through an item's menu, forms with `openDetail`; the card's "Edit" opens the form with
  `navigate`, so it takes the card's place beside the list and back returns to the card.
- Data (`RecipesRepository` of `core:data`, database version 9): Grocy's normal recipes
  (`objects/recipes?query[]=type=normal`, meal plan recipes are left out and filtered by type
  on the client too; they are stored for the meal plan, see `meal-plan.md`), what the server
  computes at their desired servings (`/recipes/fulfillment`, Grocy's `recipes_resolved`), the
  ingredients as defined (`recipes_pos`) and as resolved (`recipes_pos_resolved` of normal
  recipes, one row per recipe and ingredient, included recipes' ones with `is_nested`), the
  included recipes (`recipes_nestings`) and the pictures (`recipe_pictures`, 1200 pixels wide,
  unused ones deleted by a refresh). Nothing computed is derived locally; every change
  (servings, consume, missing products, ingredients, inclusions, the recipe itself, copy,
  delete) is online only and downloads everything again afterwards (`afterChange`; a failed
  download there never fails the change).
- List (`RecipesScreen`, `RecipesViewModel`): Grocy's tabs as list or gallery, switched by the
  floating toolbar's first button and kept like Grocy's last tab; then Scroll to top and
  Search; "Add" as FAB. The chips: Grocy's status filter (with the stock feature, the number
  of recipes per status in its menu), sort (the name and the shown fields but the picture),
  group by (none,
  desired servings, due score, requirements fulfilled, also when not shown; groups in the
  order of their value, recipes not computed yet last as "Not set"), the shown fields
  (`MultiSelectChip`) and "Clear filter". Fields, grouping, sort and view are
  `RecipesOptions` (`core:model`) in `SettingsStore`, read at app start by
  `RecipesOptionsRepository`, the same for list and gallery; defaults: due score, requirements
  and the picture (`RecipeField.PICTURE`, after Grocy's columns) shown, by name.
  The search is fuzzy over the name and the ingredients' products (Grocy's hidden column).
  The pure `buildRecipeList` sorts (requirements by status, then missing ingredients; ties by
  name), filters and groups.
- Items: a row has the name, the shown fields as chips that wrap (servings, "Due score: 11",
  the requirements) and at its end the picture as thumbnail before the menu, both centered to
  each other at the top of higher rows; a gallery card (as many per row as `galleryColumns` fits
  into the list's 640.dp, cards at least 156.dp: two on every upright phone, 8.dp apart in both
  directions; `extraLarge` corners) has the picture across its top with 16.dp padding and
  concentric corners (24 - 16 = 8.dp), the name below (body large, 16.dp above, 8.dp to the
  chips; chips wider than the card shorten their text with "…") and the menu in the bottom end
  corner; the cards of a row are as high as the highest, so their menus line up; without the
  picture field neither shows it. The
  requirements are status chips (`RecipeStatusChips.kt`, pure `statusChipsOf`), shared with the
  card: green (`positive`) "Enough in stock" with `priority`, yellow "2 ingredients missing"
  with `warning` while the shopping list covers them, then a blue "On shopping list" with the
  cart, otherwise red with `dangerous`; screen readers get Grocy's long text. Each has the
  narrow menu with Grocy's actions in three groups: Edit; "Put on shopping list" (Grocy's "Put
  missing products on shopping list", with stock and shopping list, off while enough is in
  stock), Add to meal plan (with the meal plan feature), Copy, Delete; Download recipe
  grocycode, Print recipe grocycode (with a label printer the server runs). Swiping a row
  towards the end runs "Put on shopping list" (off like the menu item; also an accessibility
  action of rows and cards; the gallery has no swipe). Beside a card or form the item shown there is marked (`ListDetailItem`, the card a
  selectable `Surface` with an outline).
- Actions (`RecipeActionsViewModel`, `RecipeActionsHost`, shared by list, card and form): copy
  (`/recipes/{id}/copy`, the copy's card opens like Grocy shows it), delete (Grocy's question,
  a shown card or form of the recipe closes), Grocy's meal plan dialog (day with today and the
  date picker, servings, section from `meal_plan_sections`, the default one named "None"),
  the grocycode `grcy:r:<id>` as PNG (`rememberGrocycodeDownload`) and on the label printer
  (`/recipes/{id}/printlabel`), and "Put on shopping list" without Grocy's question: all
  missing products (`add-not-fulfilled-products-to-shoppinglist`), but nothing is sent while
  the shopping list covers what is missing already, so repeating it never adds anything twice.
- Card (`RecipeScreen`, `RecipeViewModel`): "Recipe" as title, the picture right below the app
  bar, as wide as the content and fully rounded (`CircleShape`, 2:1); then the recipe's name
  (headline small, a heading) with the requirements as status chips like the list's, Grocy's
  "Desired servings" (a number field with stepper, filled before the card shows so its label
  never jumps, saved once typing pauses like Grocy's field, the server scales the ingredients
  anew; meanwhile a loading indicator at the end of the name's item, `isSavingServings`), energy per serving and costs as chips (with price tracking; warning color while
  prices are incomplete; a tap on the costs opens `CostsDialog` with Grocy's notes). The ingredients
  follow by Grocy's ingredient group and, with the user setting, product group (pure
  `buildRecipeSections`): "2 Pieces Milk" (Grocy keeps amounts in the stock unit and the unit
  only shows them, unless "Only check if any amount is in stock"), the variable amount instead
  of amount and unit, and chips: the stock status like the recipe's ("Enough in stock", or
  "2 Pieces missing" and after it "1 Piece on shopping list" while some of it is there), due soon/overdue/expired like Grocy's highlighted names, the
  sub product used for a parent out of stock, "Variable amount", energy, costs; the note above
  them. A tap opens the product overview. With Grocy's ingredient checkbox setting a checkbox
  crosses an ingredient out, kept in the ViewModel only. The preparation (HTML as text)
  follows, then each included recipe (resolved for this recipe, by name) with its ingredients
  and preparation. The toolbar: "Consume all ingredients" as FAB (Grocy's question and hint;
  without the stock feature Edit is the FAB), "Put missing products on shopping list" (Grocy's
  question with every missing product as checked choice, unchecked ones are excluded; off while
  the shopping list covers what is missing, like Grocy), Edit, the preparation mode (`cooking`),
  Scroll to top; the app bar's menu has the recipe actions above the app menu.
- Preparation mode (`RecipePreparationRoute`, `RecipePreparationScreen` with its own
  `RecipeViewModel`): the card for cooking in a screen of its own like the shopping mode, a
  close button, "Preparation mode" as title, no toolbar or FAB. The overflow menu only holds the
  checkable "Keep screen on", on whenever the mode opens, until it is closed (ViewModel state,
  never saved). From Material's expanded width (840 dp, `showsSecondColumn`) the picture,
  servings and ingredients form the left column and the preparations the right one, each
  scrolling on its own (`SegmentedListScreen(secondContent)`); narrower screens follow the
  card's order.
- Form (`RecipeFormScreen`, `RecipeFormViewModel` on `ObjectFormViewModel`): name, servings
  (Grocy's hint), "Do not check against the shopping list" (with the shopping list feature),
  "Produces product" (with the stock feature), the preparation (plain text, an unchanged one
  keeps its HTML), the userfields of `recipes` ("Configure userfields" in the toolbar), then
  Grocy's right column: the ingredients list and the included recipes (tap edits in a
  `FormDialog`, the menu deletes after Grocy's question; "Add" is off with a hint until the
  recipe is saved, a created recipe becomes its edit form like "Save & continue"), the picture
  (`PictureDialog` of `core:forms`, shared with the product form, applied on saving) and the
  grocycode. The form has no copy or delete, like Grocy's (`objectActionsOf`).
- Ingredient dialog: product, "Only check if any amount is in stock" (then every unit and
  the amount as typed), amount (0 allowed otherwise, like Grocy's picker) and unit (the units
  the product's stock unit converts into, its stock unit when a product is chosen; the amount
  is saved divided by the unit's factor and rounded to the decimal places, at least one, like
  Grocy's picker), variable amount, round up (sent only where the server knows it, Grocy 4.4,
  or when switched on), "Disable stock fulfillment checking" (preset from the product for new
  ones), group, note, price factor (with price tracking). Included recipe dialog: another
  recipe and its servings.
- Start screen: "Recipes" as a group of its own between stock and master data (with the
  recipes feature), telling how many recipes there are and, with the stock feature, how many
  have enough in stock, the rest on the shopping list, or not enough (`recipesSummaryOf`).

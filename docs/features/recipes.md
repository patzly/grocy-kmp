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
  (`objects/recipes?query[]=type=normal`, meal plan recipes are left out), what the server
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
  of recipes per status in its menu), sort (the name and the shown fields), group by (none,
  desired servings, due score, requirements fulfilled, also when not shown; groups in the
  order of their value, recipes not computed yet last as "Not set"), the shown fields
  (`MultiSelectChip`) and "Clear filter". Fields, grouping, sort and view are
  `RecipesOptions` (`core:model`) in `SettingsStore`, read at app start by
  `RecipesOptionsRepository`; Grocy's defaults: due score and requirements shown, by name.
  The search is fuzzy over the name and the ingredients' products (Grocy's hidden column).
  The pure `buildRecipeList` sorts (requirements by status, then missing ingredients; ties by
  name), filters and groups.
- Items: a row has the name and the shown fields as chips (servings, "Due score 11", the
  status with Grocy's icon: check, exclamation in warning, cross in critical); a gallery card
  (as many per row as `galleryColumns` fits, cards at least 260.dp, up to four) has the picture
  across its top, name and chips below. Each has the narrow menu with Grocy's actions in three
  groups: Edit; Add to meal plan (with the meal plan feature), Copy, Delete; Download recipe
  grocycode, Print recipe grocycode (with a label printer the server runs). There is no swipe
  action. Beside a card or form the item shown there is marked (`ListDetailItem`, the card a
  selectable `Surface` with an outline).
- Actions (`RecipeActionsViewModel`, `RecipeActionsHost`, shared by list, card and form): copy
  (`/recipes/{id}/copy`, the copy's card opens like Grocy shows it), delete (Grocy's question,
  a shown card or form of the recipe closes), Grocy's meal plan dialog (day with today and the
  date picker, servings, section from `meal_plan_sections`, the default one named "None"),
  the grocycode `grcy:r:<id>` as PNG (`rememberGrocycodeDownload`) and on the label printer
  (`/recipes/{id}/printlabel`).
- Card (`RecipeScreen`, `RecipeViewModel`): the picture right below the app bar, as wide as
  the content and fully rounded (`CircleShape`, 2:1); then the requirements chip, Grocy's
  "Desired servings" (a number field with stepper, saved once typing pauses like Grocy's field,
  the server scales the ingredients anew), energy per serving and costs (with price tracking;
  warning color and note while prices are incomplete, Grocy's hint below). The ingredients
  follow by Grocy's ingredient group and, with the user setting, product group (pure
  `buildRecipeSections`): "2 Pieces Milk" (Grocy keeps amounts in the stock unit and the unit
  only shows them, unless "Only check if any amount is in stock"), the variable amount instead
  of amount and unit, and chips: the stock status ("Enough in stock (3 Pieces)" or Grocy's
  missing and on-list amounts), due soon/overdue/expired like Grocy's highlighted names, the
  sub product used for a parent out of stock, "Variable amount", energy, costs; the note above
  them. A tap opens the product overview. With Grocy's ingredient checkbox setting a checkbox
  crosses an ingredient out, kept in the ViewModel only. The preparation (HTML as text)
  follows, then each included recipe (resolved for this recipe, by name) with its ingredients
  and preparation. The toolbar: "Consume all ingredients" as FAB (Grocy's question and hint;
  without the stock feature Edit is the FAB), "Put missing products on shopping list" (Grocy's
  question with every missing product as checked choice, unchecked ones are excluded; off while
  the shopping list covers what is missing, like Grocy), Edit, Scroll to top; the app bar's menu
  has the recipe actions above the app menu.
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

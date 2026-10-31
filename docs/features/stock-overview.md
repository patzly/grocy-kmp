# Stock overview

Grocy's stock overview page. Moved from CLAUDE.md.

Stock overview (`stockoverview/` of `feature:stock`, it opens the product overview of
`core:productcard`):
- `StockOverviewScreen` is Grocy's stock overview page: the active products not hidden there
  that are in stock or below their min. stock amount (every active one with the user setting
  `stock_overview_show_all_out_of_stock_products`). Grocy computes the page in the view
  `uihelper_stock_current_overview`, which the API does not expose, so
  `StockRepository.refreshOverview` downloads its parts as the server computed them: `/stock`
  (amounts, opened amounts, value, next due date; `StockAmount`), the missing products of
  `/stock/volatile`, `products_last_purchased`, `products_average_price`,
  `stock_current_locations` and the products on shopping lists (`product_stock_info` and
  `stock_locations`, database version 6); the pure `buildStockOverview` only joins, filters,
  sorts and groups them, like Grocy's page does in its template and JS (due status from the
  date: `dueStatusOf` of `core:model`, unknown due dates sort as Grocy's 2888-12-31, never
  overdue last).
- Below the title (`SegmentedListScreen(subtitle)`, onSurfaceVariant) Grocy's "%s Products,
  %s total value". The filter chips: status first (with the number of products of each in its
  menu instead of Grocy's colored messages; "All", then a group with "not fresh" (due soon,
  overdue or expired, not in Grocy), due soon, overdue, expired, then below min. stock amount,
  in stock and opened (not in Grocy); `DropdownFilterChip(optionGroups, selectedLabel)`), then
  location and product group, then Grocy's table options: sort (the name and the shown fields
  but the picture), group by (Grocy's groupable columns; groups as group titles, rows without a
  value last as "Not set") and the shown fields (`MultiSelectChip`, never selected: it chooses
  the look, it filters nothing; only fields of features the server has, `availableFields`). Fields, grouping and sort are kept like Grocy keeps its table
  options (`StockOverviewOptions` of `core:model` in `SettingsStore`, read at app start by
  `StockOverviewOptionsRepository`); hiding the field the list is sorted by sorts it by Grocy's
  default again (next due date, the name while that is hidden). Search like the products.
- Each product is a segmented item: the name as title (Grocy's cart icon at 18.dp in the
  harmonized blue after it while on a shopping list, centered on the title, read as text like
  the chips: a description would replace the item's name for screen readers), the shown fields
  as chips that wrap below it
  (`FieldChips`: surfaceContainer, no outline or elevation; the amount blue below the min. stock
  amount, the amount with sub products with the "functions" icon as Grocy's sigma, the next due
  date relative and capitalized as a label of its own (`formatRelativeTimeLabel`: "Tomorrow",
  "Morgen"; ICU and Intl write the middle of a sentence), yellow when due soon and red when
  overdue or expired: `rememberStatusColors` of
  `core:design`, Material's custom color roles of fixed hues harmonized with primary), the
  description as text, the picture as thumbnail. A tap opens the product overview. Grocy's
  buttons in front of a row are swipe actions (`SwipeActionBox` of `core:design`, like Gmail's: a
  rounded primary surface opens from the screen's edge and uncovers the icon, which rests 16.dp
  from that edge until the surface's middle reaches it and stays in the middle then; passing the
  threshold ticks; letting go beyond it runs the action and the item springs back instead of
  being swiped away, so no extra tap): towards the end "Consume" (the quick consume amount),
  towards the start "Mark as opened" (the quick open amount, tertiary instead of primary, so
  both sides differ: `SwipeAction(containerColor, contentColor)`); both are also the item's
  accessibility actions and the first menu group (with "Consume all" and "Consume as spoiled"),
  then "Add to shopping list" and "Search for recipes", then "Edit product"; actions that are
  not possible are disabled (`AppBarMenuItem(enabled)`), like Grocy's buttons.
- The bookings work like Grocy's row buttons (`StockRepository.consumeProduct`/`openProduct`:
  `allow_subproduct_substitution`, amounts in the stock unit, "Consume all" the own amount, a
  parent without own stock its sub products, the tare weight with tare weight handling), online
  only; Grocy's message with "Undo" undoes the whole transaction (`undoTransaction`), and the
  overview is downloaded again afterwards. A failed download after a booking never reports the
  booking as failed (it would invite booking twice), the next refresh gets it.
- The floating toolbar: Journal, Stock entries, Reports (with location or price tracking) and
  Search, scanning as its FAB. Reports, scanning and recipes do nothing yet, their pages do not
  exist.

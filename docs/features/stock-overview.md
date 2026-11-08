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
  `core:design`, custom color roles of fixed hues, `docs/ui.md`), the
  description as text, the picture as thumbnail before the menu (both centered to each other,
  at the top of rows higher than one line, like the recipes' rows). A tap opens the product overview. Grocy's
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
  Search, scanning as its FAB. Reports opens a menu right above the toolbar (`ReportsMenu`,
  like the calendar's views): "Location Content Sheet" with location tracking, "Spendings" with
  price tracking (see below). The FAB opens the scan page (`StockScanRoute`, below);
  "Search for recipes" opens the recipes searched for the product's name (`RecipesRoute`).

## Stock reports

- "Location Content Sheet" (`LocationContentSheetRoute`, `reports/LocationContentSheet*`):
  Grocy's `/locationcontentsheet` without its printing. A group per location (by name, NOCASE),
  its active products by name (binary order like Grocy's `ORDER BY p.name`) with amount and
  stock unit and "x opened" below; "Show only in stock products" as toggle chip, on by default,
  off adds the products without stock at their default location (`locationContentOf`,
  core:model). Computed from the stored stock entries, which it downloads again on open (the
  amounts per location and the opened part are sums of the entries, like Grocy's
  `stock_current_location_content`). A tap opens the product overview.
- "Spendings" (`StockSpendingsRoute`, `reports/Spendings*`): Grocy's
  `/stockreports/spendings`. Chips: "Group by" product, product group or store; the date
  range with Grocy's presets (today ... last year, default this month, `SpendingsRange`) and
  "Custom range" with Material's date range picker; the product group (All, the groups,
  Ungrouped) only while grouped by product; "Clear filter". Below a doughnut of the totals with
  the total in the middle, then Grocy's table as list (name, product group, total, share).
- Grocy's API does not expose its `products_price_history` view, so the app rebuilds it from
  the stock journal (`pricedPurchasesOf`, core:model, after migrations 0224/0230/0239): purchases,
  inventory corrections and self-production with a price and amount above 0, not undone; an
  edited entry counts once, with its newest `stock-edit-new` (price, date) and the amount it then
  had at the start (that edit's amount plus what was consumed or corrected away before). Self
  production costs nothing in the spendings, like Grocy's `transaction_type != 'self-production'`.
  `StockRepository.pricedPurchases(from, to)` reads the bookings of the range
  (`purchased_date` filters), all `stock-edit-new` bookings and, for entries whose newest edit
  lies in the range, all bookings of those entries (at most 4 requests at once). Online only:
  every range is read anew, grouping and filters need no request; a failure shows the list's
  error with "Try again", pull to refresh reads again.
- Chart colors: the theme's chart colors (`rememberChartColors`,
  `docs/features/product-overview.md`), assigned by size; past 8 rows the smallest share one gray slice "Other" (`spendingsChartOf`),
  so no color repeats. 2dp of the container's color
  between slices. Tapping a slice or a row shows that slice's total and name in the middle and
  dims the others; the list is the chart's table view, the doughnut one image to screen readers
  with its total.

## Scan page

- The stock overview's FAB (`StockScanRoute`, `scan/StockScan*`, also `grocy://scan`): scanning
  first, the booking afterwards. While waiting for a code the camera is on, with a field below
  for typed codes or a barcode reader's Enter ("Barcode, grocycode or product name").
- A code (`scannedCodeOf`): a barcode or grocycode like on Grocy's stock pages, else an active
  product's exact name; otherwise "Unknown code". The item shows the product and its stock
  amount, with "Scan again". Below "What do you want to book?" as button group of the bookings
  it allows (`offeredKinds`): consume and transfer only what is in stock, transfer only with
  location tracking, an unknown code all of them (consume and transfer can only assign it, like
  Grocy). A single possible booking is chosen right away.
- The chosen booking is the page of that kind (`BookingViewModel` with `BookingPreset.code`, the
  code used like one scanned in its product field, barcode amount and unit included) below,
  without Grocy's scan mode switch and without the product field once a product is chosen; the
  FAB books it. Unknown codes get Grocy's "Create or assign product" of that page.
- Each booking page lives in a `ViewModelStore` of `StockScanViewModel`, so it survives rotation;
  choosing another kind replaces an unbooked one. After a booking the camera returns for the next
  code; the booked page stays until the next is chosen, so its message's "Undo" still works.

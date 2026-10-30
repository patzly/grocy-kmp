# Start screen

What is built so far and how the start screen works. Moved from CLAUDE.md.

Current state: login (demo server, QR code of Grocy's API key page, manual Grocy URL + API key,
Home Assistant ingress on Android), the reusable barcode scanner (`core:scanner`), update checks
on every platform, the start screen (on Grocy's public demo first its warning as a group of its
own, `DemoServerWarning` of `core:design` like the server settings show it, a tap opens them
through the settings categories; then sections grouped by `StartGroup`: the stock overview (only
with the stock feature, `StartSection.isAvailable`) and the shopping list (also with the
shopping list feature) as one group, then Master data, each telling about
its data as supporting text (`SupportingBulletList` of `core:design`, numbers of 0 left out;
`StartViewModel` downloads master data and stock overview when it opens): the products in stock
and, with prices, their value, then as list Grocy's messages above its stock overview (due
within the due soon days, overdue, expired, below min. stock amount with how many of them are
on shopping lists; `stockSummaryOf` of `core:model`, counted like Grocy's messages; below them
Grocy's Purchase, Consume, Transfer (with location tracking) and Inventory as a standard button
group of wide tonal icon buttons, `StockActionButtons`, the last ones in the menu of a tonal
overflow button when the item is too narrow), the items still to buy (with several lists
holding some, per list as "Name (count)"; below it Grocy's "Add item" as one wide tonal icon
button, a plain plus, opening `ShoppingListItemRoute` for the list the shopping list
shows or Grocy's default list, `StartViewModel.quickAddListId`), and the
objects of each master data category the server has in one line joined by commas
(`MasterDataCount`, `start_list_join`); the settings open as their own scene from the app
menu like on every other screen (Settings above About); from
Material's expanded width on a list-detail scene like the settings: `StartSection`, the
section's page (`StockOverviewRoute`, `MasterDataRoute`, both `DetailRoute`s) opens beside it,
`start_select_hint` until then; the shopping list is a list-detail scene of its own and opens
on its own (`StartSection.route` is no `DetailRoute`), and a category of the master data still
opens its list as a scene of its own with its forms beside it, never in place of the master
data; back leaves the whole scene like every list-detail scene, from the start screen the app,
it never closes the page beside it first, on purpose), an About
screen, the Settings overview (`feature:settings`), Grocy's stock overview and the master data
(`feature:masterdata`): the overview of Grocy's master data categories, the list and form of
every category (products with their overview dialog, locations, stores, quantity units,
product groups, chores, batteries, task categories, userfields, userentities), with the
product's barcodes, grocycode and conversion overrides, the default conversions of units,
Grocy's stock pages of products (stock entries, stock journal, journal summary), "Add to
shopping list", the stock settings ("Presets for new products"), Grocy's stock bookings
Purchase, Consume, Transfer and Inventory (see `stock-bookings.md`) and Grocy's shopping list (see
`shopping-list.md`).

The start screen's items use the segmented item's default text styles for title and supporting
text. Items with buttons below their text (`SectionButtons`) keep 16.dp between the visible
buttons and the item's bottom edge (`buttonsContentPadding`; Material's bottom padding is too
tight there): the room a standalone button keeps for its touch target counts towards it
(`ShoppingListAddButtonRoom`).

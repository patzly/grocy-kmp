# Stock bookings

Grocy's Purchase, Consume, Transfer and Inventory pages. Moved from CLAUDE.md.

Stock bookings (`booking/` of `feature:stock`, routes `PurchaseRoute`, `ConsumeRoute`,
`TransferRoute`, `InventoryRoute` of `feature:stock:api`, `bookingRoute(BookingKind)` for one
of them, from the start screen's buttons and the product overview's menu):
- One `BookingViewModel(kind)` and `BookingScreen` for Grocy's four pages, every field of
  Grocy's form in its order as segmented groups (`BookingForm`), hidden by feature flag and
  user setting like Grocy; the booking is the FAB, "Mark as opened" of the consume page a
  toolbar button. The pure logic is tested: `bookableProducts` (purchase and inventory: active
  with own stock; consume: in stock, sub products included; transfer: own stock in stock),
  `matchCode` (barcode, grocycode `grcy:p:<id>[:<stock_id>]` of `ProductGrocycode`, a known
  product the page does not offer is "not in stock", an unknown code asks Grocy's "Create or
  assign product": add as barcode to the product chosen next, or as new product, the barcode
  is saved with the booking), `pricePerStockUnit` (unit or total price, the tare weight left
  out), `purchaseDueDate` (the product's default only into an empty field, a freezer's over
  it), `amountLimits`/`validateBooking` (Grocy's number picker rules: smallest step, stock at
  the location or the chosen entry as maximum, parents without maximum, gross weight above
  container and stock, inventory not the current stock), `inventoryChange`,
  `consumeLocation`/`transferSource`.
- Prefills like Grocy's pages when a product is chosen: units of `productAmountUnits` (purchase
  unit, consume unit, stock unit for transfer, inventory and tare weight), default amounts of
  the user settings, store, location, due date, price from the last price, label type and
  price type of the product, a barcode's amount, unit, store, last price (as total) and note;
  the product's stock is loaded from the server (`StockRepository.productStockDetails`,
  `productLocationAmounts`, `productStockEntries`; online only, offline the product shows the
  offline message and nothing can be booked). Consume and transfer list only locations with
  stock, the product's default one marked like Grocy's "(Default location)", and its entries
  for "Use a specific stock item" (Grocy's text of an entry); a stock entry's grocycode chooses
  that entry with its whole amount.
- Bookings (`StockRepository.purchase/consume/open/transfer/inventory`, Grocy's endpoints,
  amounts and prices sent in the stock unit) show Grocy's message with "Undo" (undoes the
  transaction) and its warnings (due earlier than the stock, frozen, thawed, should not be
  frozen, moved on opening); userfields of `stock` are saved under the transaction like
  Grocy's pages (the server moves them to the new entries), the used barcode gets the typed
  price as last price. The form resets like Grocy's page (each keeps what Grocy keeps).
- Grocy's scan mode (user settings `scan_mode_purchase_enabled`/`_consume_enabled`, a switch
  on the page): a scanned product is booked with one unit (a barcode's amount stays) once every
  field is filled, the scanner stays open; otherwise Grocy's message.
- From a shopping list item (`PurchaseRoute(productId, amount, quId, shoppingListItemId)`) the
  purchase opens with the item's amount in its unit, books itself with
  `shopping_list_to_stock_workflow_auto_submit_when_prefilled` when complete, removes the item
  (`ShoppingListRepository.deleteItem`) and goes back. A note in `secondaryContainer` at the top
  (`BookingViewModel.shoppingListItem`) says what happens: Grocy's "Add this item to stock", or
  "Adding shopping list item %s of %s" for "Add all list items to stock"
  (`shoppingListPosition`, `shoppingListCount` of the route), and that booking removes the item.

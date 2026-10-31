# Product overview

Grocy's product overview card with what it opens (`core:productcard`): the products, the stock
pages and the shopping list show it through `ProductOverviewHost`, whose ViewModels
`rememberStockPageActions` creates for a page; `ProductLinks` say where its buttons lead.

- `ProductOverviewDialog` is Grocy's "Product overview" card, opened from the first group of
  each active product item's overflow menu, the stock pages and the shopping list (Grocy
  computes no overview for disabled products). The title is "Product overview"; on top of the scrolling
  content the card header's buttons as an expressive `ButtonGroup` (`animatedItem`, 12.dp
  apart, it always fits): Edit product and Add to shopping list (only with the shopping list
  feature) as wide filled (primary) icon buttons, then the narrow filled menu button
  (`FilledOverflowMenu`, `primary` like them) with Stock entries, Stock journal and Stock
  journal summary, then Grocy's Purchase, Consume, Transfer (with location tracking) and
  Inventory for the product, then "Download Product Grocycode" and "Print Product Grocycode on label
  printer" (only when the server runs the webhook; `OverviewActions`, the host prints through
  the screen's `LabelPrintViewModel`). Then every
  value of the card in its order, all as items with the label as text and the value as
  supporting text, the product name first in the stock group (with the loading indicator of a
  download over stored values), hidden like in Grocy by feature flag (prices: stock value, last/average price and
  price history; location: default location; best before dates: average shelf life),
  description (three lines, "Show more"), picture (1200 px, offline the stored list picture)
  and the price history. `ProductOverviewViewModel` is one per product list (`show`/`hide`, kept
  across rotations), every opening downloads `/stock/products/{id}` plus its price history
  (`MasterDataRepository.refreshProductOverview`, stored as JSON in `product_overviews`, so the
  last overview shows offline). The pure `buildProductOverview` resolves units (prices per price
  unit, per stock unit too when they differ), stores and the chart series.
- `PriceHistoryChart`: Canvas line chart after the dataviz rules: one 2.dp line per store with
  surface-ringed points, Grocy's dotted least squares trend, y axis from zero in round steps
  (`priceAxis`), first and last date below, legend from two series or with a trend. Colors come
  from `rememberChartColors` (`core:design`): a validated categorical palette in fixed order
  with light and dark steps (the tests check it with the dataviz validator, `PaletteValidator`),
  starting at its color closest to primary (the order stays), every color harmonized with
  MaterialKolor's `harmonize` like the status colors and Material's custom colors. A search for
  the strongest harmonization that kept the validator's color blind checks ended without any
  for most themes, so the colors stayed unharmonized; harmonized hues can come close for color
  blind users instead, so every series also has its own point shape (`ChartMarker`: circle,
  square, diamond, triangle; neighbours never share one), in the legend too. Every other color
  outside the color scheme must be
  harmonized with primary as well (codes such as the grocycode stay black on white); stores
  keep their color by id,
  stores beyond eight are drawn together as "Other stores". Tap, sideways drag, mouse hover and
  the arrow keys select a price (date, store, price as tooltip); screen readers get all prices.
- Grocycodes (`GrocycodeItem`): `grcy:p:<id>` (`:<stock_id>` for stock entries) written by
  `core:scanner`'s own `DataMatrixWriter` (ECC 200, square up to 48x48) or `Code128Writer` for
  Grocy's `GROCYCODE_TYPE` 1D (`ServerConfig.grocycode2D`), drawn black on white; "Download"
  saves `Grocycode.png` (`PngWriter`, stored deflate blocks) through `rememberFileSaver` of
  `core:ui` ("Save as" on Android, a download in browsers). "Print on label printer" calls
  Grocy's `printlabel` and shows only when the server runs the webhook itself
  (`labelPrinterRunsOnServer`), the app never stores webhook addresses.
- "Add to shopping list" (`AddToShoppingListViewModel`/`AddToShoppingListHost`, from the
  overview and the stock pages): Grocy's form with `updateexistingproduct`, the amount in the
  product's units sent in the stock unit (`stock/shoppinglist/add-product`), the item's
  userfields saved afterwards on the item found by product and list (Grocy forgets them).

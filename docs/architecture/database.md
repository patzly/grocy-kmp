# Database details

Schema history and the web database driver. Moved from CLAUDE.md.

- Version 4 added chores, batteries, task
  categories, userentities, users and the userfield values of the simple objects; version 5
  quantity unit conversions (defined and resolved), shopping lists, stock entries, the stock
  journal and the userfield values of barcodes; version 6 the stock overview (opened amounts,
  value and next due date of `stock`, `product_stock_info`, `stock_locations`); version 7 the
  shopping list items, the lists' description and userfields and the queue of offline check
  offs (`pending_shopping_list_done`, `ShoppingListDao`), an AutoMigration.
- Android: `BundledSQLiteDriver`, file `grocy.db`. Web: `WebWorkerSQLiteDriver` with the worker
  `core/database/web-worker/worker.js` (npm package `grocy-sqlite-worker` on top of
  `@sqlite.org/sqlite-wasm`, androidx's message protocol). It stores the database in OPFS with
  the "opfs-sahpool" VFS, which needs no COOP/COEP headers (GitHub Pages); a second tab or a
  browser without OPFS gets an in-memory database that the app fills from the server again.
  Webpack bundles worker and `sqlite3.wasm`, `precache.json` lists them for offline use.

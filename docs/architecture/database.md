# Database details

Schema history and the web database driver. Moved from CLAUDE.md.

- Version 4 added chores, batteries, task
  categories, userentities, users and the userfield values of the simple objects; version 5
  quantity unit conversions (defined and resolved), shopping lists, stock entries, the stock
  journal and the userfield values of barcodes; version 6 the stock overview (opened amounts,
  value and next due date of `stock`, `product_stock_info`, `stock_locations`); version 7 the
  shopping list items, the lists' description and userfields and the queue of offline check
  offs (`pending_shopping_list_done`, `ShoppingListDao`), an AutoMigration; version 8 the queue
  of notes edited offline (`pending_shopping_list_notes`), an AutoMigration too.
- Android: `BundledSQLiteDriver`, file `grocy.db`. Web: `WebWorkerSQLiteDriver` with the worker
  `core/database/web-worker/worker.js` (npm package `grocy-sqlite-worker` on top of
  `@sqlite.org/sqlite-wasm`, androidx's message protocol). It stores the database in OPFS with
  the "opfs-sahpool" VFS, which needs no COOP/COEP headers (GitHub Pages); a second tab or a
  browser without OPFS gets an in-memory database that the app fills from the server again.
  Webpack bundles worker and `sqlite3.wasm`, `precache.json` lists them for offline use.

## Repositories and data rules

- Offline first: screens read `GrocyDatabase` through repositories (`MasterDataRepository` in
  `core:data`), `refresh()` downloads everything a screen shows in parallel and replaces the
  tables in one `withWriteTransaction`, so readers never see half an update. Changes are online
  only (`saveProduct`, `deleteProduct`, `saveLocation`, `saveChore`, ...: one `saveObject` per
  type posts or puts, sends changed userfields, then stores the object as the server returns it).
  `refresh()` downloads all master data at once; users need a permission of their own, a 403
  keeps the stored ones instead of failing the refresh.
- `AuthRepository` clears the database before a login and on logout (`clearLocalData`), so no
  screen shows the data of another server. The database file is excluded from backups.
- Server configuration and user settings live as JSON in `server_values`; only the known feature
  flags are kept (`ServerConfig`), the rest of `/system/config` (e.g. webhooks) never is stored.
- What only the server computes (product overviews: averages, spoil rate, price history) is
  stored as the last downloaded JSON per object (`product_overviews`, version 3), never derived
  from the other tables; a refresh deletes the ones of removed products.
- Room 3.0.x via the `grocy.room` convention plugin (KSP per target, schemas exported to
  `core/database/schemas`). Every schema change needs a new version and a migration; check it
  against the exported schema.
- Grocy sends numbers as numbers or strings depending on version: DTOs use `FlexInt`,
  `FlexDouble`, `FlexBoolean`, `FlexString`. Writes send JSON with exactly `application/json`
  (Grocy compares the header as string and rejects a charset) and get `GrocyApiException.
  HttpError.serverMessage` from Grocy's `error_message`. Browsers can write too: the preflight
  carries the API key in the query and Grocy answers it.

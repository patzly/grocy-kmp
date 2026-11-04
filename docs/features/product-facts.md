# Product databases

New products filled in from open databases by their barcode. Off until the user turns a database
on in the settings (Behavior), because barcodes then leave the device.

- Sources (`ProductFactsSource`): Open Food Facts, Open Beauty Facts, Open Pet Food Facts, Open
  Products Facts, all with the same API (`https://world.<domain>/api/v2/product/<code>.json`) and
  worldwide data; their country sites are filters of the same database. Asked in this order until
  one knows the code (`ProductFactsRepository.lookUp`), only retail codes of 8 to 14 digits
  (`isLookupBarcode`), so grocycodes and own codes never leave the device. Nothing but the code is
  sent, and no credentials (`ProductFactsApi`).
- Limits: 15 product reads per minute per user, so only an explicit lookup asks, one code at a
  time. The databases ask for a `User-Agent` naming the app; browsers decide about that themselves.
  All four answer with CORS headers, API and pictures, so the web app works the same.
- Licenses: data under the ODbL, pictures under CC BY-SA, said in the settings.
- Other databases are left out on purpose: those seen so far need an account or key, or their CORS
  headers only allow their own site, which would rule out the web app.

## When it runs

- A purchase or inventory scans an unknown code, "Add as new product": `ProductRoute(barcode)`
  looks up at once and adds the code as barcode with the new product.
- A new product's form offers "Look up barcode" first while a database is on
  (`ProductFactsDialog`: typed or scanned, a scan looks up at once, the camera opens only when
  asked for).

## What it fills in

Only what is still empty, nothing the user or their presets entered (`fillInto`):

- Name, in the app's language where the database has it.
- Picture: uploaded on save like one the user chose (`PictureChange.Replace`). Only from the
  database's own host (`isPictureOf`), a failed download only leaves the picture out.
- Energy (`energyPerStockUnit`), which Grocy keeps per stock unit: by the unit's name for grams,
  kilograms, milliliters, liters and the like, else one package. It follows the stock unit while
  the user has not typed their own.
- Description, where the rest goes that Grocy has no field for: kind, brand and package,
  ingredients, nutrients per 100 g or ml.
- Product group and default store, only when one of the user's has the same name as a category or
  store of the database.

Location and units cannot come from a database, the form asks for them as usual. The item above
the form says where the values came from and asks to check them.

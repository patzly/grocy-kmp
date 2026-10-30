# Navigation details

Entry decorators, the navigationevent version and the browser history of the web app. Moved from CLAUDE.md.

- `GrocyNavDisplay` gives every entry its own `LookaheadScope` (an entry decorator): a list
  moving out of the list-detail scene when the window gets narrower otherwise had its items ask
  the left scene's lookahead pass for text baselines, which threw (`NullPointerException` in
  `LookaheadDelegate`) and broke the layout.
- Android uses androidx `navigationevent` 1.2.0 (`apps:shared`, newer than the JetBrains 1.1.0
  of the other platforms): older versions crashed when a popup (an item's menu) closed during a
  predictive back gesture; `NavigationEventVersionTest` guards it.
- Web: `BrowserHistoryInput` (a `NavigationEventInput`, installed by `GrocyNavDisplay` through
  `BrowserBackIntegration`) mirrors the back stack into the browser history, one entry per
  screen (`SceneInfo`) plus one for an open dialog or popup. Browser back and the Android back
  gesture of the installed web app therefore go back inside the app and only leave it from the
  first screen. Forward moves are reverted. Every entry stores `{ index, stack }` (the back stack
  as JSON, `BackStackCodec`) and the URL shows the screen as hash: the route's `@SerialName` is
  its path, its arguments not at their default the query (`#/settings/general`,
  `#/masterdata/product?productId=5`, the first screen keeps the plain address;
  `RouteSerializersTest` checks paths are URL-like and unique). A reload restores the entry's back stack
  (`rememberNavigator`, only if it starts with the start route of the current login state) and
  stays on its entry, whose index stays until the app's history says where it is (the back
  stack's size does not tell, a two-pane scene shows two entries; guessing from it went back
  past the app's first entry and left the page); on a dialog's entry it returns to the screen below. A pasted URL without
  stored back stack starts on the first screen. When the first screen changes in place (login),
  no handler reports it, so `onBackStackChanged` rewrites the entry. A back the
  browser animated itself (`hasUAVisualTransition`) skips the app's pop transition. Browsers
  expose no gesture progress, so the web cannot run Compose's predictive back animation.
  From `beforeunload` until the next `pageshow` `WindowBrowserHistory` moves nothing: closing a
  dialog while the page reloads (language change) would otherwise cancel the reload.

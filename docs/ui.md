# UI details

Logo, scrollbars, button groups, pointer, web page, status bar and keyboard. Moved from CLAUDE.md.

- `GrocyLogo` (`core:design`, 30.dp by default, 100.dp above the login choices; `AppBarLogo` in
  the start screen's app bar, in the place of
  the navigation button the first screen has none of, the title stays centered) stacks the logo's layers (`ic_logo_*`, one color each) and
  tints them like the API 34 splash screen with the theme's roles instead of the system's
  (`onSurface` outlines, `primaryContainer` basket, `onPrimaryContainer` letter at 80 %), the
  fruits' fixed colors harmonized with primary (`grocyLogoColors`).
- Every vertically scrolling list, form, dialog and menu shows Material's
  `nonInteractiveScrollbar` through `Modifier.verticalScrollbar` (`core:design`): no track, a
  4.dp fully round thumb in Material's default `outline` at 70 % while scrolling, 4.dp from the
  end edge and as far from the top and bottom (`ScrollbarInset`), where the rounded corners of
  panes and menus cover it; `SegmentedListScreen` lays it between app bar and navigation bar
  (beside side cutouts), dialogs 4.dp from their edge between the dividers, also lists that
  scroll a dialog themselves (`dialogListScrollbar`, in the dialog's end padding). Material's
  scrollbar cannot be dragged, on purpose no own one that can.
- Button groups of icon buttons (`ButtonGroupItems.kt` of `core:design`): Material's 12.dp lie
  between the visible containers, the touch targets take no room in the group (48.dp targets
  of narrow 32.dp buttons would leave 16.dp at least): Compose's hit test extends small
  targets to 48.dp anyway and gives a touch in their overlap to the nearer button, each keeps
  about 46 x 48.dp of its own. Items go in with `animatedItem` (the pressed one grows by
  Material's ratio, its neighbours give way), icon buttons size themselves with
  `smallIconButtonSize(width)`: in an item they fill the width the group animates (Material's
  `TooltipBox` passes no minimum width on, so the button kept its size while only its slot
  grew), on their own they keep the 48.dp room; overflow indicators go in
  `ButtonGroupOverflowIndicator` (own width, hidden from screen readers until the group shows
  it: the web announced the unplaced indicator as empty button). The groups' outer edges keep
  the room of single buttons (`touchTargetRoom`), so they sit where single buttons would.
  The top app bar's actions are such a group too (`SegmentedListScreen(actions:
  List<AppBarAction>)` plus "Reload" and the overflow menu); a menu button in a group wraps
  button and popup in one `Box`, the popup's anchor would be an item of its own.
  `ConnectedButtonGroup` centers its overflow indicator in the touch target height of its toggle
  buttons: the group lines its items up at the top, so the smaller indicator sat higher.
- Overflow menus: `AppBarMenuItem(checked = ...)` is a checkable item (check mark while on, the
  menu stays open, `ToggleOn`/`ToggleOff` haptics). `SegmentedListScreen(refreshMenuItems)` puts
  items into the group of "Reload" (e.g. the shopping mode's auto sync), `onTitleClick` makes the
  title a choice (drop down arrow after it, the whole room between navigation icon and actions
  is one pill shaped target with the subtitle, always the small bar; e.g. Grocy's list selector).
- Chips: `LocalChipSize` (`core:forms`) provides `ChipSize.LARGE` for field and filter chips
  readable at arm's length (title medium, 40.dp high, 20.dp icons), e.g. the shopping mode.
- `KeepScreenOn(enabled)` (`core:ui`): the window's `keepScreenOn` on Android, a screen wake lock
  in browsers that offer one (requested again whenever the page becomes visible).
- Font is Google Sans Flex from Compose Resources. On web fonts load async;
  `rememberResourcesReady()` (also every string and icon: browsers load them asynchronously, so a new screen would fill in its texts while sliding in) gates `onContentReady`, which removes the Android splash / the HTML
  `#loading` element.
- `LocalPointerKind` (`core:design`, provided and tracked by `GrocyApp` with
  `trackPointerKind`): whether the user works with a mouse or trackpad right now. It starts with
  the device (`(pointer: fine)` in browsers, `FEATURE_PC` on Android) and follows the pointer of
  the last press or wheel turn; hover moves do not count, browsers emulate them after a tap.
- `ScrollableAlertDialog` closes on a tap beside it through `detectTapGestures`, not
  `clickable`, which would merge the dialog's pane title into one node (the web throws).
- "Recommend" (`rememberRecommendAction`) opens the system share sheet; where there is none
  (desktop browsers on Linux, `isTextSharingAvailable`) it shows `AppDialog.RECOMMEND` with the
  text and "Copy to clipboard" (`plainTextClipEntry` of `core:ui`).
- `GrocyApp` draws a `surfaceContainer` background below all screens, so fading transitions never
  show the window or page behind them. On the web `SystemBarAppearance` also sets the page
  background to `surfaceContainer`.
- System bars of the installed web app (Chrome on Android): a page cannot draw behind them, not
  even with `viewport-fit=cover`. Chrome paints the status bar in the page's `theme-color`, so
  `SegmentedListScreen` reports its app bar color (`StatusBarColorEffect`, the same
  `lerp(surfaceContainer, surfaceContainerHighest, FastOutLinearIn(collapsedFraction))` the large
  app bar uses) and `StatusBarColorRequests` writes the newest screen's color into the meta tags
  (`surfaceContainer` without app bar). The gesture bar takes the manifest's `theme_color`, which
  has no dark variant; `manifest.webmanifest` therefore has none, so the bar follows the
  system's light or dark mode. Never add `theme_color` back.
- Keyboard: `WindowInsets.keyboard` (`core:design`) is `WindowInsets.ime` on Android (edge-to-edge
  plus `adjustResize`, animated frame by frame). On the web `observeBrowserKeyboard` (called in
  `main()`) measures it from the visual viewport, without animation: Chromium and Firefox
  shrink the page above the keyboard themselves (`interactive-widget=resizes-content`), so
  nothing is covered and the app lays out anew; Safari only shrinks the visual viewport, the
  covered part applies at once. Browsers expose no keyboard animation; an own one (the old
  VirtualKeyboard API with `overlaysContent`) never matched it and let the page background
  flash above the keyboard, so never add one back. `SegmentedListScreen` pads the list by the keyboard and puts only
  the uncovered part of the navigation bar below the content (`ScreenContentPadding`, read
  during layout, so animations relayout without recomposing).
- Web page (`index.html`): `viewport-fit=cover` and `interactive-widget=resizes-content`; the
  page fills the screen (behind the gesture bar of an installed app) while `#composeApp` is
  fixed inside `env(safe-area-inset-*)`, because Compose on the web knows no window insets.
  Layout and colors of screens and dialogs: see `docs/design.md`.

## Components of `core:design`

- Colors of states outside the color scheme (Grocy's highlighted rows) come from
  `rememberStatusColors` (`core:design`): info (blue), warning (yellow), critical (red), fixed
  hues harmonized with primary and turned into container, content and color tones like
  Material's custom colors (90/10/40 light, 30/90/80 dark; `color` for small marks such as
  dots), always together with a text. Gestures on list items
  use `SwipeActionBox` (Gmail's look, the action runs on letting go beyond the threshold, the
  item springs back); every swipe action must also be in the item's menu and its accessibility
  actions.
- Reuse components from `core:design` (`SegmentedListScreen`, `segmentedGroup`,
  `SegmentedContainer`, `AppBarIconButton`, `InsetLazyColumn` + `insetItem` with max width
  640.dp, `RoundedTextField`, `ConnectedButtonGroup`, `NavigateBackButton`, `HapticTooltipBox`,
  `TextDialog`, `FormattedText`, `FloatingToolbarWithFab` + `FloatingToolbarIconButton` for the
  floatingToolbar slot of `SegmentedListScreen` (snackbars never cover it: they lie above it
  and follow it while it slides out and back in, `snackbarLift`), `SegmentedSwitchItem`,
  `SupportingLines` for every multi-line supporting content in lazy lists (Material sizes items
  by the slot's baselines, a reused column kept those of its former line count, so items stayed
  too high), `SupportingBulletList` for a text with a bulleted list in that slot (wrapping
  lines; `FormattedText`'s lists bring paragraph spacing a list item does not want), `SearchableDropdownField` for every choice of objects, a combobox like Grocy's pickers: the
  text is the value (a text naming an option chooses it, an empty one chooses nothing, other
  text shows "Choose an entry from the list" once no suggestion is open and is reported through
  `onUnmatchedChange`, so forms refuse to save); the menu only suggests (fuzzy, no check marks,
  optional fields offer "None"), opens through its arrow or while typing and closes on a choice
  or once the text names an option completely;
  a tap on the field or its trailing action (scan) only focuses, the arrow never does (choosing
  needs no keyboard); its menu is one expressive menu group in a `DropdownMenuPopup`, not
  `ExposedDropdownMenu`, whose old menu padding sits outside the scrolling and cuts the items
  off, and no `ExposedDropdownMenuBox` either: it focuses the field whenever its menu opens), units of text fields go in the
  `suffix` slot of `RoundedTextField`. `SegmentedSwitchItem` is Material's checkable segmented item: while on it takes the selected
  shape and color (`secondaryContainer`) of the segmented defaults. Clicks trigger `haptic.confirm()`. `HapticTooltipBox`
  puts its modifier on an outer `Box`, because Material's `TooltipBox` loses parent data such as `align`.
  Tooltips show on long press, mouse hover and keyboard focus, never after a plain tap or click:
  Compose on the web focuses a clicked button, so `TooltipPressTracker` tells a pointer focus
  (`TooltipTrigger.PointerFocus`) from keyboard focus and long press. Focus counts as keyboard
  focus only while the keyboard is in use (`rememberKeyboardInput`: Android's input mode, on the
  web the last of `keydown` and `pointerdown`), because a screen opened by a click focuses its
  first button (the back button) by itself.

## `FormattedText` markup

Long texts (changelog, licenses) are files in `composeResources/files/`, rendered by
`FormattedText`, which has its own markup: paragraphs split by blank lines, `#` headlines
(`#_` for tabular numbers), `__` subtitles, `**bold**` / `<b>`, `=> ` / `> ` link lines,
`? ` note and `! ` warning boxes in the size of the surrounding paragraphs (`bodyMedium` in
dialogs), `?- ` / `!- ` boxes with small text (`bodySmall`).

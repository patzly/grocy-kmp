# Master data

Master data lists and forms, list-detail scenes, menus, refresh, products, barcodes and
conversions. Moved from CLAUDE.md.

The building blocks it shares with the stock pages and the shopping list (fields, userfields,
`ObjectFormViewModel`/`ObjectFormScreen`, object actions, filter chips, list parts, `ChangeError`)
live in `core:forms`. The product overview with "Add to shopping list", grocycodes and label
printing, which the products, the stock pages and the shopping list open, lives in
`core:productcard` (`rememberStockPageActions` creates its ViewModels for a page).

Master data (`feature:masterdata`), built after Grocy's web frontend:
- `MasterDataScreen` (beside the start screen without back button and app menu, the start
  screen's bar has it) lists the categories of Grocy's "Manage master data" menu in its order and
  hides the ones whose feature flag is off (`MasterDataCategory.isAvailable`), from the eager
  `ServerRepository.serverConfig` (`MasterDataDependencies.serverConfig`), so they show in the
  first frame: on the web a refresh's write transaction keeps the single database connection
  busy for seconds while animations run, and reads wait behind it; every category shows its
  number of objects (`MasterDataCategory.count`, disabled ones included) once it is known (stored
  objects or a finished download), without holding back the categories.
- Lists and their forms are list-detail scenes of Navigation 3 (`listPaneMetadata` /
  `detailPaneMetadata` of `core:navigation`, Material's `ListDetailSceneStrategy` in
  `GrocyNavDisplay`): from Material's expanded window width on (840.dp, the default pane
  directive) the form opens beside its list, before a choice `DetailPlaceholder` (`core:design`)
  fills the detail pane with `objects_select_hint` (icon and text directly on the pane's
  `surfaceContainer`, no card); narrower windows show list and form one after
  the other. Both panes start half and half with Material's `VerticalDragHandle` between them
  (`PaneAnchors` 1/2, 1/3, 2/3: drag, the screen reader's action or the arrow keys, a tick per
  snap; `PaneBackgroundScene` measures the scene at least 48.dp wide, Material's scaffold throws
  when it clamps the handle into a window narrower than its margins, e.g. the one pixel window
  of headless browsers' screenshots); the pane expansion overrides the hinge avoidance, the middle lies on a book posture's
  fold anyway. The scene never moves the focus into its destination pane
  (`withoutAutoFocus`): browsers let items take the focus without keyboard, so after a reload
  the first item of the list showed the focus state layer (grey) until the first click. Both panes are `SegmentedListScreen(pane = ScreenPane.List / Detail)`: on a
  `surfaceContainerHighest` background like the scrolled app bars (`PaneBackgroundDecorator`, a
  `SceneDecoratorStrategy` of `GrocyNavDisplay` for two-pane scenes of lists, so it also fills
  the space between the panes) with 16.dp margin at the screen's start, end and bottom edges,
  each with a small app bar in its scrolled color (buttons included) on that background, and
  below it a `surfaceContainer` surface with
  `extraLargeIncreased` corners that holds the list, the background of single screens, so the
  content looks the same in every window size (`paneMargin`, `paneSurface`;
  `DetailPlaceholder` draws the same surface below the bar's height). Beside its list a form has no back button (the list's leaves both). Forms are `DetailRoute`s opened with `Navigator.openDetail` (replaces the form beside
  the list, and the details opened from it), lists leave with `goBack(from = route)`. A detail
  opened from a detail (`navigate`, e.g. the stock settings from Grocy's settings) takes its
  place beside the list and back returns to it (`NestedDetailSceneStrategy`: Material's scene
  goes back until its panes change, which left the list); Material's other option, an extra
  pane that pushes the list out, is for supplementary content of a detail, not for pages. A form with unsaved changes asks
  (`UnsavedChangesDialog`) also when another item of the list beside it is chosen or the list is
  left: `rememberLeaveGuard(route)` + `LeaveGuard.onLeaveRequest`. Beside a form
  (`showsDetailBesideList`) list items are single choice items marking the shown object
  (`ListDetailItem`), otherwise plain clickable items (no radio semantics in one pane).
- Overflow menus act on objects like Grocy's buttons and table menus (`ObjectActionsViewModel`,
  one per list and per form, `objectActionsOf`): every object can be deleted (Grocy's question,
  with its warning for products), products copied (`stock/products/{id}/copy`, the copy opens)
  and merged, chores merged (Grocy's merge dialog, the object preset as the one to keep, a
  refresh afterwards). Every list item has the narrow three dots button (`ListItemOverflowMenu`
  of `core:design`), products with "Product overview" as first group; forms put the actions as a
  group before Help and Send feedback (`SegmentedListScreen(actionMenuItems)`, the app menu
  without Settings and About: `rememberAppMenu()` without both). A deleted or merged away object
  closes its form. Forms get menu, leave guard, back and actions as one `FormFrame`.
- Date fields (date and datetime userfields, a chore's start date) have a calendar button
  (`DatePickerButton`): Material's date picker, for date and time then its time picker (dial or
  text input); the pure `withPickedDate`/`withPickedTime` write Grocy's format, typing still
  works. Material's time picker dialog fills the window's height beside its dial (landscape
  layout, e.g. desktop browsers), so it is capped at the specified 384.dp. The product's due
  days fields have one too (`DueDaysPickerButton`): a date from today
  on becomes the days until it (`dueDaysUntil`), e.g. the date printed on the first purchase.
- Number fields are Grocy's number pickers: minus and plus after the field in the same item
  (`NumberStepperField` of `core:design`, narrow filled `secondaryContainer` icon buttons in a
  standard `ButtonGroup`, `TextItem(stepper)`; 12.dp between the buttons, to the field and, the
  row reaching 4.dp into the item's end padding, to the item's end (see "Button groups" in `docs/ui.md`); the
  buttons are centered on the field, which
  grows with a wrapped label, so its error shows below the whole row through
  `NumberStepperField(error)` instead of the field's supporting text), stepping by one within Grocy's range (`NumberStepper`,
  `NumberStepper.Positive` for amounts above zero; the pure `steppedDecimal` of `core:model`
  keeps the typed decimals and separator, an empty field becomes 1 or 0 like Grocy's).
- The other categories share one list (`objects/`): `ObjectListKind` describes each like its
  Grocy table (columns in Grocy's order to sort by, first column as default sort, e.g. Entity for
  userfields, "Show disabled" where objects can be disabled, the entity of "Configure
  userfields"), `ObjectListViewModel` turns the database into `ObjectRow`s (shown texts and
  numeric sort values per `ObjectColumn`, enum labels through `ListTexts`, product count of
  groups, charge cycle of batteries) and the pure `buildObjectList` filters, sorts (empty values
  last, ties by name) and searches every column. Userfields filter by entity (Grocy's entity list:
  OpenAPI's exposed entities, `userentity-<name>`, users; `UserfieldsRoute(entity)` from every
  "Configure userfields"), userentities link to their fields (narrow icon button and the item's
  menu as a standard `ButtonGroup`, 12.dp apart, see "Button groups" in `docs/ui.md`). The filter chips
  (`FilterChipRow`, `DropdownFilterChip`, `ShowDisabledChip`, `SortChip`, `ClearFilterChip`;
  their menus scroll their groups together and leave room for the groups' shadows inside the
  scrolling, which clips at its bounds; each group draws its container and shadow only for its
  visible part, so the menu keeps its outer shape and shadow while only the items scroll, a cut
  off group ends at the edge with the menu's outer corners: `ScrollingMenuGroup`), the search
  item and the empty states (`ListParts.kt`)
  are shared with the product list; a list without search (stock entries) keeps the same gap
  below the chips (`filterGapItem`).
- Forms of the other categories build on `ObjectFormViewModel` (load, refresh for unknown ids,
  validation, unsaved changes, userfields of `userfieldEntity`, `ObjectFormEvent`s) and
  `ObjectFormScreen` (save FAB, Scroll to top, userfield group, leave guard). Fields and rules
  follow Grocy's forms, each with pure `XxxValues.validate` and conversion tested:
  `SimpleObjectViewModel` for locations (freezer only with freezing), stores, quantity units
  (plural forms only for languages with more than two, `grocyPluralFormCount` of the app
  language; default conversions as placeholder), product groups, batteries and task categories;
  `ChoreViewModel` (Grocy's first period and assignment type for new chores, start now, hidden
  period fields set like Grocy's JS, assignment only with `choresAssignments`, consumed product
  with stock; saving lets Grocy calculate the next assignment), `UserfieldViewModel` (entity and
  type start empty, options for preset types, "Now / today" for dates) and
  `UserentityViewModel`. Saved objects close the form, created ones become their edit form.
- List pictures are stored for offline use: `MasterDataRepository.productThumbnail` keeps them
  in `product_thumbnails` (Grocy gives every new picture a new file name, so they never get
  outdated), the product list downloads the missing ones after each refresh
  (`downloadMissingThumbnails`), and a refresh deletes those no product shows anymore.
- Every screen except About and the settings has the app menu in its overflow menu: Settings,
  About, Help and Send feedback (`appMenuItems()` of `core:ui`, Settings and About leading to
  the `LocalAppMenuLinks` of `GrocyApp`; forms use `rememberAppMenu` + `AppMenuDialogs` without
  them; the items go to `SegmentedListScreen(menuItems)`, which puts "Reload" as a group of its
  own before them).
  The feedback dialog reads the unlock state from `LocalUnlockState`, provided by `GrocyApp`.
  Ripples of grouped menus follow the group's corners through
  `menuItemShape(index, count, groupIndex, groupCount)`.
- Every list screen (start, master data, products) has pull to refresh through
  `SegmentedListScreen(isRefreshing, onRefresh)`: Material's own
  `PullToRefreshDefaults.LoadingIndicator`, sliding out from below the app bar, and "Reload" in
  the app bar's overflow menu (`AppBarOverflowMenu`, narrow three dots button) for keyboards and
  screen readers. With a mouse or trackpad (`LocalPointerKind`, below) pulling is off (a wheel
  only showed the indicator without refreshing) and "Reload" is a round icon button of the bar
  instead (only the three dots are narrow). These screens use the small centered app bar with `pinnedScrollBehavior` (no
  collapsing, which would fight with pulling), always in its scrolled color
  (`surfaceContainerHighest`), so the indicator visibly comes from below it; the others keep
  the large one, except while a mouse is used: its wheel has no gesture end the large bar could
  settle on, so every screen gets the small centered bar then, changing only its color.
  Menus opened from list items get `LocalMinimumInteractiveComponentSize` back to 48dp
  (`MenuTouchTarget`): a list item's trailing slot lowers it by its padding and the popup
  inherits it, so the items were 44dp with tighter padding than the app bar's menus.
  Single items of a menu group use `menuItemShape(0, 1)`: Material's standalone item shape has
  the small corners of middle items, so its ripple would not fill the rounded group. ViewModels run downloads through
  `RefreshController` (`core:data`: one at a time, offline/failed errors, `loadedOnce`);
  `RefreshErrorEffect` (`core:ui`) shows a failed download as snackbar.
- `ProductsScreen`: one segmented item per product (`itemSpacing = SegmentedGap`, a product with
  picture shows it at the item's end before the overflow menu button, `ProductThumbnail`, 56.dp with small corners, without
  `LeadingTrailingContentWrapper`, whose padding only icons and switches need), first the
  filter row directly on the background
  (`edgeToEdgeItem` + `EdgeToEdgeRow` of `InsetLazyColumn`: centered on the screen while all
  chips fit between its 16.dp margins, otherwise lined up with the items and scrolling sideways
  up to the screen's edges, the padding scrolls along) and, while open, the search as its own
  segmented item below it (full width, opens with Material's spatial spring from its top edge,
  so it can be focused, keyboard included, right away without scrolling the list; closing
  clears the focus at its start; `showSearchAtTop` opens it only once the list scrolled to the
  top: opened while scrolling, the field took the focus when the list passed it and bringing it
  into view stopped the scrolling short of the top).
  `ProductFilterBar` has outlined Material 3 chips with a `surfaceContainerLow` container (they
  lie on the background) and no elevation in any state, one per filter: product group and status
  as dropdown filter chips that are selected and show the value while not "All", "Show
  disabled" as toggle chip, a sort chip, and "Clear filter" as assist chip with a reset icon
  at its start while a filter (not the search) is active; chips use the standard overloads with Material's 8.dp rounded rectangle, not the
  morphing `shapes` overloads whose tokens are full round). Every chip, also the choices of
  forms (weekdays, checklist userfields), has no elevation in any state
  (`filterChipElevation`): the web draws the hover elevation of an outlined chip as a dark
  frame. Back closes an open search first
  (`NavigationBackHandler`, also the browser's back) and stays on the list
  and a floating toolbar (Add as FAB, Configure userfields (the product userfields), Presets for
  new products, Scroll to
  top, Search last). Filtering, sorting and the fuzzy search are the pure `buildProductList`:
  in stock means `amount_aggregated > 0` of an active product, like Grocy's `stock_current`.
- Related objects of forms (`barcode/`, `conversion/`, `related/`): Grocy's popup forms are
  `FormDialog`s (Save with loading indicator, Cancel, the server's error above the groups),
  their state lives in a ViewModel (`ProductBarcodesViewModel`, `ConversionsViewModel`, shown
  object set by `showProduct`/`showOwner`, so a just created product gets them), saving
  online only (`changeErrorOf`). The product form shows after its userfields Grocy's right
  column: barcodes (amount in the units the purchase unit converts into, `productAmountUnits`
  from `quantity_unit_conversions_resolved`, "This equals" below), the grocycode, the product's
  conversion overrides with "Show resolved conversions" (`ResolvedConversionsDialog`), the
  picture; a quantity unit's form its default conversions (`from` fixed). Items edit on a tap
  and delete through their menu (`ObjectActionsViewModel` of `product_barcodes` /
  `quantity_unit_conversions`, Grocy's questions); a saved or deleted conversion downloads the
  resolved ones again.
- Units are named like Grocy's `__n` for quantity units: `QuantityUnit.nameFor(amount,
  language)` with the plural rules of Grocy's translations (`grocyPluralIndex`) and the unit's
  `plural_forms` lines.
- `ProductScreen` edits or creates (`ProductRoute(productId = null)`) a product with every field
  of Grocy's form, in its order, grouped, fields of disabled features hidden
  (`ProductFormFeatures`). The logic is pure and tested: `ProductFormValues.validate` (Grocy's
  required fields and number ranges), `toProduct` (hidden fields keep their values, an
  unchanged description keeps its HTML), `newProductValues` (presets), `linkedQuantityUnit`
  (the other units follow the stock unit). Saving is online only: an edited product closes the
  form like "Save & return", a created one turns the form into its edit form. Userfields are
  edited by type (`UserfieldInput`, only changed ones are sent; files and images read only),
  the picture through `ProductPictureDialog` (`rememberImagePicker` of `core:ui`; its item
  shows the picture the product will have before its arrow, the stored list picture or the
  picked one, `ProductViewModel.thumbnail`, and "No picture available" without one), the parent
  product can also be scanned (barcode or grocycode `grcy:p:<id>`): the scanner opens inside the
  field's item below it (`DropdownItem(below)`, small corners, text field color). Pickers whose
  text names no option (`ProductFormState.unmatchedChoices`, `UserfieldInput.isUnmatched`) block
  saving. Leaving with unsaved changes (`ProductViewModel.hasUnsavedChanges`: form values,
  userfields, picture or unmatched text differ from the loaded or last saved state; back button,
  back gesture and browser back) asks in `UnsavedChangesDialog` (saveable, survives rotation):
  save (and leave once saved), discard, or cancel.

# Screen and dialog design

Binding layout and color rules for screens and dialogs (product requirement). Moved from CLAUDE.md.

Every screen should look as polished as `AboutScreen` (with its own content). Reference
implementations: `AboutScreen` and the login screens for screens, `FeedbackDialog` for dialogs.

Build screens with the shared components of `core:design`, which already apply every rule below:
`SegmentedListScreen` (scaffold, colored large app bar, `InsetLazyColumn`), `segmentedGroup`
(group with optional title as `StringResource`), `SegmentedContainer` (non-clickable item for
text fields, buttons, text or a scanner), `SegmentedListDefaults.colors()`/`.shape(index, count)`
for `SegmentedListItem`s, and `AppBarIconButton` for app bar actions.

- Screen background is always `surfaceContainer` (`Scaffold(containerColor = ...)`).
- Top app bar: `LargeFlexibleTopAppBar` (Material 3 Expressive) with the expanded title at the
  start and the collapsed one centered. Compose aligns both rows alike, so `SegmentedListScreen`
  centers them and lets the expanded row's title fill its width (text at its start), which puts
  it exactly where a start aligned title would be; the row is told apart by its text style.
- Top app bar: `surfaceContainer` while not scrolled, `surfaceContainerHighest` when scrolled
  (`TopAppBarDefaults.topAppBarColors(containerColor = surfaceContainer,
  scrolledContainerColor = surfaceContainerHighest)` plus a scroll behavior connected via
  `nestedScroll`).
- App bar actions (navigation icon included) are `FilledIconButton`s with `contentColor =
  onSurface` inside a `HapticTooltipBox`: `AppBarIconButton`, which `NavigateBackButton` and
  `AppBarOverflowMenu` use. Their container is always `surfaceContainerHighest`: it stands out
  on the `surfaceContainer` bar (`surfaceContainerHigh` was a single tone step above it, too
  faint) and becomes one with the scrolled bar.
- Nothing except group titles sits directly on the `surfaceContainer` background. Exceptions: the
  header of `LoginChoiceScreen` (logo, welcome title, subtitle, centered above the choices) and
  the primary action button that ends a form (`LoginManualScreen`'s Connect, full list width in
  an `insetItem`). A heading or explanation that belongs to one control goes inside its item as
  `SegmentedTexts` (list item headline/supporting styles), not as group title. All other
  content (text, values, text fields, buttons, switches, data, ...) lives inside Material 3
  Expressive segmented list items, logically grouped:
  - `InsetLazyColumn` with `insetItem` per group, `consumeWindowInsets(padding)` and content
    padding of the scaffold padding + 16.dp top and bottom; 16.dp between groups. Screens with
    text input also need the keyboard handling of `SegmentedListScreen`.
  - A group is a `Column` with `Arrangement.spacedBy(ListItemDefaults.SegmentedGap)`.
  - Items use `SegmentedListItem` (clickable) or `ListItem` with the content-lambda overload
    and `shapes` (non-clickable), shapes from `ListItemDefaults.segmentedShapes(index, count)`, colors from
    `SegmentedListDefaults.colors()`, which is Material's default `segmentedColors()` with the
    `surface` container (`SegmentedListDefaults.containerColor` for custom items).
  - Leading icons go in `LeadingContentWrapper`.
  - Group titles: `titleSmall`, color `secondary`, `semantics { heading() }`, 8.dp below.
- Dialogs, except the plain text dialog (`TextDialog`), are segmented lists too: build them with
  `ScrollableAlertDialog` + `ScrollableAlertDialogContent` from `core:design`, a short
  `bodyMedium`/`onSurfaceVariant` message on top if needed, then groups of segmented items in
  `surface` with 16.dp between groups, like `FeedbackDialog`.
- Always prefer the project's own components from `core:design` (`ScrollableAlertDialog`,
  `InsetLazyColumn`, `RoundedTextField`, `ConnectedButtonGroup`, `NavigateBackButton`,
  `HapticTooltipBox`, `LeadingContentWrapper`, ...) over raw Material components when they fit.

## Haptics on the web

The web app has no haptics at all: browsers only offer `navigator.vibrate` at full strength
without amplitude control, which feels like a cheap buzz (Compose's `Confirm` even vibrates twice
there), so `apps/web` provides a silent `LocalHapticFeedback`.

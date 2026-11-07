# Focus and keyboard

Binding for every screen, dialog and field. The goal: a user works through a form with as few
extra taps as possible, without switching between keyboard and screen. It follows Grocy's web
forms (`public/viewjs/*.js`) where those work, and Android's and Material's guidance on top: the
keyboard opens on its own only where typing is the main task, everything works with a hardware
keyboard and mouse too.

The pieces: `FormFocus`, `LocalFormFocus`, `rememberFormFocus`, `FocusRequestEffect`,
`AutoFocusEffect` (`core:design`, `FormFocus.kt`), `RoundedTextField`, `SearchableDropdownField`,
`TextItem`, `DropdownItem`, `ScannerFieldState` and `ScannerToggleButton` (`core:forms`),
`ScannerFormsRepository` (`core:data`).

## What takes the focus when a form opens

- **New object:** the first field the user types into (`autoFocus = viewModel.isNew`), usually
  the name. Grocy does this too.
- **Editing an object:** nothing in master data forms, because the user may only want to look and
  the keyboard would cover half of it. Only where the edit is usually about one value, that value
  takes the focus: the amount of a shopping list item, an ingredient, a meal plan entry or a stock
  entry; the servings of a nested recipe.
- **Product forms** (`ScannerFieldState`): see below.
- **Dialog with a choice or a date first:** nothing, unless it is the dialog's only input. A
  picker or calendar button needs no keyboard. Fields with both a value to choose first and a
  number after it focus the number once the choices are made (`ConversionDialog`).
- **Screens without input** never open the keyboard. Never auto-focus a field on a screen whose
  main job is reading, or a filter or search that is not open yet.

`autoFocus` acts once per opened form: `AutoFocusEffect` keeps that across scrolling and rotating,
and waits for the window's focus, without which a dialog would not get its keyboard.

## Scanner or keyboard

Forms whose first field can be scanned (Purchase, Consume, Transfer, Inventory, shopping list
item, recipe ingredient, meal plan product, barcode dialog, chore and battery tracking) remember per form (`ScannerForm`) what the user entered with last:

- The form opens with the scanner and no keyboard if the scanner was used last, else with the
  focus in the field and the keyboard. It shows that in its first frame, the repository is an
  eager flow.
- The scanner button sets it. Closing the scanner focuses the field, opening it closes the
  keyboard, which would cover the camera.
- A scan means "the scanner": it closes the scanner and the amount takes the focus, but the next
  empty form opens with the scanner again. Scan mode leaves it open and books. Where everything
  after the scan is prefilled (chore and battery tracking) nothing takes the focus: the keyboard would only
  cover the FAB, one tap on which books.
- Tapping into the field while scanning means "the keyboard": the scanner closes and that is
  remembered.
- A form opened with its value (a preset product, an edited item) needs neither. It does not
  change what is remembered.
- A code the page does not offer keeps scanning; a code that needs an answer (Grocy's "Create or
  assign product") closes the scanner until it is answered.

The product form's parent picker is not remembered: scanning is a rare extra there, far below the
first field, and must not open a camera with the form.

A new form with a scanner adds a `ScannerForm` entry, gets its state from
`ScannerFormsRepository.fieldState` and passes `onFocusChange = scanner::onFieldFocusChange`.

## Going through a form

- Single-line fields have `ImeAction.Next`, multi-line fields keep Enter for their line breaks.
- The last field the user usually changes has `isLastField = true`: its action is `Done` and
  submits the form (`LocalFormFocus`). What follows it is prefilled or optional. Examples:
  Consume and Inventory from the amount, Transfer from the target location, Purchase from the
  price (`keyboardEndField`). A field before userfields is only last without userfields
  (`viewModel.hasUserfields`), the last userfield then is.
- Outside of a form `isLastField` makes `Done` close the keyboard.
- A number's text is selected when it takes the focus, so typing replaces it (`RoundedTextField`).
  A value set from code while the field may be focused uses `setTextAndSelectAll`.
- A picker (`SearchableDropdownField`): the keyboard's action takes the best suggestion for typed
  text and goes on. Without typed text it changes nothing. A tapped suggestion goes on as well
  while the field has the focus; a choice from the arrow leaves the keyboard closed, the arrow
  exists to choose without it.
- A product picker's value is followed by the amount. `onKeyboardNext` keeps the focus in the
  field while it has no product, because the disabled amount cannot take it.
- A barcode reader types like a keyboard and ends with Enter: pickers that accept codes use
  `onSubmitText`, so a code works like a scanned one, in scan mode too.
- Product pickers with Grocy's "Create or assign product" ask for text no suggestion contains, also
  when the focus leaves the field (`submitsTextOnLeave`, `docs/features/master-data.md`). After
  "Cancel" the field takes the focus back with its text selected, so typing replaces it.

## Submitting

- The FAB and the dialog's button call `FormFocus.submit`, not the ViewModel's save, so all ways
  to submit behave alike.
- A refused submit takes the focus to the first invalid field, also outside the composed part of a
  long list (`rememberFormFocus(listState)`). `onSubmit` returns false only for invalid input, not
  while saving or loading. Errors must set the `isError` of the field, which is how it is found.
- After a booking the form starts over at the top, with the scanner or the focus in the field.

## Hardware keyboard, browser

- Enter submits like in Grocy's forms, except where the field uses it itself: a picker takes its
  suggestion first, Enter again submits. In multi-line fields Ctrl or Cmd with Enter submits. All
  of this comes with `RoundedTextField` inside a form.
- Tab reaches every control in order. Never make a control unfocusable to skip it on the way,
  touch input skips buttons by itself (`clickable` is not focusable in touch mode).
- The search closes with Escape, its keyboard action only closes the keyboard.

## Choosing the keyboard

- Numbers `KeyboardType.Decimal` or `Number`; addresses `Uri`.
- Dates always use `DateTextItem`. By default it needs no keyboard: the field is a button for
  Material's date picker (with "Clear"), the keyboard's way through the form passes it, Tab
  stops on it, and a refused submit opens the picker of the first invalid date. With "Enter dates
  with keyboard" it is a number field of digits only, the separators are shown, not typed
  (`typedDateSeparators`), and the form's state keeps Grocy's format as soon as the digits name a
  date (`grocyDateOfTyped`). The order follows the language (`languageDateOrder`) unless the user
  chose one, both through `LocalDateInput`.
- Due dates (`DateTextItem(scanDueDate = true)`: bookings, stock entries) also have a scan
  button after the calendar (`TextItem(trailingButton)`, which keeps its click above the
  field's button). It closes the keyboard and opens the camera below the field; a read date fills
  the field and closes it, a date whose day and month could be swapped first asks which one.
- `autoCorrect = false` for what is no language (codes). Pickers and the search turn it off
  themselves.
- Secrets use `RoundedSecureTextField` with `ContentType.Password` for password managers.

## Checklist for a new screen or dialog

1. Which field does the user type into first? Does typing come first here at all?
2. Inside a form? Provide `LocalFormFocus` (`ObjectFormScreen` and `FormDialog` do), submit through
   it.
3. Which field ends the keyboard's way through the form? `isLastField`, checked by hand: going on
   from the real last field otherwise jumps back to the top.
4. Can its first field be scanned? `ScannerFieldState`.
5. Does a chosen value have an obvious next field? Move the focus there.
6. Do the errors reach `isError`?
7. Test the logic that decides this (`ScannerFieldStateTest`, `keyboardEndField`).

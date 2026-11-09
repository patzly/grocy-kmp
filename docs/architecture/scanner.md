# Barcode scanner

`core:scanner`: engines, permissions and platform details, and the date scanner. Moved from
CLAUDE.md.

- One component for the whole app: `BarcodeScanner(onBarcodeScanned, formats, shape, paused)`.
  Pass the segmented item shape (`SegmentedListDefaults.shape(index, count)`) and place it as an
  item of a group, never directly on the background. It is a landscape rectangle (16:10, at most
  240.dp high, so wide windows such as desktop browsers get a strip) and
  draws its own `surface` background and permission/error states inside. The torch button sits
  in the bottom end corner with 12.dp margin.
- `CameraScannerBox` is the frame of every camera scanner (size, permission, errors, torch);
  `CameraPreview` runs a `FrameAnalyzer<T>` on every frame, the barcode decoder or the text
  recognizer, and reports its result on the main thread.
- Engines implement `BarcodeDecoder` (`decode(CameraFrame, formats): List<Barcode>`):
  ZXing via `BarcodeDecoderFactory.Zxing` (zxing-core on Android, zxing-wasm in browsers) and
  ML Kit via `MlKitBarcodeDecoderFactory` (`core:scanner-mlkit`). `CameraFrame` is an
  expect class (ImageProxy / ImageData). All engines share `ScanResultFilter` (linear codes need
  two identical reads in a row, a code in view is reported once) and the `BarcodeFormat` set, so
  they behave the same. ZXing on Android decodes the upright image and a 90 degree turn for
  linear codes, like ML Kit's orientation-independent detection.
- ML Kit creates its component registrars by reflection; `core:scanner-mlkit` ships the keep
  rule for their constructors (`consumer-rules.pro`), which R8's full mode removes otherwise,
  so every ML Kit client crashed with a `NullPointerException`. The Play app's minified builds
  fail when R8 removes one anyway (check after `minify<Variant>WithR8` on `usage.txt`).
- Engine selection: `ScannerRepository.activeEngine` = setting if the platform has it and (for
  ML Kit) `isUnlocked`, else ZXing. ZXing is always the default; unlocking (key installed, or the
  key check turned off by 10 long presses on the key item in About) never switches the engine,
  the user selects ML Kit themselves. `GrocyApp` provides the matching factory through
  `LocalBarcodeDecoderFactory`, so screens only call `BarcodeScanner`. The user chooses in the
  settings (`ScannerSettingsRoute`, `docs/features/settings.md`). `activeEngine` is an eager
  `StateFlow` of the app scope. A key removed later shows and uses ZXing but keeps the stored
  choice, so ML Kit returns with the key.
- Permission: asked only when the user taps the scanner's button (`rememberCameraAccess`);
  granted cameras start right away. Android detects "denied for good" and opens the app settings;
  browsers get a hint for the site settings. The camera stops when the scanner leaves the
  composition or the lifecycle stops.
- Android: CameraX `CameraXViewfinder` in `ImplementationMode.EMBEDDED` (TextureView, so the
  rounded clip works), `ImageAnalysis` 1280x720 with KEEP_ONLY_LATEST, decoding on
  `Dispatchers.Default`, torch button if the camera has a flash. The CAMERA permission and an
  optional camera feature are declared in the module manifest.
- Web: `HtmlElementView` with a `<video>` from `getUserMedia` (needs HTTPS); the element lies
  above the Compose canvas, so its corners are rounded with CSS from the shape
  (`toCssBorderRadius`) and Compose overlays cannot cover it. Frames are decoded every 120 ms.
  `zxing_reader.wasm` is copied from node_modules into the web app (`copyZxingWasm` in
  `apps/web`) and loaded from there instead of zxing-wasm's default CDN.

## Due dates

`TextScanner` reads text instead of codes, for the due date printed on packages, often in dot
matrix print that is hard to read.

- Engines implement `TextRecognizer` (`recognize(CameraFrame): List<String>`, the lines top to
  bottom). For now only ML Kit's bundled Latin model (`MlKitTextRecognizerFactory` of
  `core:scanner-mlkit`, Play app); FOSS and web have none: a bundled open engine (PP-OCRv5 on
  ONNX Runtime) read well but made the APK about 50 MB larger. Like ML Kit's barcodes it needs the
  unlock key, whichever barcode engine is selected. `PlatformDependencies.textRecognizer`
  passes it, `GrocyApp` provides it as `LocalTextRecognizerFactory` while
  `ScannerRepository.scansDueDates` (the setting "Scan due dates", an engine and the unlock key),
  else null, which hides every date scanner (`canScanDueDates`).
- Dot matrix: ML Kit reads every other frame with the dots joined into strokes
  (`CameraFrame.dotsJoined`: grayscale, `boxBlurred` with radius 2, as NV21), the frames between
  as they are; voting combines both.
- `printedDueDateOf` (`core:model`) finds the due date among the lines: numeric dates with dots,
  slashes, dashes or spaces, year first dates, month names of six languages, month and year
  (the month's last day), digits without separators only right after a keyword. It fixes what
  text recognition confuses in numbers (O for 0, a small l for 1, commas for dots) and skips lot
  numbers, times and phone numbers. Keywords like MHD, best before or EXP in the line or the two
  before win, production or packing dates lose, then the later date. Dots are always day
  first; other separators follow `LocalDateInput.order` with the swapped date as alternative.
  Only dates from three years ago to 25 years ahead count.
- `PrintedDueDateVotes` accepts a date read in two of the last five frames, so a misread digit
  never fills a field. A date with an alternative pauses the camera and asks with two buttons.
- `DateTextItem(scanDueDate = true)` (bookings, stock entries) and the product's default due
  days (`rememberDueDaysScan`) open the camera below the field (`DueDateScanner`).

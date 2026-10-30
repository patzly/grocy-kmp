# Barcode scanner

`core:scanner`: engines, permissions and platform details. Moved from CLAUDE.md.

- One component for the whole app: `BarcodeScanner(onBarcodeScanned, formats, shape, paused)`.
  Pass the segmented item shape (`SegmentedListDefaults.shape(index, count)`) and place it as an
  item of a group, never directly on the background. It is a landscape rectangle (16:10, at most
  240.dp high, so wide windows such as desktop browsers get a strip) and
  draws its own `surface` background and permission/error states inside. The torch button sits
  in the bottom end corner with 12.dp margin.
- Engines implement `BarcodeDecoder` (`decode(CameraFrame, formats): List<Barcode>`):
  ZXing via `BarcodeDecoderFactory.Zxing` (zxing-core on Android, zxing-wasm in browsers) and
  ML Kit via `MlKitBarcodeDecoderFactory` (`core:scanner-mlkit`). `CameraFrame` is an
  expect class (ImageProxy / ImageData). All engines share `ScanResultFilter` (linear codes need
  two identical reads in a row, a code in view is reported once) and the `BarcodeFormat` set, so
  they behave the same. ZXing on Android decodes the upright image and a 90 degree turn for
  linear codes, like ML Kit's orientation-independent detection.
- Engine selection: `ScannerRepository.activeEngine` = setting if the platform has it and (for
  ML Kit) `isUnlocked`, else ZXing. ZXing is always the default; unlocking (key installed, or the
  key check turned off by 10 long presses on the key item in About) never switches the engine,
  the user selects ML Kit themselves. `GrocyApp` provides the matching factory through
  `LocalBarcodeDecoderFactory`, so screens only call `BarcodeScanner`. There is no settings
  screen yet to change the engine.
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

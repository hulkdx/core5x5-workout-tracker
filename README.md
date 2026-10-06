# Core5x5

A 5×5 strength-training tracker for Android and iOS, built with Kotlin Multiplatform and Compose Multiplatform.

Currently in development.

## Build

For Android, open the project in Android Studio or run:

```bash
./gradlew :androidApp:assembleDebug
```

For iOS, open [Core5x5.xcodeproj](iosApp/Core5x5.xcodeproj) in Xcode and run the `Core5x5` scheme. Running on a physical device requires your own signing configuration.

## Screenshot tests

Compose screenshot tests cover the app screens in the standalone `:screenshot-test` module. From `kmm/`, update reference images after intentional visual changes and validate them with:

```bash
./gradlew :screenshot-test:updateDebugScreenshotTest :screenshot-test:validateDebugScreenshotTest
```

Reference images are stored in `screenshot-test/src/screenshotTestDebug/reference`.

## License

[MIT](LICENSE)

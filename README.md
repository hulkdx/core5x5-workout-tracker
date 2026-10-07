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

To validate screenshots:
```bash
./gradlew :screenshot-test:validateDebugScreenshotTest
```

To record screenshots:

```bash
./gradlew :screenshot-test:updateDebugScreenshotTest
```

Updating references accepts the current rendered previews as the new baseline. Review the changed images before committing them.

## License

[MIT](LICENSE)

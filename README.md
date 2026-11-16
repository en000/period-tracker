# Cycle Journal — v.1

An offline Android period tracker built with Kotlin and Jetpack Compose.

## Features

- Monthly calendar with period tracking and approximate predictions.
- Daily logs for flow, symptoms, mood, pain and notes.
- Cycle insights and editable period history.
- Optional local reminders and light/dark themes.
- Password-encrypted backup export/import and local data deletion.

Records stay on your device unless you export a backup. No account is required.
Estimates are for personal tracking, not medical advice. See [PRIVACY.md](PRIVACY.md).

## Run the app

1. Open the project in Android Studio with JDK 17 and Android SDK 37 installed.
2. Start an emulator, or connect a phone by USB with USB debugging enabled.
3. Select the device and click **Run ▶**.

## Build and test

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

On macOS/Linux, use `./gradlew` instead of `.\gradlew.bat`.
The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
Run `:app:connectedDebugAndroidTest` with a connected device for integration/UI tests.

## Structure

One activity hosts Compose views. Data flows through data sources, repositories,
use cases and ViewModels. Room stores records; DataStore stores preferences.

This is version 1. Full localization and device accessibility validation are still
in progress.

# PingPlace

PingPlace is an Android reminder app for errands that are tied to brands or place types instead of one exact address.

Examples:

- Return Amazon package when near any Whole Foods
- Buy batteries when near any Costco
- Drop off package when near any UPS Store
- Pick up toothpaste when near any CVS

Tagline: `Reminders that show up when you show up`

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Room
- WorkManager
- Google Play Services Location
- OpenStreetMap store lookup via Overpass

## Project shape

- `app/src/main/java/com/pingplace/ui`: Compose screens and view models
- `app/src/main/java/com/pingplace/data`: Room, repository, and app container
- `app/src/main/java/com/pingplace/background`: monitoring worker, notifications, geofences, boot restore
- `app/src/main/java/com/pingplace/location`: location and nearby-place provider abstractions
- `app/src/main/java/com/pingplace/domain`: blocked-time and reminder-evaluation logic

## Setup

1. Open the project in Android Studio.
2. Optional: point the app at your own offline-pack catalog with a Gradle property:

```properties
OFFLINE_PACK_MANIFEST_URL=https://example.com/offline-packs/catalog.json
```

If you skip that, PingPlace uses the bundled offline-pack catalog.

## Current behavior

- Create reminders for supported brands or any custom chain/category.
- Choose distance or travel-time trigger per reminder.
- Group reminders by brand on the home screen.
- Define recurring blocked-time windows.
- Monitor with a visible location foreground service while background monitoring is enabled and unfinished reminders exist.
- Request location fixes about every 5 seconds when driving (20 mph+), 10 seconds when moving, and 30 seconds when stationary. Keep fast checks through short traffic stops. Android and GPS availability can affect delivery timing.
- Search for stores at any speed, including walking or an unknown speed. A reminder explicitly marked driving-only still requires fast movement.
- Allow roughly 45 seconds of driving approach distance (capped at 2 km), without shrinking a larger configured distance.
- Group notifications by brand. Unfinished errands can ping again after 3 minutes while driving or 10 minutes otherwise; snoozes, completion, and blocked times are honored. Only successful notification posts start the cooldown.
- Keep geofences for up to 90 nearby branches, sized to reminder triggers, plus a 15-minute WorkManager recovery check that also works with offline data.
- Cache live coordinates and recalculate distance/ETA for every fix, with bounded network retries.
- Show live monitoring status, animated radar rings, screen transitions, animated filters and list changes, and an expandable quick-add panel.
- Recover monitoring after reboot or app update.

## Notes

- Travel-time mode currently uses a speed-based local estimate when route data is unavailable.
- Nearby place lookup uses free OpenStreetMap/Overpass data and does not require an API key.
- The public Overpass service is a practical no-key default for light use; if usage grows, switch to your own hosted endpoint.
- Continuous precise location monitoring uses more battery than the recovery worker alone.

## Alert setup

Open the updated app once, enable **Settings → Background monitoring**, and allow precise location and notifications. For reliable background recovery, choose **Android app settings → Permissions → Location → Allow all the time**. The home screen reports missing permissions or a missing location fix; Android notification-channel settings and Do Not Disturb still control sound.

Android requires a declared [location foreground service](https://developer.android.com/about/versions/14/changes/fgs-types-required) for this monitoring mode. [Geofence response times are best effort](https://developers.google.com/android/reference/com/google/android/gms/location/Geofence.Builder), so the service supplies frequent fixes while the periodic worker is a recovery path.

## Verification

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.pingplace.ReminderMonitoringTest com.pingplace.test/android.test.InstrumentationTestRunner
```

Unit tests cover speed-aware timing, approach distance, retry eligibility, and repeat-alert cooldowns. Android tests exercise real `Location` objects with controlled providers for walking/unknown speed, driving-only reminders, hybrid/offline modes, blocked-time filtering, failed searches, and geofence coverage. They do not change the app's saved reminders.

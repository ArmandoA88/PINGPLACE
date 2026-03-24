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
- Run periodic nearby evaluation in the background.
- Group notifications by brand and avoid repeat alerts for the same visit.
- Recover monitoring after reboot or app update.

## Notes

- Travel-time mode currently uses a speed-based local estimate when route data is unavailable.
- Nearby place lookup uses free OpenStreetMap/Overpass data and does not require an API key.
- The public Overpass service is a practical no-key default for light use; if usage grows, switch to your own hosted endpoint.
- The app was rebuilt locally and the debug APK was installed on an emulator on March 23, 2026.

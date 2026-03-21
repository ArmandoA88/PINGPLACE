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
- Google Places Text Search integration via HTTPS

## Project shape

- `app/src/main/java/com/pingplace/ui`: Compose screens and view models
- `app/src/main/java/com/pingplace/data`: Room, repository, and app container
- `app/src/main/java/com/pingplace/background`: monitoring worker, notifications, geofences, boot restore
- `app/src/main/java/com/pingplace/location`: location and nearby-place provider abstractions
- `app/src/main/java/com/pingplace/domain`: blocked-time and reminder-evaluation logic

## Setup

1. Open the project in Android Studio.
2. Add a Places API key as a Gradle property:

```properties
PLACES_API_KEY=your_key_here
```

You can place that in your user `gradle.properties` file or pass it on the command line.

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
- Nearby place lookup expects a valid Google Places API key.
- The project was verified with static review and a local Gradle startup attempt; the build environment on this machine failed before compilation while loading Gradle's Windows native library.

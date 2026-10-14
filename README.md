# Money Manager

**Offline-first personal finance tracker for Android.** Track income and expenses, organize them by category, review your history, and export clean PDF reports — all entirely on your device. No account, no internet, no cloud.

A [Rainweaver Studio](https://rainweaver.qd.je) app.

---

## Features

- **Record income & expenses** with amount, category, date, and note
- **Categories** — built-in defaults plus your own, for income and expenses
- **Dashboard** — period totals (income, expense, balance) at a glance
- **History** — filter transactions by type; browse by month
- **PDF report export** — professional report with **separate Credit (CR) and Debit (DB) columns**, totals, expense-by-category breakdown, and multi-page pagination
- **Formatting preferences** — currency, decimals, and thousands separator
- **100% offline** — data lives in a local database; nothing leaves the device
- **Exact money math** — all amounts stored as integer minor units (no floating-point drift)

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Room** for local persistence
- **Android `PdfDocument`** for on-device report generation
- **SAF** (Storage Access Framework) for export/share
- **min SDK 24** (Android 7.0)

## Download

Grab the latest signed release from [**Releases**](https://github.com/phantomlee707-lang/money-manager/releases/latest):

- `app-release.apk` — release build (optimized, signed)

Install it directly on your device (enable "install from unknown sources"). The release APK is signed with a different key than debug builds, so uninstall any debug version first.

## Build from source

```bash
git clone https://github.com/phantomlee707-lang/money-manager.git
cd money-manager
./gradlew assembleDebug        # debug APK (works out of the box)
```

A **release** build additionally requires a `keystore.properties` file (with your signing keystore) in the project root — it is intentionally **not** committed:

```properties
storeFile=release.keystore
storePassword=…
keyAlias=…
keyPassword=…
```

Then: `./gradlew assembleRelease`

> Requires JDK 17 and the Android SDK (compileSdk 34).

## Tests

```bash
./gradlew testDebugUnitTest
```

Covers money parsing/formatting and the Credit/Debit classification + financial totals (including transfer exclusion).

## License

© Rainweaver Studio. All rights reserved.

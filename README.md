# Garage Ledger

Garage Ledger is a local-first Android replacement for the old aCar/Fuelly mobile workflow. This repo is focused on offline durability, faithful import of historical data, a dense dashboard-first console, and open export formats.

## Current milestone

Phase 27 sample fixture regression harness is in place:

- Native Android project scaffold in Kotlin + Jetpack Compose
- Room schema covering vehicles, catalogs, reminders, fill-ups, services, expenses, trips, and attachments
- DataStore-backed preference snapshot import
- aCar sectioned CSV importer
- aCar `.abp` ZIP/XML importer
- Fuelly CSV fill-up importer with preview, unit selection, and field-mapping UI for non-standard headers
- Pure Kotlin calculation engine for fuel efficiency, reminders, trip costs, and chrono/odometer validation
- End-to-end flows for import/export, vehicle browsing, vehicle detail, fuel-up/service/expense/trip entry, and record browsing/filtering
- WorkManager-backed periodic reminder checks and local backup scheduling
- Android notification channel + reminder alert delivery, with runtime permission enablement from the app
- Open zipped JSON backup export plus full restore/import, alongside scheduled local rotating backups in app documents storage
- Picker-backed date and date/time entry across record editors, reminder due dates, browse filters, and vehicle purchase/selling fields
- Settings-backed date formats now affect browse rows, detail screens, dashboard cards, predictions, and reminder summaries
- Local attachment support for photos and PDFs across fuel-up, service, expense, and trip editors
- Launcher quick actions with home-screen pin requests and a vehicle chooser for fast entry
- Classic Android home-screen widgets for quick add and service reminders
- Optional home-screen widgets for fuel efficiency and fuel price snapshots
- Dedicated statistics and charts screen with vehicle/timeframe filtering
- Local statistics CSV export with chart-series data
- Standalone HTML statistics export with summary sections and embedded chart visuals
- Record detail screens for fuel-ups, services, expenses, and trips with explicit delete flows
- Editor-level `Customize this screen` support backed by imported visible-field preferences
- Settings screen for units, formats, backup/reminder thresholds, notifications, and optional fields
- Type Management screen for local fuel, service, expense, and trip type catalogs
- Vehicle add/edit, retire/reactivate, destructive delete, and local profile photo support
- Vehicle part list/editor management with local add/edit/delete flows
- Reminder center plus local reminder add/edit/delete, default seeding, and direct service handoff
- Predictions screen for next fill-up date/odometer, estimated range, and local trip-cost rates
- Home-screen predictions widget driven by the same local prediction engine
- Trip entry helpers for open-trip finishing, last-destination/return-trip starts, and end-odometer absolute vs trip-distance entry
- Local latitude/longitude capture plus map-open actions across fuel-up, service, expense, and trip records
- Browse/search parity improvements with structured subtype, payment, place, fuel, and trip-specific filters
- Browse-level quick actions for view/edit/delete plus trip copy/finish flows
- Trip detail actions for copy/finish and retired-vehicle edit protection aligned with old aCar behavior
- Browse-level saved sort order control plus local CSV export of the current filtered result set
- Browse quick date presets, active filter chips, and a collapsible advanced query builder for local record search
- Saved browse searches with local save/load/delete flows and open-backup round-trip support
- Configurable local sample-fixture regression test wiring plus a PowerShell runner for the supplied aCar CSV/ABP files
- Instrumented UI coverage for browse sort/menu behavior and browse-launched trip copy/finish flows
- Parser/regression/unit tests, backup/export tests, reminder alert tests, and instrumented Compose tests for entry flows plus import/export center navigation

## Build

This workspace uses:

- JDK 17
- Android SDK under `%LOCALAPPDATA%\\Android\\Sdk`
- Gradle wrapper

From PowerShell on this machine:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat testDebugUnitTest
.\scripts\run-sample-fixture-regression.ps1
```

## Project layout

- [`app/src/main/kotlin/com/garageledger/data`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/data): importers, repository, Room persistence, and export orchestration
- [`app/src/main/kotlin/com/garageledger/background`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/background): WorkManager scheduling and workers
- [`app/src/main/kotlin/com/garageledger/attachments`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/attachments): local attachment import/capture storage
- [`app/src/main/kotlin/com/garageledger/domain`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/domain): pure models and calculation rules
- [`app/src/main/kotlin/com/garageledger/notifications`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/notifications): notification channel and reminder notifier
- [`app/src/main/kotlin/com/garageledger/shortcuts`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/shortcuts): launcher quick actions and pinning helpers
- [`app/src/main/kotlin/com/garageledger/widgets`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/widgets): app widget providers, updater, and reminder formatting
- [`app/src/main/kotlin/com/garageledger/ui`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/ui): Compose dashboard, settings, type management, statistics/charts, record detail, import center, browse screen, vehicle detail, and record editors
- [`docs/architecture.md`](D:/Code/Codex/Garage_Ledger/docs/architecture.md): implementation plan and schema proposal
- [`docs/import-formats.md`](D:/Code/Codex/Garage_Ledger/docs/import-formats.md): CSV/ABP/Fuelly format notes
- [`docs/migration-notes.md`](D:/Code/Codex/Garage_Ledger/docs/migration-notes.md): migration assumptions and sample expectations
- [`docs/known-differences-vs-acar.md`](D:/Code/Codex/Garage_Ledger/docs/known-differences-vs-acar.md): honest deltas from aCar today

# Garage Ledger

Garage Ledger is a local-first Android replacement for the old aCar/Fuelly mobile workflow. This repo is focused on offline durability, faithful import of historical data, a dense dashboard-first console, and open export formats.

## Current milestone

Phase 1 is in place:

- Native Android project scaffold in Kotlin + Jetpack Compose
- Room schema covering vehicles, catalogs, reminders, fill-ups, services, expenses, trips, and attachments
- DataStore-backed preference snapshot import
- aCar sectioned CSV importer
- aCar `.abp` ZIP/XML importer
- Fuelly CSV fill-up importer
- Pure Kotlin calculation engine for fuel efficiency, reminders, trip costs, and chrono/odometer validation
- First end-to-end flow: import -> browse vehicles -> vehicle detail -> add/edit fuel-up -> recalculate stats
- Parser/regression/unit tests, plus an instrumented Compose editor smoke test

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
```

## Project layout

- [`app/src/main/kotlin/com/garageledger/data`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/data): importers, repository, Room persistence
- [`app/src/main/kotlin/com/garageledger/domain`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/domain): pure models and calculation rules
- [`app/src/main/kotlin/com/garageledger/ui`](D:/Code/Codex/Garage_Ledger/app/src/main/kotlin/com/garageledger/ui): Compose dashboard, import center, vehicle detail, fuel-up editor
- [`docs/architecture.md`](D:/Code/Codex/Garage_Ledger/docs/architecture.md): implementation plan and schema proposal
- [`docs/import-formats.md`](D:/Code/Codex/Garage_Ledger/docs/import-formats.md): CSV/ABP/Fuelly format notes
- [`docs/migration-notes.md`](D:/Code/Codex/Garage_Ledger/docs/migration-notes.md): migration assumptions and sample expectations
- [`docs/known-differences-vs-acar.md`](D:/Code/Codex/Garage_Ledger/docs/known-differences-vs-acar.md): honest deltas from aCar today

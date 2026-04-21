# Release Checklist

Use this checklist before calling a build ready for day-to-day use.

## Automated verification

- Run `scripts/run-full-verification.ps1`
- Run `scripts/run-full-verification.ps1 -IncludeSampleFixtures` on a machine that has the supplied aCar `.abp` and sectioned CSV files
- Confirm `lintDebug` passes with no errors
- Confirm the sample fixture regression still matches the expected counts in [migration-notes.md](D:/Code/Codex/Garage_Ledger/docs/migration-notes.md)

## Device and emulator QA

- Install the latest debug or release build on an Android 13+ device
- Verify first launch, console rendering, and vehicle switching
- Import the sample `.abp` backup and confirm the vehicle counts and recent records look correct
- Import the sectioned CSV into a fresh install and confirm the same record counts
- Create, edit, and delete one fuel-up, service, expense, and trip record
- Save and reload a saved browse search
- Trigger a manual local backup, then restore it into a fresh install
- Attach a photo or PDF to a record and reopen it from the detail screen
- Add the quick add, reminders, fuel efficiency, fuel price, and predictions widgets to the home screen
- Verify pinned shortcuts open the intended quick-entry flows
- Enable reminder notifications and confirm the app can post alerts

## Final sanity checks

- Review [known-differences-vs-acar.md](D:/Code/Codex/Garage_Ledger/docs/known-differences-vs-acar.md) and keep it honest
- Confirm `main` is green in GitHub Actions
- Tag the release commit and capture the APK artifact used for testing

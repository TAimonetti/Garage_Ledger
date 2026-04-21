# Known Differences Vs aCar

This build is intentionally focused on data fidelity and the most important working workflows. It is still not feature-complete against aCar 5.2.17.

## Implemented now

- Local `.abp`, sectioned CSV, and open zipped backup import
- Local sectioned CSV export and open zipped JSON backup export
- Dense console-style start screen
- Picker-backed date and date/time entry across record editors, reminder due dates, browse filters, and vehicle purchase/selling fields
- Settings-backed date formats now affect browse rows, detail screens, dashboard cards, predictions, and reminder summaries
- Vehicle browsing, vehicle detail, and browse/filter across all record families
- Fuel-up add/edit with two-of-three cost math
- Service, expense, and trip add/edit screens
- Recomputed fuel efficiency, reminder schedules, and vehicle stats after import/edit
- WorkManager reminder checks, local rotating backups, and Android reminder notifications
- Import/export center UI with manual local backup trigger and notification enablement
- Local record attachments for photos and PDFs
- Launcher quick actions with home-screen pin requests and vehicle selection
- Home-screen quick add and service reminder widgets
- Statistics and charts screen with local CSV export for summary and chart data
- Record detail screens with edit/delete actions across all main record families
- Editor-level "Customize this screen" support for fuel-up, service, expense, and trip flows
- Settings screen for local units, formats, reminder thresholds, notifications, and optional fields
- Type management for local fuel, service, expense, and trip catalogs
- Vehicle profile photos plus add/edit, retire/reactivate, and destructive delete flows
- Vehicle part management with local add/edit/delete flows
- Reminder center with local add/edit/delete, default seeding, silence toggles, and direct service creation
- Predictions screen and home-screen widget backed by local fill-up cadence and cost history
- Trip helpers for return trips, last-destination starts, open-trip finishing, and end-odometer distance entry
- Local coordinate capture and map-open actions for imported and newly entered records
- Browse filters for subtype, payment type, event place, fuel facets, and trip-specific criteria
- Browse quick date presets, active filter chips, and a collapsible advanced query builder
- Browse action menus for direct view/edit/delete plus trip copy/finish shortcuts
- Retired vehicles keep history visible but block further record edits and deletes
- Browse sort order can be changed in-place and the current filtered browse result set can be exported locally as CSV
- Saved browse searches can be stored, reloaded, deleted, and restored from open backups

## Not complete yet

- No major documented parity gaps remain in the current milestone.
- Real device and emulator QA is still a release task rather than something fully automated inside the repo.

## Intentional differences in the current build

- The editors keep manual ISO-style text entry available even though picker buttons are now built in.
- Preferences are stored as one DataStore snapshot object internally rather than many separate preference keys.
- The app recalculates efficiency and reminder state after import instead of preserving stale derived backup values as authoritative.
- Attachments are copied into app-local storage and referenced through a FileProvider URI instead of relying on external provider URIs directly.

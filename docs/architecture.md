# Architecture

## Short implementation plan

1. Stabilize import and calculation fidelity before chasing visual parity.
2. Preserve aCar's dashboard-first mental model with a console home screen, vehicle switcher, dense stats, and direct record-entry actions.
3. Keep the core local: Room for ledger data, DataStore for settings, Storage Access Framework for files, and no account or server dependency.
4. Treat imported raw records as source-of-truth, then recompute derived fields such as fuel efficiency and reminder due state after import or edit.
5. Add broader parity in phases after the first vertical slice is stable: full detail/customization screens, optional advanced widgets, and attachment/export polish.

## Package structure

- `core.model`
  - unit enums and shared storage-facing types
- `domain.model`
  - business entities and import/report models
- `domain.calc`
  - pure Kotlin rules for MPG, reminders, trip costing, and consistency validation
- `data.importer`
  - aCar CSV, aCar ABP, and Fuelly CSV parsing
- `data.export`
  - sectioned CSV export, statistics CSV export, and open zipped JSON backup export
- `data.backup`
  - local backup file rotation and app-local backup writing
- `attachments`
  - local copy-in storage for picked files, gallery images, and camera captures
- `data.local`
  - Room entities, DAO, converters, and domain/entity mappers
- `data.preferences`
  - DataStore-backed preference snapshot persistence
- `data`
  - repository and import persistence orchestration
- `background`
  - WorkManager scheduler, workers, and worker factory
- `notifications`
  - notification channels and reminder delivery
- `shortcuts`
  - launcher quick actions, pinned shortcut requests, and launch routing
- `widgets`
  - classic RemoteViews-based home-screen widgets for quick add and reminders
- `ui`
  - Compose console, settings, type management, statistics/charts, record detail, import/export center, quick-add chooser, browse records, vehicle detail, and record editors

## Database schema proposal

Primary tables:

- `vehicles`
- `vehicle_parts`
- `fuel_types`
- `service_types`
- `expense_types`
- `trip_types`
- `service_reminders`
- `fillup_records`
- `service_records`
- `expense_records`
- `trip_records`
- `record_attachments`

Join tables:

- `service_record_types`
- `expense_record_types`

Key schema choices:

- All business records use local Room IDs plus optional `legacySourceId` for import traceability.
- Vehicle purchase/sale metadata stays on the vehicle row, matching old aCar behavior.
- Fill-up rows store both raw imported efficiency and recalculated efficiency fields so recalculation is deterministic without losing import visibility.
- Reminder rows store absolute due state (`dueDate`, `dueDistance`) rather than the backup's relative counters, then regenerate from service history after import.
- Preferences are stored as a DataStore snapshot rather than another Room table because the settings are app-scoped, not ledger rows.

## Current implemented flows

1. Import `.abp` or sectioned CSV from local storage.
2. Persist catalogs, vehicles, reminders, and records into Room.
3. Recompute fill-up fuel efficiency and reminder due state.
4. Browse imported vehicles from the console or vehicles list.
5. Open a vehicle detail page with recent fill-ups, services, expenses, trips, and summary stats.
6. Add or edit a fuel-up, service, expense, or trip and immediately refresh derived stats and reminder schedules.
7. Export a readable sectioned CSV or an open zipped JSON backup through the Storage Access Framework.
8. Schedule local rotating backups and reminder checks with WorkManager from imported or local preferences.
9. Capture or attach local photos/PDFs to records from the file picker, gallery, or camera intents.
10. Launch fast-entry flows from pinned launcher shortcuts into a vehicle chooser when needed.
11. Surface quick add and service reminder summaries on the home screen with classic Android widgets.
12. Browse and filter records across all record families, then jump back into the matching editor.
13. Review per-vehicle or all-vehicle statistics with timeframe filtering, simple Compose charts, and local CSV export.
14. Open any browsed record into a detail screen, then edit or permanently delete it with explicit confirmation.
15. Toggle optional editor fields on and off with the same imported visible-field preferences used for aCar migration.
16. Adjust app settings locally for units, formats, notification thresholds, and backup policy with no account dependency.
17. Manage fuel, service, expense, and trip type catalogs directly on-device through a dedicated type management screen.
18. Preview Fuelly CSV headers locally, map non-standard columns, choose units, and import fill-ups into an existing vehicle.

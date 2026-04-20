# Architecture

## Short implementation plan

1. Stabilize import and calculation fidelity before chasing visual parity.
2. Preserve aCar’s dashboard-first mental model with a console home screen, vehicle switcher, dense stats, and direct record-entry actions.
3. Keep the core local: Room for ledger data, DataStore for settings, Storage Access Framework for files, and no account or server dependency.
4. Treat imported raw records as source-of-truth, then recompute derived fields such as fuel efficiency and reminder due state after import or edit.
5. Add broader parity in phases after the first vertical slice is stable: browse/filter, service/expense/trip editors, widgets/shortcuts, backup/export center, reminder workers, and attachment polish.

## Package structure

- `core.model`
  - unit enums and shared storage-facing types
- `domain.model`
  - business entities and import/report models
- `domain.calc`
  - pure Kotlin rules for MPG, reminders, trip costing, and consistency validation
- `data.importer`
  - aCar CSV, aCar ABP, and Fuelly CSV parsing
- `data.local`
  - Room entities, DAO, converters, and domain/entity mappers
- `data.preferences`
  - DataStore-backed preference snapshot persistence
- `data`
  - repository and import persistence orchestration
- `ui`
  - Compose console, import center, vehicle detail, and fuel-up editor

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
- Reminder rows store absolute due state (`dueDate`, `dueDistance`) rather than the backup’s relative counters, then regenerate from service history after import.
- Preferences are stored as a DataStore snapshot rather than another Room table because the settings are app-scoped, not ledger rows.

## First implemented flow

1. Import `.abp` or sectioned CSV from local storage.
2. Persist catalogs, vehicles, reminders, and records into Room.
3. Recompute fill-up fuel efficiency and reminder due state.
4. Browse imported vehicles from the console or vehicles list.
5. Open a vehicle detail page with recent fill-ups and summary stats.
6. Add or edit a fuel-up and immediately refresh derived stats.

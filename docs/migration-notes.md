# Migration Notes

## Sample fixture expectations

The supplied sample files are wired into regression tests with these expectations:

- Vehicles: 4
- Fill-ups: 328
- Service records: 19
- Expense records: 0
- Trip records: 0
- Vehicle parts: 1
- Master services: 43
- Master expense types: 9
- Master trip types: 6
- Master fuel types: 28

Vehicle-specific expectations:

- Corolla: 264 fill-ups, 8 service records
- Tundra: 64 fill-ups, 4 service records
- Acadia: 0 fill-ups, 6 service records
- Altima: 0 fill-ups, 1 service record

Imported preference expectations:

- Distance unit: `mi`
- Volume unit: `gal (US)`
- Fuel efficiency unit: `MPG (US)`
- Currency: `$`
- Fuel efficiency method: `previous-record`
- Browse sort: descending
- Backup history count: `10`

## Current assumptions

- CSV vehicle matching merges by normalized vehicle name when importing into a non-empty local database.
- Reminder due state is recalculated from service history instead of preserving the backup’s relative due counters directly.
- Fuel type normalization is strongest with `.abp` imports because the backup contains the full fuel catalog; CSV-only imports keep raw fuel type text when needed.
- Preferences are stored as one DataStore snapshot blob for simplicity and migration safety in phase 1.

## Practical migration guidance

- Use the `.abp` import first when possible because it carries the master catalogs, reminders, and preferences.
- Use the sectioned CSV as a second validation path or as a merge source when a full backup is not available.
- After import, verify per-vehicle fill-up counts and a few known MPG values before adding new records.
- Run `scripts/run-sample-fixture-regression.ps1` to verify the supplied sample files directly, or pass `-DgarageLedger.fixture.abp=<path>` and `-DgarageLedger.fixture.csv=<path>` to `testDebugUnitTest` on another machine.

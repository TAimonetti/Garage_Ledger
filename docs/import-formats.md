# Import Formats

## aCar sectioned CSV

Supported sections:

- `Metadata`
- `Vehicles`
- `Fill-Up Records`
- `Service Records`
- `Expense Records`
- `Trip Records`

Importer behavior:

- Reads metadata date/time patterns and falls back to `MM/dd/yyyy` + `hh:mm a`
- Accepts quoted odometer values with commas
- Accepts currency symbols in price and total columns
- Preserves tags and notes
- Creates dynamic service/expense/trip type catalogs from CSV record text when no master catalog is present
- Leaves fuel type as imported text when no normalized catalog is available

Current assumption:

- Multiline CSV fields are not handled yet; notes are expected to stay on one line

## aCar `.abp`

The importer treats `.abp` as a ZIP archive and parses:

- `metadata.inf`
- `preferences.xml`
- `services.xml`
- `expenses.xml`
- `trip-types.xml`
- `fuel-types.xml`
- `vehicles.xml`

Importer behavior:

- Imports master catalogs before record families
- Imports visible-field preferences and unit preferences into DataStore
- Parses nested vehicle parts, reminders, fill-ups, services, expenses, and trips from `vehicles.xml`
- Maps reminder `service-id` references through the imported service catalog
- Recomputes derived values after persistence instead of trusting stale backup cache fields

## Fuelly CSV

Current support is fill-up focused and handles these common columns:

- `odometer` or `miles` or `km`
- `gallons` or `litres`
- `price`
- `city_percentage`
- `fuelup_date`
- `tags`
- `notes`
- `brand`
- `missed_fuelup`
- `partial_fuelup`

The parser already accepts an explicit field-mapping dictionary so the UI can grow into a real mapping screen later without rewriting the importer.

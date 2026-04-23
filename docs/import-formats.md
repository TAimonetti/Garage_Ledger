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

Importer behavior:

- Opens through the Import & Export Center and targets an existing vehicle
- Previews detected headers and sample rows before writing anything
- Suggests field mappings from common Fuelly-style column names
- Lets the user override distance and volume units when headers are ambiguous
- Blocks import until odometer, volume, total price, and date are mapped cleanly
- Skips malformed rows with an import note instead of silently inventing values

Current assumptions:

- Fuelly import still focuses on total-price CSVs, not separate price-per-unit exports
- Ambiguous `odometer` or `volume` headers default to the current local unit setting until the user changes them

## Current export formats

### Sectioned CSV

The app exports a readable sectioned CSV that mirrors the import-oriented structure:

- `Metadata`
- `Vehicles`
- `Fill-Up Records`
- `Service Records`
- `Expense Records`
- `Trip Records`

Behavior:

- Writes a human-readable CSV meant for inspection and round-trip parsing
- Uses the current local preference snapshot for currency/unit formatting
- Resolves normalized service, expense, and trip type names into the exported text

### Statistics HTML

The app can also export the current statistics dashboard as a standalone HTML file.

Behavior:

- Uses the same vehicle/timeframe filter state as the on-screen statistics view
- Includes overall, fill-up, service, expense, and trip summary sections
- Embeds simple inline SVG charts so the report stays local and self-contained
- Includes chart data tables in the HTML so the export remains open and inspectable

### Open zipped JSON backup

The app exports and restores a ZIP with two entries:

- `metadata.json`
- `guzzlio-backup.json`

Behavior:

- Stores the full local ledger, catalogs, reminders, attachments, cross-reference tables, and preferences
- Restores back into local storage as a full replacement import through the Import & Export Center
- Uses ISO-8601 strings for date/time fields for low-friction migration
- Writes only open JSON, never a proprietary binary payload

# Known Differences Vs aCar

This build is intentionally focused on data fidelity and the most important working workflows. It is still not feature-complete against aCar 5.2.17.

## Implemented now

- Local `.abp` and sectioned CSV import
- Local sectioned CSV export and open zipped JSON backup export
- Dense console-style start screen
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

## Not complete yet

- Optional fuel-efficiency and fuel-price widgets
- Optional HTML statistics export
- Fuelly CSV import field-mapping UI

## Intentional differences in the current build

- The editors use text-based date/time entry (`yyyy-MM-dd HH:mm`) for now instead of a classic dialog-heavy aCar flow.
- Preferences are stored as one DataStore snapshot object internally rather than many separate preference keys.
- The app recalculates efficiency and reminder state after import instead of preserving stale derived backup values as authoritative.
- Browse filtering is currently local and text-driven rather than a full advanced query builder.
- Attachments are copied into app-local storage and referenced through a FileProvider URI instead of relying on external provider URIs directly.

# Known Differences Vs aCar

This build is intentionally focused on data fidelity and the first working workflow. It is not yet feature-complete against aCar 5.2.17.

## Implemented now

- Local `.abp` and sectioned CSV import
- Dense console-style start screen
- Vehicle browsing and vehicle detail
- Fuel-up add/edit with two-of-three cost math
- Recomputed fuel efficiency and stats after import/edit

## Not complete yet

- Full service, expense, and trip editor screens
- Browse/search/filter across all record families
- Widgets and pinned shortcuts
- WorkManager reminder checks and local backup scheduling
- Attachments UI
- Chart rendering and statistics export screens
- Open zipped JSON backup UI/export path
- Full field-customization screens mirroring aCar’s “Customize this screen”

## Intentional differences in phase 1

- The fuel-up editor uses a text-based date/time format (`yyyy-MM-dd HH:mm`) for now instead of a classic dialog-heavy aCar flow.
- Preferences are stored as one DataStore snapshot object internally rather than many separate preference keys.
- The app recalculates efficiency/reminder state after import instead of preserving stale derived backup values as authoritative.

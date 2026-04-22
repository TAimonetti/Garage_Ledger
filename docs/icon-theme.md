# Icon Theme Direction

This document defines an aCar-inspired but original icon direction for Garage Ledger.

## Reference Points

Primary visual references:

- [Fuelly Docs: Getting Started](https://docs.fuelly.com/acar-getting-started)
- [Fuelly Docs: New Fuel-Up Record](https://docs.fuelly.com/acar-new-fuelup-record)
- [Fuelly Docs: More Options](https://docs.fuelly.com/acar-more-options)

Local structural reference from the decompiled 2018 app:

- `aCar-5.2.17/resources/res/layout/home.xml`

Important note:

- We are not copying Fuelly artwork, icon files, or trademarks.
- We are borrowing the feel of the old console: dense, colorful, mnemonic, and easy to scan.

## What Made aCar's Console Work

- Each action had a distinct pictogram, not just a generic Material icon.
- Colors were used as memory anchors.
- The shapes were simple enough to read at very small sizes.
- Most console actions looked like "tiny illustrated tools" instead of flat outline glyphs.
- Labels were short and secondary to the icon itself.

## Garage Ledger Direction

We should keep the same strengths:

- bright semantic colors
- one strong pictogram per action
- compact tile proportions
- short labels
- consistent icon container shape

We should avoid:

- copying aCar assets directly
- over-detailed icons that blur at phone size
- all-blue or all-gray controls
- relying on text alone to find actions

## Shape Language

Recommended family:

- rounded square or softly rounded badge
- bold silhouette
- one main object plus one supporting cue when needed
- minimal internal line work

Examples:

- fuel-up: pump silhouette with plus cue
- service: wrench or support tool
- expense: banknote or receipt
- trip: road or route line
- reminders: alarm clock
- predictions: crystal-ball-like "forecast" cue, interpreted legally as insights/forecast rather than copied artwork

## Color System

These are the first-pass dashboard colors now reflected in code.

| Action | Motif | Palette |
| --- | --- | --- |
| Fuel-Up | pump | teal green |
| Service | tool | service blue |
| Expense | money/receipt | warm amber |
| Trip | road | orange |
| Add Vehicle | car with add intent | red |
| Browse | search/explore | sky blue |
| Stats | calculator/stat summary | green |
| Charts | chart | aqua |
| Details | vehicle detail/edit | brick red |
| Reminders | alarm | golden yellow |
| Parts | tire/part | violet |
| Predictions | forecast/insight | indigo blue |
| Import | archive box | steel blue |
| Vehicles | garage/fleet | slate |
| Settings | gear/tune | gray |
| Types | category/tag | blue-violet |

## Action-by-Action Motif Ideas

### Fuel-Up

- Best fit: pump
- Optional accent: small plus spark or droplet
- Why: strongest old-aCar memory anchor

### Service

- Best fit: wrench, support arm, or tool cross
- Why: aCar used a support-tool style icon, not a plain text metaphor

### Expense

- Best fit: banknote stack or receipt stub
- Why: finance icon is faster to spot than a generic document

### Trip

- Best fit: curved road with center line
- Why: more memorable than a map pin

### Add Vehicle

- Best fit: car silhouette with add cue
- Why: should read instantly as creation, not browsing

### Browse

- Best fit: globe/search or file/search
- Why: old aCar browse felt like exploring records, not just filtering

### Stats

- Best fit: calculator/stat board
- Why: old aCar's mental model separated statistics from charts

### Charts

- Best fit: combo chart or rising bars
- Why: should look visibly different from Stats

### Details

- Best fit: car + lens, or edit card
- Why: "vehicle details" should feel vehicle-specific

### Reminders

- Best fit: alarm clock
- Why: strong carry-over from old aCar

### Parts

- Best fit: tire, wheel, or part box
- Why: different from service, less tool-heavy

### Predictions

- Best fit: insight graph, spark, or forecast badge
- Why: preserve the old "prediction" feeling without copying the crystal-ball artwork

## Implementation Plan

### Phase 1

- Use color-coded action themes on the console
- Keep current vector icons, but assign them consistent color families
- Tighten card size and icon container styling

### Phase 2

- Replace the most generic Material icons with custom vector drawables or Compose vectors
- Start with: Fuel-Up, Service, Expense, Trip, Reminders, Predictions

### Phase 3

- Apply the same icon language to widgets, pinned shortcuts, record headers, and browse filters

## Immediate Next Custom Icons To Draw

1. Fuel-Up pump badge
2. Service wrench badge
3. Expense banknote badge
4. Trip road badge
5. Reminder alarm badge
6. Predictions insight badge

These six will do most of the work in making the app feel more like the old aCar console.

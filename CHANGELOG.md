# Changelog

## 1.1.1 - 28 Aug 2026

### Fixes

- Fixes custom theme color edits not being saved for Custom Color Bases when switching between bases.
- Fixes resetting a custom theme color while using a preset base restoring the `Classic` default instead of that base's default.

## 1.1.0 - 28 Aug 2026

### Features

- Adds a new grey/white `Light` theme preset.
- Renames the original light preset to `Light classic`.
- Adds a `Custom color base` dropdown to use a preset as the starting point for a custom theme.
- Adds persistent `Custom 1`, `Custom 2`, and `Custom 3` color bases for saving custom theme variants.
- Adds configurable text shadow behavior with `Auto`, `On`, and `Off` modes.
- Adds an option to use bold text throughout the overlay.
- Adds options for item and stats tooltip backgrounds and label/value text to match the selected theme or custom overlay colors, with separate tooltip transparency controls.
- Adds custom overlay colors for stats labels, stats levels, active tabs, and inactive tabs.

### Fixes

- Fixes doubled-looking overlay text on light backgrounds by disabling text shadows automatically for light themes.
- Fixes stats tab level colors not following custom overlay colors or theme presets.
- Fixes tab text being tied to the combat text color.


## 1.0.0 - 11 Aug 2026

Initial release of Player Examine, including:

### Features

- Adds a right-click `Examine` option for players.
- Opens a movable RuneLite overlay after examining a player.
- Renders `Visual`, `List`, and `Hybrid` equipment layouts.
- Supports an optional stats tab with visual or list-style display.
- Shows hover tooltips for equipped items.
- Supports GE, HA, and total value display.
- Supports long or compact value formatting for item hover values and overlay footer totals, with compact formatting as the default.
- Supports item bonus comparison against your currently equipped item.
- Supports optional item wiki search on slot click.
- Supports separate hover tooltip controls for item text, labels, values, and bonus deltas.
- Shows stats hover tooltips with skill name, rank, experience, and remaining XP.
- Uses a hiscore-backed stats tab with OSRS skill ordering.
- Uses OSRS skill icons in the visual stats tab, with optional weapon icon overrides for Attack.
- Supports configurable overlay width, transparency, light and themed color presets including Saradomin, opening glow, and text colors.
- Supports choosing Equipment, Stats, or Remember last from a single default tab setting.
- Supports optional GE value threshold glow on equipment slot borders.
- Keeps the overlay open through short loading transitions such as tunnels and area changes.
- Auto-expands list and hybrid overlays when item text would otherwise clip.
- Lets you add the `(Members)` suffix in item names.
- Includes configurable overlay colors, list-style colors, tooltip colors, and stats hover tooltip settings.
- Includes update notices for new plugin versions.

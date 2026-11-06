# Crosshair-Selection

English | [简体中文](README.zh-CN.md)

A client-side mod for Minecraft 1.7.10 / GTNH. It replaces the vanilla block selection box and
crosshair with customizable versions.

Download: [Releases](https://github.com/kanami2024520-jpg/Crosshair-Selection/releases)

## Features

### Selection box

- Colour, thickness, outline margin and see-through mode
- Optional inner fill with its own settings page: fill toggle, blink toggle, blink alpha and speed
- Flowing rainbow with hue range, saturation, brightness, motion curve, direction and sweep
- Linked doors, beds, double plants and pistons
- Optional smooth transition between blocks
- Part-aware selection: GT cover plates, GT pipe/cable targeting, Forge Multipart/ProjectRed wires,
  Forge Microblocks, AE2 cables/facades, EnderIO conduits and Translocator attachments
- Optional mining progress indicator: rising fill on the block face or a percentage read-out,
  each with its own screen position

### GT overlay

- Replaces GT's wrench/cover overlay for machines and pipes
- 3x3 grid with configurable cell size, inset and line width
- Input/output markers with Cross / Arrow / Fill / Full X styles
- Grid-accurate clicks and an option to ignore clicks outside the grid
- Pipe outline that either follows the selection box style or uses its own colour and width

### Crosshair

- Shapes: cross and vanilla; colour, size, gap, thickness, rotation
- Outline and centre dot with independent colours
- Flowing rainbow and a separate crosshair flow editor
- Attack and placement feedback driven by a target-value state machine, so rapid clicks stay
  responsive; expand and return speeds are configurable
- Bow and TiC crossbow: the crosshair opens while aiming and closes as the draw completes,
  with an optional percentage or `↑` ready marker and selectable screen position
- Visibility conditions (normal, F1, F3, third person) on their own page

### Rendering

- Selection outlines are drawn as camera-facing quads resolved by a capsule distance field in the
  fragment shader, which keeps edges smooth at any angle and closes corners at any thickness
- The crosshair is a full-screen SDF pass, so rotation, scaling and feedback never get clipped
- Colours run in world space, so they stay put while the camera moves

### Interface

- Preview panel shared by every page
- Flow-based layout: controls reflow instead of using fixed coordinates
- Sub-pages for fill settings, crosshair details, visibility and the GT overlay
- `/csc` opens the settings screen

### BetterQuesting back key

BetterQuesting hardcodes Backspace as its page back key. With BetterQuesting installed this mod
registers a **Back** key in the `BetterQuesting` key category, so it can be rebound to any key or
mouse side button. It defaults to Backspace, so behaviour is unchanged until you change it.
Text fields still delete characters normally.

## Install

Every release ships two jars. Pick **one**:

| Jar | Contents |
| --- | --- |
| `crosshairselection-<version>-patched.jar` | **Recommended.** Everything below, plus the BetterQuesting back-key patch. |
| `crosshairselection-<version>-clean.jar` | The mod without that patch, for packs that do not want another rebindable key. |

Put the jar in `mods`. Requires Forge 10.13.4.1614, GTNHLib and UniMixins. Tested on GTNH
2.9.0-stable.

Settings are under **Mods → Crosshair-Selection → Config**, or run `/csc`.

## Build

JDK 25 is required.

```bash
./gradlew build
```

Two jars are produced in `build/libs/`:

- `crosshairselection-<version>-patched.jar` — includes the BetterQuesting back-key patch
- `crosshairselection-<version>-clean.jar` — without it

## Credits

Inspired by [shedaniel/CustomSelectionBox-New](https://github.com/shedaniel/CustomSelectionBox-New),
[xiej2520/CustomSelectionBox](https://github.com/xiej2520/CustomSelectionBox) and
[ArchiDreamZ/Custom-Crosshair-Mod-1.7.10](https://github.com/ArchiDreamZ/Custom-Crosshair-Mod-1.7.10).
Built with [GTNHLib](https://github.com/GTNewHorizons/GTNHLib),
[Angelica](https://github.com/GTNewHorizons/Angelica) and
[LegacyModdingMC/UniMixins](https://github.com/LegacyModdingMC/UniMixins).

## License

MIT.

# Crosshair-Selection

English | [简体中文](README.zh-CN.md)

## Introduction

A client-side mod for Minecraft 1.7.10 / GTNH that replaces the vanilla block selection box and
crosshair, with a few optional patches on the side.

Open the settings with `/csc` or **Mods → Crosshair-Selection → Config**.

## Features

### Selection box

- Optional inner fill, smooth transition and see-through mode;
- Configurable rainbow flow: hue range, speed, direction, band density and opacity;
- Per-part handling for GT covers, GT pipes/cables, Forge Multipart/ProjectRed wires,
  Forge Microblocks, AE2 cables/panels, EnderIO conduits and Translocator attachments;
- Optional mining progress: a bar on the block face or a percentage read-out, each with its own
  screen position.

### GT overlay

- Replaces GT's wrench/cover overlay for machines and pipes (optional);
- 3x3 grid with adjustable cell width, inset and line width;
- Input/output markers in Cross, Arrow, Fill and Full X styles;
- Clicks match the rendered grid, with an option to ignore clicks outside it.

### Crosshair

- Many adjustable parameters: shape, colour, size, gap, thickness, rotation, outline, centre dot;
- Configurable dynamic behaviour: attack/placement feedback and bow draw contraction;
- HSV picker with alpha;
- Compatible with Tinkers' Construct bows and crossbows.

### Extra patches

The mod ships a small number of patches for other mods. They are optional, and currently there is
one.

**BetterQuesting back key.** BetterQuesting hardcodes Backspace as its page back key. With
BetterQuesting installed this mod registers a **Back** key in the `BetterQuesting` key category,
which can be rebound to any keyboard key or mouse side button. It defaults to Backspace, so
behaviour is unchanged until you change it; text fields still delete characters normally.

## Install

Every release ships two jars. Install only one:

| Jar | Contents |
| --- | --- |
| `crosshairselection-<version>-patched.jar` | Includes the patches (recommended) |
| `crosshairselection-<version>-clean.jar` | Without the patches |

Since 2.0 the rendering was rebuilt from scratch. Requires **Forge 10.13.4.1614, GTNHLib,
UniMixins and Angelica**.

Tested on GTNH 2.9.0-stable.

## Build

JDK 25 is required.

```bash
./gradlew build
```

The jars are in `build/libs/`.

## Credits

Inspired by [shedaniel/CustomSelectionBox-New](https://github.com/shedaniel/CustomSelectionBox-New),
[xiej2520/CustomSelectionBox](https://github.com/xiej2520/CustomSelectionBox) and
[ArchiDreamZ/Custom-Crosshair-Mod-1.7.10](https://github.com/ArchiDreamZ/Custom-Crosshair-Mod-1.7.10).

## License

MIT.

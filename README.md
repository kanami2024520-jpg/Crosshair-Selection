# Crosshair-Selection

English | [简体中文](README.zh-CN.md)

A client-side mod for Minecraft 1.7.10 / GTNH. It replaces the vanilla block selection box and
crosshair with customizable versions.

Download: [Releases](https://github.com/kanami2024520-jpg/Crosshair-Selection/releases)

## Features

- Selection box: RGBA colors, thickness, outline margin, blinking fill, rainbow, linked blocks, hidden-block mode
- Crosshair: cross, circle, square, vanilla, arrow and triangle shapes; RGBA colors, size, gap,
  thickness, rotation, outline, center dot and rainbow
- Live preview in the config screen

## Install

Put the jar in `mods`. Requires Forge 10.13.4.1614, GTNHLib and UniMixins. Tested on GTNH
2.9.0-RC-2.

Settings are under **Mods → Crosshair-Selection → Config**. There is no keybind or command.

## Build

JDK 25 is required.

```bash
./gradlew build
```

The jar is in `build/libs/`.

## Credits

Inspired by [shedaniel/CustomSelectionBox-New](https://github.com/shedaniel/CustomSelectionBox-New),
[xiej2520/CustomSelectionBox](https://github.com/xiej2520/CustomSelectionBox) and
[ArchiDreamZ/Custom-Crosshair-Mod-1.7.10](https://github.com/ArchiDreamZ/Custom-Crosshair-Mod-1.7.10).
Built with [GTNHLib](https://github.com/GTNewHorizons/GTNHLib),
[Angelica](https://github.com/GTNewHorizons/Angelica) and
[LegacyModdingMC/UniMixins](https://github.com/LegacyModdingMC/UniMixins).

## License

MIT.

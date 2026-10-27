# Crosshair-Selection

A client-side Minecraft **1.7.10 / GTNH** mod that combines a custom block selection box with a
custom crosshair. Written from scratch; feature ideas were borrowed from the projects credited
below, but no source code was copied.

- Repository: <https://github.com/kanami2024520-jpg/Crosshair-Selection>
- Releases: <https://github.com/kanami2024520-jpg/Crosshair-Selection/releases>
- Author: **kuionc**
- Mod id: `crosshairselection`
- Requires: Minecraft 1.7.10, Forge 10.13.4.1614, **UniMixins**, **GTNHLib**
- Tested with: GT New Horizons 2.9.0-RC-2, Angelica 2.2.28, GTNHLib 0.11.52

## Features

**Selection box**

- Custom colour with alpha
- Thickness, blinking fill, rainbow mode
- Link blocks: doors, beds, double plants, pistons
- Show hidden blocks (disable depth test)
- Live 3D preview in the settings screen

**Crosshair**

- Shapes: cross, circle, square, vanilla, arrow, triangle
- Colour with alpha, width, height, gap, thickness, rotation
- Outline and centre dot with their own colours
- Rainbow mode and speed
- Visibility: default, F1, F3, third person
- Static gap (no bow-draw/spread animation by design)
- Live preview in the settings screen

The vanilla crosshair is hidden automatically while the custom crosshair is enabled.

## Settings

Open **Mods → Crosshair-Selection → Config** on the main menu. There is no command and no
default keybind (by design).

Colours are edited with the built-in RGBA picker: RGB/A sliders, hue bar, checkerboard preview and
hex readout. The selection box and all crosshair colours use the same picker.

## Config

- `config/crosshairselection/config.cfg` (GTNHLib/Forge config format)
- On first launch the old `config/CSB/config.json` is imported automatically if present
- Old `customselectionbox-*.jar` must be removed; the mod id changed to `crosshairselection`

## Build

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew build
```

The output jar is in `build/libs/`. GTNH's build plugin requires JDK 25 to run Gradle; the mod
itself targets Java 8.

## Releases

GitHub Actions builds every push to `main` and every pull request. To publish a release:

1. Set `modVersion` in `gradle.properties`.
2. Commit the change.
3. Push a matching tag, for example `v1.0.0`.
4. The release workflow builds the mod and attaches the main and sources jars to the GitHub
   Release.

## Credits

Feature inspiration and design reference:

- [shedaniel/CustomSelectionBox-New](https://github.com/shedaniel/CustomSelectionBox-New) — original selection box mod
- [xiej2520/CustomSelectionBox](https://github.com/xiej2520/CustomSelectionBox) — feature-complete 1.12.2 port that guided the selection box feature set
- [ArchiDreamZ/Custom-Crosshair-Mod-1.7.10](https://github.com/ArchiDreamZ/Custom-Crosshair-Mod-1.7.10) — 1.7.10 crosshair mod by Sparkless101 / 彼梦; the crosshair feature set is based on its behaviour
- [GTNewHorizons/GTNHLib](https://github.com/GTNewHorizons/GTNHLib) — config system and Tessellator bridge
- [GTNewHorizons/Angelica](https://github.com/GTNewHorizons/Angelica) — GL/Tessellator compatibility
- [LegacyModdingMC/UniMixins](https://github.com/LegacyModdingMC/UniMixins) — Mixin runtime for 1.7.10

## License

MIT. See [LICENSE](LICENSE).

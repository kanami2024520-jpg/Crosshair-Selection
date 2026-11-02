# Crosshair-Selection

[English](README.md) | 简体中文

一个适用于 Minecraft 1.7.10 / GTNH 的客户端模组，用来替换原版的方块选择框和准星。

下载：[Releases](https://github.com/kanami2024520-jpg/Crosshair-Selection/releases)

## 功能

- 选择框：RGBA 颜色、粗细、外框边距、闪烁填充、流光彩虹、联动方块、透视选中框、可选平滑过渡
- GT 覆盖层：替换 GT 机器/管道的扳手与覆盖板覆盖层，九格网格、外格宽度、输出/连接 X 标记，支持十字/箭头/填满/全格 X，点击范围与九格一致
- 准星：十字、原版；RGBA 颜色、大小、间距、粗细、旋转、描边、中心点、流光彩虹、弓箭蓄力
- 渲染选项：逐片元纹理流光，纹理尺寸 Auto/128/256/512，抗锯齿开关与粗线边缘羽化
- 配置界面实时预览

## 安装

把 jar 放进 `mods`。需要 Forge 10.13.4.1614、GTNHLib 和 UniMixins，已在 GTNH 2.9.0-RC-2 测试。

设置位于 **Mods → Crosshair-Selection → Config**，也可以执行 `/crosshairselection`
（别名 `/csb`）。没有快捷键。

## 构建

需要 JDK 25。

```bash
./gradlew build
```

产物在 `build/libs/`。

## 致谢

灵感来自 [shedaniel/CustomSelectionBox-New](https://github.com/shedaniel/CustomSelectionBox-New)、
[xiej2520/CustomSelectionBox](https://github.com/xiej2520/CustomSelectionBox) 和
[ArchiDreamZ/Custom-Crosshair-Mod-1.7.10](https://github.com/ArchiDreamZ/Custom-Crosshair-Mod-1.7.10)。
使用了 [GTNHLib](https://github.com/GTNewHorizons/GTNHLib)、
[Angelica](https://github.com/GTNewHorizons/Angelica) 和
[LegacyModdingMC/UniMixins](https://github.com/LegacyModdingMC/UniMixins)。

## 许可证

MIT。

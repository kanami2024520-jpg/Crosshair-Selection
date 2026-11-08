# Crosshair-Selection

[English](README.md) | 简体中文

## 介绍

一个专门适用于 Minecraft 1.7.10 / GTNH 的客户端模组，用来替换原版的方块选择框和准星，并附带少量可选补丁。

`/csc` 或 **Mods → Crosshair-Selection → Config** 打开设置界面。

## 功能

### 选择框

- 可选的内部填充、平滑过渡、透视模式；
- 可自定义的流光彩虹：色相范围、速度、方向、色带密度、透明度；
- 针对 GT 覆盖板、GT 管道/线缆、Forge Multipart/ProjectRed 线缆、Forge Microblock、
  AE2 线缆/面板、EnderIO 管道和 Translocator 附件做单独优化；
- 可选破坏进度显示：方块面上的进度条或百分比数字，位置可分别设置。

### GT 覆盖层

- 可选替换 GT 机器/管道的扳手与覆盖板覆盖层；
- 九格网格：外格宽度、内缩、线宽可调；
- 支持多种输出/连接标记（十字、箭头、填满、全格 X）；
- 点击范围与本模组渲染的九格一致，可忽略九格之外的点击。

### 准星

- 多个可设置参数：形状、颜色、大小、间距、粗细、旋转、描边、中心点；
- 可设置的动态准星：攻击/放置反馈、拉弓收紧；
- HSV 调色盘与透明度；
- 兼容匠魂弩和弓。

### 额外补丁

本模组也包含少量作者对其他模组的补丁，目前只有一个，且是可选的。

**BetterQuesting 返回键**：BetterQuesting 把返回键写死为 Backspace。安装 BetterQuesting 时，
本模组会在 `BetterQuesting` 按键分类下注册一个 **返回** 键，可以改成任意键盘键或鼠标侧键。
默认仍是 Backspace，不改动时行为不变；文本框输入不受影响。

## 安装

每次发布提供两个 jar，只装其中一个：

| Jar | 说明 |
| --- | --- |
| `crosshairselection-<version>-patched.jar` | 包含补丁（推荐） |
| `crosshairselection-<version>-clean.jar` | 不含补丁 |

从 2.0 版本开始彻底重构了渲染方式，需要 **Forge 10.13.4.1614、GTNHLib、UniMixins 和 Angelica**。

已在 GTNH 2.9.0-stable 测试。

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

## 许可证

MIT。

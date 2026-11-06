# Crosshair-Selection

[English](README.md) | 简体中文

一个适用于 Minecraft 1.7.10 / GTNH 的客户端模组，用来替换原版的方块选择框和准星。

下载：[Releases](https://github.com/kanami2024520-jpg/Crosshair-Selection/releases)

## 功能

### 选择框

- 颜色、粗细、外框边距、透视模式
- 可选内部填充，独立页面：填充开关、闪烁开关、闪烁透明度、闪烁速度
- 流光彩虹：色相范围、饱和度、亮度、运动曲线、方向、扫光
- 联动门、床、双格植物、活塞
- 可选平滑过渡
- 按部件选择：GT 覆盖板、GT 管道/线目标、Forge Multipart/ProjectRed 线、Forge Microblock、AE2 线/面板、EnderIO 管道和 Translocator 附件
- 可选破坏进度显示：方块面上的进度条或百分比数字，位置可分别设置

### GT 覆盖层

- 替换 GT 机器/管道的扳手与覆盖板覆盖层
- 九格网格：外格宽度、内缩、线宽可调
- 输出/连接标记：十字 / 箭头 / 填满 / 全格 X
- 点击范围与九格一致，可忽略九格之外的点击
- 管道描边可跟随选择框样式，也可单独设置颜色与粗细

### 准星

- 形状：十字、原版；颜色、大小、间距、粗细、旋转
- 描边与中心点，颜色独立
- 流光彩虹，以及单独的准星流光编辑页
- 攻击/放置反馈由目标值状态机驱动，连续快速点击依然跟手；扩张与归位速度可调
- 弓与匠魂弩：瞄准时准星张开，拉满回到原位；可选百分比或 `↑` 就绪标记，位置可选
- 显示条件（正常 / F1 / F3 / 第三人称）独立成页

### 渲染

- 选择框由朝向相机的四边形构成，在片段着色器里用胶囊体距离场求解，任意角度边缘平滑，任意粗细拐角闭合
- 准星是全屏 SDF 绘制，旋转、缩放、反馈位移都不会被裁剪
- 颜色基于世界坐标计算，移动视角时不会跟着屏幕漂移

### 界面

- 所有页面共用一个预览面板
- 流式布局：控件自动换行，不再使用固定坐标
- 子页面：填充设置、准星详情、显示条件、GT 覆盖层
- `/csc` 打开设置界面

### BetterQuesting 返回键

BetterQuesting 把返回键写死为 Backspace。安装 BetterQuesting 时，本模组会在 `BetterQuesting`
按键分类下注册一个 **返回** 键，可以改成任意键盘键或鼠标侧键。默认仍是 Backspace，不改动时行为
不变；文本框输入不受影响。

## 安装

把 jar 放进 `mods`。需要 Forge 10.13.4.1614、GTNHLib 和 UniMixins，已在 GTNH 2.9.0-stable 测试。

设置位于 **Mods → Crosshair-Selection → Config**，也可以执行 `/csc`。

## 构建

需要 JDK 25。

```bash
./gradlew build
```

`build/libs/` 会产出两个 jar：

- `crosshairselection-<version>-patched.jar` —— 包含 BetterQuesting 返回键补丁
- `crosshairselection-<version>-clean.jar` —— 不含该补丁

## 致谢

灵感来自 [shedaniel/CustomSelectionBox-New](https://github.com/shedaniel/CustomSelectionBox-New)、
[xiej2520/CustomSelectionBox](https://github.com/xiej2520/CustomSelectionBox) 和
[ArchiDreamZ/Custom-Crosshair-Mod-1.7.10](https://github.com/ArchiDreamZ/Custom-Crosshair-Mod-1.7.10)。
使用了 [GTNHLib](https://github.com/GTNewHorizons/GTNHLib)、
[Angelica](https://github.com/GTNewHorizons/Angelica) 和
[LegacyModdingMC/UniMixins](https://github.com/LegacyModdingMC/UniMixins)。

## 许可证

MIT。

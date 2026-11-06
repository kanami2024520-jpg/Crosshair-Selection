/**
 * 构建两个产物：
 *
 *   1. crosshairselection-<version>-patched.jar  —— 带补丁版（默认测试用）
 *   2. crosshairselection-<version>-clean.jar    —— 不含可选补丁
 *
 * 关键点：两者都必须以 **reobfJar 的输出 jar** 为底。
 * reobfJar 会把开发名（thePlayer）改写为运行时名（field_71439_g）；
 * 直接从 sourceSets.main.output 打包得到的是开发名 class，
 * 加载后第一次访问原版字段/方法就抛 NoSuchFieldError / NoSuchMethodError，
 * 表现为 mixin 与渲染全部静默失效。
 *
 * 底包由 GTNH 约定生成在 build/libs/<modId>-<version>.jar，
 * 这里读取它并重新打包成两个分发文件。
 */
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

/** 只有带补丁版才需要的类（按前缀匹配，覆盖内部类）。 */
val patchClassPrefixes = listOf(
    "me/kuionc/crosshairselection/compat/BuiltinPatches",
    "me/kuionc/crosshairselection/input/BetterQuestingKeybinds",
    "me/kuionc/crosshairselection/input/BetterQuestingBackHandler",
    "me/kuionc/crosshairselection/mixin/MixinGuiScreenKeyboard",
    "me/kuionc/crosshairselection/mixin/AccessorKeyBinding"
)

/** 干净版需要从 mixin 配置里删除的条目。 */
val patchMixins = listOf("MixinGuiScreenKeyboard", "AccessorKeyBinding")

/** GTNH 约定的产物名：modId + 版本（从 gradle.properties 读取）。 */
val modIdValue = providers.gradleProperty("modId").get()
val modVersionValue = providers.gradleProperty("modVersion").get()
val baseJarName = "$modIdValue-$modVersionValue.jar"
val baseJarFile = layout.buildDirectory.file("libs/$baseJarName")

/** 带补丁版：底包本身已经包含全部补丁，只需改名。 */
val patchedJar = tasks.register<Jar>("patchedJar") {
    group = "build"
    description = "带内置补丁的构建产物（基于已反混淆的底包）"
    dependsOn(tasks.named("reobfJar"))
    from(zipTree(baseJarFile))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveFileName.set("$modIdValue-$modVersionValue-patched.jar")
    // 保留底包的 manifest（FMLCorePluginContainsFMLMod / TweakClass / MixinConfigs / ForceLoadAsMod）。
    // 直接写死四项属性，比从 zipTree 里抽取更稳定。
    manifest {
        attributes(
            "FMLCorePluginContainsFMLMod" to "true",
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "MixinConfigs" to "mixins.crosshairselection.json",
            "ForceLoadAsMod" to "true"
        )
    }
}

/** 干净版：从底包剔除补丁类，并重写 mixin 配置。 */
val cleanJar = tasks.register<Jar>("cleanJar") {
    group = "build"
    description = "不含可选补丁的构建产物（基于已反混淆的底包）"
    dependsOn(tasks.named("reobfJar"))
    from(zipTree(baseJarFile)) {
        exclude { entry ->
            val path = entry.relativePath.toString()
            patchClassPrefixes.any { path.startsWith(it) }
        }
        filesMatching("mixins.crosshairselection.json") {
            filter { line ->
                val trimmed = line.trim()
                if (patchMixins.any { trimmed.contains("\"$it\"") }) "" else line
            }
        }
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveFileName.set("$modIdValue-$modVersionValue-clean.jar")
    manifest {
        attributes(
            "FMLCorePluginContainsFMLMod" to "true",
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "MixinConfigs" to "mixins.crosshairselection.json",
            "ForceLoadAsMod" to "true"
        )
    }
}

tasks.build {
    // 底包由 reobfJar 生成，保证它先完成。
    dependsOn(tasks.named("reobfJar"), patchedJar, cleanJar)
}

package me.kuionc.crosshairselection.compat;

/**
 * 内置可选补丁的注册入口。
 *
 * <p>由 {@code ClientProxy} 在运行时反射加载：类不存在（干净版产物）时静默跳过，
 * 因此同一份源码能产出「干净版」与「带补丁版」两个产物。
 *
 * <p>目前只保留 BetterQuesting 返回键补丁，它由 mixin 与按键绑定类组成，
 * 在构建时通过排除相应类与 mixin 条目来裁剪。侧键修复不再内置：
 * 这类功能更适合使用独立的修复模组，内置会扩大与本项目无关的故障面。
 */
public final class BuiltinPatches {

    private BuiltinPatches() {
    }

    /** 注册全部内置补丁；当前没有需要运行期注册的补丁。 */
    public static void register() {
        // 保留入口：构建脚本与 ClientProxy 都依赖它存在。
    }
}

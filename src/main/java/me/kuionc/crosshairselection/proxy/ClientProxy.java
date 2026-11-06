package me.kuionc.crosshairselection.proxy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import me.kuionc.crosshairselection.command.ConfigScreenOpener;
import me.kuionc.crosshairselection.render.BreakProgressHud;
import me.kuionc.crosshairselection.render.CrosshairFeedback;
import me.kuionc.crosshairselection.render.CrosshairHud;
import me.kuionc.crosshairselection.render.CrosshairRenderer;
import net.minecraft.command.ICommand;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.Method;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        // 在 init 阶段创建：代理构造时 Minecraft 的单例还没就绪。
        CrosshairRenderer crosshairRenderer = new CrosshairRenderer();
        FMLCommonHandler.instance().bus().register(crosshairRenderer);
        FMLCommonHandler.instance().bus().register(new ConfigScreenOpener());
        CrosshairFeedback feedback = new CrosshairFeedback();
        FMLCommonHandler.instance().bus().register(feedback);
        FMLCommonHandler.instance().bus().register(new BreakProgressHud());
        FMLCommonHandler.instance().bus().register(new CrosshairHud());
        MinecraftForge.EVENT_BUS.register(feedback);
        registerQuestBackKey();
        registerBuiltinPatches();
        registerCommand();
    }

    /**
     * 注册构建时可选包含的补丁。
     *
     * <p>干净版产物里 {@code BuiltinPatches} 不存在，因此必须用反射探测：
     * 直接引用会让干净版在类加载阶段就抛 NoClassDefFoundError。
     */
    private static void registerBuiltinPatches() {
        try {
            Class<?> patches = Class.forName("me.kuionc.crosshairselection.compat.BuiltinPatches");
            patches.getMethod("register").invoke(null);
        } catch (Throwable ignored) {
            // 干净版：没有补丁可注册。
        }
    }

    /** BetterQuesting 返回键补丁同样只在带补丁的产物里存在。 */
    private static void registerQuestBackKey() {
        try {
            Class<?> keybinds = Class.forName("me.kuionc.crosshairselection.input.BetterQuestingKeybinds");
            java.lang.reflect.Field back = keybinds.getField("BACK");
            Object binding = back.get(null);
            registerKeyBinding(binding);
            Class<?> handler = Class.forName("me.kuionc.crosshairselection.input.BetterQuestingBackHandler");
            handler.getMethod("register").invoke(null);
        } catch (Throwable ignored) {
            // 干净版：不注册该按键。
        }
    }

    /**
     * 注册按键绑定。
     *
     * <p>直接调用 {@code ClientRegistry.registerKeyBinding} 依赖编译期签名，
     * 在运行时映射不同的环境下会抛 NoSuchMethodError，因此改为按参数类型反射查找。
     */
    /**
     * 注册按键绑定。
     *
     * <p>{@code ClientRegistry} 是静态工具类，没有 instance()：之前用反射找实例再找方法
     * 会直接抛异常并被吞掉，导致按键根本没有注册（表现为主界面键位里找不到这一项）。
     * 这里直接调用 Forge 的稳定 API。
     */
    private static void registerKeyBinding(Object binding) {
        if (binding instanceof net.minecraft.client.settings.KeyBinding) {
            cpw.mods.fml.client.registry.ClientRegistry
                    .registerKeyBinding((net.minecraft.client.settings.KeyBinding) binding);
        }
    }

    /**
     * 注册 /crosshairselection 命令。
     *
     * <p>同样是签名兼容问题：{@code ClientCommandHandler.registerCommand} 在不同版本里返回类型不同，
     * 编译期绑定会在运行时抛 NoSuchMethodError。这里按参数类型找到接受 {@link ICommand} 的方法。
     */
    private static void registerCommand() {
        // 直接用 Forge 的公开 API：ClientCommandHandler.instance.registerCommand(ICommand)。
        // 之前用反射按名字找方法，反混淆后名字对不上，静默失败。
        try {
            net.minecraftforge.client.ClientCommandHandler.instance
                    .registerCommand(new me.kuionc.crosshairselection.command.CommandOpenConfig());
        } catch (Throwable t) {
            System.err.println("[Crosshair-Selection] /csc command registration failed");
            t.printStackTrace();
        }
    }
}

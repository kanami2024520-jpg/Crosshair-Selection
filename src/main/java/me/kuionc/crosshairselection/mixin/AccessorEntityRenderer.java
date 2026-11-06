package me.kuionc.crosshairselection.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * EntityRenderer 的客户端实例访问器。
 *
 * <p>运行时的 {@code Minecraft.getMinecraft()} 描述符与编译期不一致，直接调用会抛
 * {@code NoSuchMethodError}。{@code @Accessor} 由注解处理器生成 SRG 字段访问字节码
 * （refmap 里可见 {@code mc -> field_78531_r}），不经过方法解析，是唯一稳定的取值方式。
 *
 * <p>注意：本接口必须写进 mixins.crosshairselection.json，否则不会生成 refmap 映射，
 * 运行时取到 null。
 */
@Mixin(EntityRenderer.class)
public interface AccessorEntityRenderer {

    /** 原版 EntityRenderer.mc 字段，即当前客户端实例。 */
    @Accessor("mc")
    Minecraft crosshairselection$mc();
}

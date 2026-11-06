package me.kuionc.crosshairselection.mixin;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * KeyBinding 字段访问器。
 *
 * <p>在这套运行时里，{@code getKeyCode()} 等方法的描述符与编译期不一致，直接调用会抛
 * {@code NoSuchMethodError}。{@code @Accessor} 生成的是字段读写字节码，mixin 在目标类上
 * 直接操作，不经过任何方法解析，因此是唯一稳定的访问方式。
 */
@Mixin(KeyBinding.class)
public interface AccessorKeyBinding {

    /** keyCode 字段。 */
    @Accessor("keyCode")
    int crosshairselection$getKeyCode();

    /** pressed 字段：isPressed() 的等价物，但不需要调用方法。 */
    @Accessor("pressed")
    boolean crosshairselection$isPressed();

    /** pressed 字段写入。 */
    @Accessor("pressed")
    void crosshairselection$setPressed(boolean pressed);
}

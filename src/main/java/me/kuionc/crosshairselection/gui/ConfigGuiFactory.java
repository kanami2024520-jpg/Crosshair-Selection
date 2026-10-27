package me.kuionc.crosshairselection.gui;

import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;

/**
 * Provides the settings screen in the FML mod list.
 */
@SideOnly(Side.CLIENT)
public class ConfigGuiFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigScreen.class;
    }
}

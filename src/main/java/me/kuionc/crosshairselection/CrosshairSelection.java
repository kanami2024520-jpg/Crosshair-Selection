package me.kuionc.crosshairselection;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import me.kuionc.crosshairselection.proxy.CommonProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Crosshair-Selection: a clean, GTNH-focused rewrite that combines a custom block selection box
 * with a custom crosshair.
 *
 * <p>Only the feature ideas were borrowed from the projects listed in the README. All code in this
 * project is written from scratch.</p>
 */
@Mod(modid = CrosshairSelection.MOD_ID,
     name = CrosshairSelection.MOD_NAME,
     version = CrosshairSelection.VERSION,
     acceptedMinecraftVersions = "[1.7.10]",
     acceptableRemoteVersions = "*",
     guiFactory = "me.kuionc.crosshairselection.gui.ConfigGuiFactory",
     dependencies = "required-after:unimixins;required-after:gtnhlib;required-after:gregtech;required-after:EnderIO;required-after:ForgeMultipart")
public class CrosshairSelection {

    public static final String MOD_ID = "crosshairselection";
    public static final String MOD_NAME = "Crosshair-Selection";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @SidedProxy(clientSide = "me.kuionc.crosshairselection.proxy.ClientProxy",
                serverSide = "me.kuionc.crosshairselection.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}

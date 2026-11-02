package me.kuionc.crosshairselection.proxy;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.config.GtOverlayConfig;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.render.CrosshairType;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        try {
            ConfigurationManager.registerConfig(SelectionBoxConfig.class);
            ConfigurationManager.registerConfig(CrosshairConfig.class);
            ConfigurationManager.registerConfig(GtOverlayConfig.class);
        } catch (ConfigException exception) {
            throw new IllegalStateException("Could not register Crosshair-Selection config", exception);
        }

        CrosshairType type = CrosshairType.sanitize(CrosshairConfig.type);
        if (type != CrosshairConfig.type) {
            CrosshairConfig.type = type;
            ConfigurationManager.save(CrosshairConfig.class);
        }
    }

    public void init(FMLInitializationEvent event) {
    }
}

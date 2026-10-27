package me.kuionc.crosshairselection.proxy;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import me.kuionc.crosshairselection.CrosshairSelection;
import me.kuionc.crosshairselection.config.ConfigMigration;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;

import java.io.File;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        File configDir = event.getModConfigurationDirectory();
        File newConfig = new File(new File(configDir, CrosshairSelection.MOD_ID), "config.cfg");
        File oldConfig = new File(new File(configDir, "CSB"), "config.json");
        boolean migrate = !newConfig.exists() && oldConfig.exists();

        try {
            ConfigurationManager.registerConfig(SelectionBoxConfig.class);
            ConfigurationManager.registerConfig(CrosshairConfig.class);
        } catch (ConfigException exception) {
            throw new IllegalStateException("Could not register Crosshair-Selection config", exception);
        }

        if (migrate) {
            if (ConfigMigration.migrateFromLegacyJson(oldConfig)) {
                ConfigurationManager.save(SelectionBoxConfig.class, CrosshairConfig.class);
                CrosshairSelection.LOGGER.info("Migrated legacy CSB config from " + oldConfig);
            }
        }
    }

    public void init(FMLInitializationEvent event) {
    }
}

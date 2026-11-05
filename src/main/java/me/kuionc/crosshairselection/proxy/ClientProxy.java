package me.kuionc.crosshairselection.proxy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import me.kuionc.crosshairselection.command.CommandOpenConfig;
import me.kuionc.crosshairselection.command.ConfigScreenOpener;
import me.kuionc.crosshairselection.input.BetterQuestingBackHandler;
import me.kuionc.crosshairselection.input.BetterQuestingKeybinds;
import me.kuionc.crosshairselection.render.CrosshairRenderer;
import net.minecraftforge.client.ClientCommandHandler;
import cpw.mods.fml.client.registry.ClientRegistry;

public class ClientProxy extends CommonProxy {

    private final CrosshairRenderer crosshairRenderer = new CrosshairRenderer();

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        FMLCommonHandler.instance().bus().register(crosshairRenderer);
        FMLCommonHandler.instance().bus().register(new ConfigScreenOpener());
        ClientRegistry.registerKeyBinding(BetterQuestingKeybinds.BACK);
        BetterQuestingBackHandler.register();
        ClientCommandHandler.instance.registerCommand(new CommandOpenConfig());
    }
}

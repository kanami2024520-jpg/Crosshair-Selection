package me.kuionc.crosshairselection.proxy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import me.kuionc.crosshairselection.render.CrosshairRenderer;

public class ClientProxy extends CommonProxy {

    private final CrosshairRenderer crosshairRenderer = new CrosshairRenderer();

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        FMLCommonHandler.instance().bus().register(crosshairRenderer);
    }
}

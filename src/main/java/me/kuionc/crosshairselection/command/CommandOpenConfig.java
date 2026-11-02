package me.kuionc.crosshairselection.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.List;

/**
 * Client-side command that opens the settings screen. It never reaches the server, so it also
 * works on servers without the mod and with cheats disabled.
 */
public class CommandOpenConfig extends CommandBase {

    @Override
    public String getCommandName() {
        return "crosshairselection";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/crosshairselection";
    }

    @Override
    public List<String> getCommandAliases() {
        return java.util.Collections.singletonList("csb");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        ConfigScreenOpener.request();
    }
}

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
        return "csc";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/csc";
    }

    @Override
    public List<String> getCommandAliases() {
        // 只保留 csc 一个名字，避免多别名在客户端命令处理里表现不一致。
        return java.util.Collections.emptyList();
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

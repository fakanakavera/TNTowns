package com.tntowns.commands;

import com.tntowns.model.ChatChannel;
import com.tntowns.service.TownService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class ChatToggleCommand implements CommandExecutor, TabCompleter {

    private final TownService townService;

    public ChatToggleCommand(TownService townService) {
        this.townService = townService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
        Player p = (Player) sender;
        if (args.length == 0) {
            p.sendMessage("§eUsage: /" + label + " <global|local|town|nation>");
            return true;
        }
        ChatChannel ch;
        switch (args[0].toLowerCase()) {
            case "global": ch = ChatChannel.GLOBAL; break;
            case "local": ch = ChatChannel.LOCAL; break;
            case "town": ch = ChatChannel.TOWN; break;
            case "nation": ch = ChatChannel.NATION; break;
            default: p.sendMessage("§cUnknown channel."); return true;
        }
        townService.getOrCreateResident(p).setChatChannel(ch);
        p.sendMessage("§aChat channel set to §e" + ch.name());
        return true;
    }
    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        java.util.List<String> subs = java.util.Arrays.asList("global","local","town","nation");
        if (args.length == 1) {
            String pref = args[0].toLowerCase();
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String s : subs) if (s.startsWith(pref)) out.add(s);
            return out;
        }
        return java.util.Collections.emptyList();
    }
}



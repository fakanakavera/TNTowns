package com.tntowns.commands;

import com.tntowns.model.Town;
import com.tntowns.service.TownService;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class TownAdminCommand implements CommandExecutor, TabCompleter {

    private final TownService townService;

    public TownAdminCommand(TownService townService) {
        this.townService = townService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
        Player p = (Player) sender;
        if (!p.hasPermission("tntowns.admin")) { p.sendMessage("§cNo permission."); return true; }
        if (args.length == 0) {
            p.sendMessage("§eUsage: /" + label + " <pvp on|off>");
            return true;
        }
        if (args[0].equalsIgnoreCase("pvp")) {
            if (args.length < 2) { p.sendMessage("§eUsage: /" + label + " pvp <on|off>"); return true; }
            Chunk c = p.getLocation().getChunk();
            Optional<Town> owner = townService.findTownByChunk(c);
            if (owner.isEmpty()) { p.sendMessage("§cThis chunk is wilderness."); return true; }
            Town t = owner.get();
            boolean enable = args[1].equalsIgnoreCase("on");
            t.setPvpEnabled(enable);
            p.sendMessage("§aSet town PVP to §e" + enable);
            return true;
        }
        p.sendMessage("§cUnknown subcommand.");
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (args.length == 1) return java.util.Collections.singletonList("pvp");
        if (args.length == 2 && args[0].equalsIgnoreCase("pvp")) return java.util.Arrays.asList("on","off");
        return java.util.Collections.emptyList();
    }
}



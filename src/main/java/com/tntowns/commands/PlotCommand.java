package com.tntowns.commands;

import com.tntowns.TNtownsPlugin;
import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.PlotService;
import com.tntowns.service.TownService;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PlotCommand implements CommandExecutor, TabCompleter {

    private final TNtownsPlugin plugin;
    private final TownService townService;
    private final PlotService plotService;

    public PlotCommand(TNtownsPlugin plugin, TownService townService, PlotService plotService) {
        this.plugin = plugin;
        this.townService = townService;
        this.plotService = plotService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
        Player p = (Player) sender;
        if (args.length == 0) {
            p.sendMessage("§eUsage: /" + label + " <forsale <price>|buy|setowner <player>>");
            return true;
        }
        Resident res = townService.getOrCreateResident(p);
        Optional<Town> optTown = townService.getResidentTown(res);
        if (optTown.isEmpty()) { p.sendMessage("§cYou are not in a town."); return true; }
        Town town = optTown.get();
        Chunk chunk = p.getLocation().getChunk();
        Optional<Town> ownerTown = townService.findTownByChunk(chunk);
        if (ownerTown.isEmpty() || !ownerTown.get().getId().equalsIgnoreCase(town.getId())) { p.sendMessage("§cYour town must own this chunk."); return true; }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "forsale":
                if (args.length < 2) { p.sendMessage("§eUsage: /" + label + " forsale <price>"); return true; }
                try {
                    double price = Double.parseDouble(args[1]);
                    com.tntowns.model.Plot plot = plotService.getOrCreate(chunk);
                    plot.setForSale(true);
                    plot.setPrice(price);
                    p.sendMessage("§aPlot listed for $" + price);
                } catch (NumberFormatException ex) {
                    p.sendMessage("§cInvalid price.");
                }
                return true;
            case "buy":
                com.tntowns.model.Plot plot = plotService.getOrCreate(chunk);
                if (!plot.isForSale()) { p.sendMessage("§cPlot not for sale."); return true; }
                double price = plot.getPrice();
                if (plugin.getEconomy().getBalance(p) < price) { p.sendMessage("§cInsufficient funds."); return true; }
                plugin.getEconomy().withdrawPlayer(p, price);
                plot.setOwnerUuid(p.getUniqueId().toString());
                plot.setForSale(false);
                p.sendMessage("§aPurchased plot for $" + price);
                return true;
            case "setowner":
                if (args.length < 2) { p.sendMessage("§eUsage: /" + label + " setowner <player>"); return true; }
                if (!townService.isMayor(res, town) && !p.hasPermission("tntowns.plot.admin")) { p.sendMessage("§cOnly mayor can set plot owner."); return true; }
                Player target = p.getServer().getPlayerExact(args[1]);
                if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
                com.tntowns.model.Plot plot2 = plotService.getOrCreate(chunk);
                plot2.setOwnerUuid(target.getUniqueId().toString());
                plot2.setForSale(false);
                plot2.setPrice(0);
                p.sendMessage("§aSet plot owner to §e" + target.getName());
                return true;
            default:
                p.sendMessage("§cUnknown subcommand.");
                return true;
        }
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        java.util.List<String> subs = java.util.Arrays.asList("forsale","buy","setowner");
        if (args.length == 1) {
            String pref = args[0].toLowerCase();
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String s : subs) if (s.startsWith(pref)) out.add(s);
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setowner")) {
            java.util.List<String> names = new java.util.ArrayList<>();
            for (org.bukkit.entity.Player p : ((org.bukkit.entity.Player) sender).getServer().getOnlinePlayers()) names.add(p.getName());
            String pref = args[1].toLowerCase();
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String n : names) if (n.toLowerCase().startsWith(pref)) out.add(n);
            return out;
        }
        return java.util.Collections.emptyList();
    }
}

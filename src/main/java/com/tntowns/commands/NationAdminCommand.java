package com.tntowns.commands;

import com.tntowns.model.Nation;
import com.tntowns.model.NationRank;
import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.NationService;
import com.tntowns.service.TownService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class NationAdminCommand implements CommandExecutor, TabCompleter {

    private final TownService townService;
    private final NationService nationService;

    public NationAdminCommand(TownService townService, NationService nationService) {
        this.townService = townService;
        this.nationService = nationService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
        Player p = (Player) sender;
        Resident res = townService.getOrCreateResident(p);
        Optional<Town> optTown = townService.getResidentTown(res);
        if (optTown.isEmpty() || optTown.get().getNationId() == null) { p.sendMessage("§cYou are not in a nation."); return true; }
        Nation nation = nationService.getNationById(optTown.get().getNationId()).orElse(null);
        if (nation == null) { p.sendMessage("§cNation not found."); return true; }

        if (args.length == 0) {
            p.sendMessage("§eUsage: /" + label + " <invite|accept|kick|rank|bank list>");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "invite": {
                if (args.length < 2) { p.sendMessage("§eUsage: /" + label + " invite <player>"); return true; }
                Player target = p.getServer().getPlayerExact(args[1]);
                if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
                com.tntowns.TNtownsPlugin.getInstance().getInviteManager().invite(target.getUniqueId(), nation.getId());
                p.sendMessage("§aInvited §e" + target.getName() + " §ato join the nation.");
                target.sendMessage("§eYou were invited to a nation. Use §a/nationadmin accept§e to join.");
                return true;
            }
            case "accept": {
                String invite = com.tntowns.TNtownsPlugin.getInstance().getInviteManager().getInvite(p.getUniqueId());
                if (invite == null || !invite.equalsIgnoreCase(nation.getId())) { p.sendMessage("§cYou have no pending nation invite."); return true; }
                nation.getMemberRanks().put(p.getUniqueId().toString(), NationRank.E1.name());
                com.tntowns.TNtownsPlugin.getInstance().getInviteManager().clearInvite(p.getUniqueId());
                p.sendMessage("§aJoined nation.");
                return true;
            }
            case "kick": {
                if (args.length < 2) { p.sendMessage("§eUsage: /" + label + " kick <player>"); return true; }
                Player target = p.getServer().getPlayerExact(args[1]);
                if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
                nation.getMemberRanks().remove(target.getUniqueId().toString());
                p.sendMessage("§aKicked §e" + target.getName() + " §afrom the nation.");
                return true;
            }
            case "rank": {
                if (args.length < 3) { p.sendMessage("§eUsage: /" + label + " rank <player> <E1-E9>"); return true; }
                Player target = p.getServer().getPlayerExact(args[1]);
                if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
                NationRank rank = NationRank.fromString(args[2]);
                nation.getMemberRanks().put(target.getUniqueId().toString(), rank.name());
                p.sendMessage("§aSet nation rank of §e" + target.getName() + " §ato §e" + rank.name());
                return true;
            }
            case "bank": {
                if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                    p.sendMessage("§6Nation bank balances:");
                    for (java.util.Map.Entry<String, Double> e : nation.getMemberDeposits().entrySet()) {
                        java.util.UUID id = java.util.UUID.fromString(e.getKey());
                        String name = p.getServer().getOfflinePlayer(id).getName();
                        p.sendMessage("§e" + (name != null ? name : id.toString()) + "§7: §a$" + String.format(java.util.Locale.US, "%.2f", e.getValue()));
                    }
                    return true;
                }
                p.sendMessage("§eUsage: /" + label + " bank list");
                return true;
            }
            default:
                p.sendMessage("§cUnknown subcommand.");
                return true;
        }
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return java.util.Collections.emptyList();
        java.util.List<String> subs = java.util.Arrays.asList("invite","accept","kick","rank","bank");
        if (args.length == 1) return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("bank")) return java.util.Collections.singletonList("list");
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("rank"))) {
            String pref = args[1].toLowerCase();
            return ((Player) sender).getServer().getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(pref)).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("rank")) {
            java.util.List<String> ranks = java.util.Arrays.asList("E1","E2","E3","E4","E5","E6","E7","E8","E9");
            String pref = args[2].toUpperCase();
            return ranks.stream().filter(r -> r.startsWith(pref)).collect(Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }
}



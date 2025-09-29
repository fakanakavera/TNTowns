package com.tntowns.commands;

import com.tntowns.TNtownsPlugin;
import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.TownService;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import org.bukkit.OfflinePlayer;

public class TNTownCommand implements CommandExecutor, TabCompleter {

	private final TNtownsPlugin plugin;
	private final TownService townService;

	public TNTownCommand(TNtownsPlugin plugin, TownService townService) {
		this.plugin = plugin;
		this.townService = townService;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}
		Player player = (Player) sender;
		if (!player.hasPermission("tntowns.use")) {
			player.sendMessage("§cYou do not have permission.");
			return true;
		}
		if (args.length == 0) {
			handleInfo(player);
			return true;
		}
		String sub = args[0].toLowerCase(Locale.ROOT);
		switch (sub) {
			case "help":
				sendHelp(player, label);
				return true;
			case "compass":
				handleCompass(player);
				return true;
			case "create":
				if (args.length < 2) {
					player.sendMessage("§eUsage: /" + label + " create <name>");
					return true;
				}
				String name = joinArgs(args, 1);
				if (townService.isTownNameTaken(name)) {
					player.sendMessage("§cA town with that name already exists.");
					return true;
				}
				handleCreate(player, name);
				return true;
			case "claim":
				handleClaim(player);
				return true;
			case "unclaim":
				handleUnclaim(player);
				return true;
			case "bank":
				handleBank(player, args);
				return true;
			case "invite":
				handleInvite(player, args);
				return true;
			case "accept":
				handleAccept(player);
				return true;
			case "leave":
				handleLeave(player);
				return true;
			case "kick":
				handleKick(player, args);
				return true;
			case "settax":
				handleSetTax(player, args);
				return true;
			case "setmayor":
				handleSetMayor(player, args);
				return true;
			case "setjail":
				handleSetJail(player);
				return true;
			case "jail":
				handleJail(player, args);
				return true;
			case "outlaw":
				handleOutlaw(player, args);
				return true;
			default:
				sendHelp(player, label);
				return true;
		}
	}

	private void sendHelp(Player player, String label) {
		player.sendMessage("§6TNtowns:");
		player.sendMessage("§e/" + label + " compass §7- Point compass to your town");
		player.sendMessage("§e/" + label + " create <name> §7- Create a town");
		player.sendMessage("§e/" + label + " claim §7- Claim current chunk");
		player.sendMessage("§e/" + label + " unclaim §7- Unclaim current chunk");
		player.sendMessage("§e/" + label + " bank §7- Show town bank info");
		player.sendMessage("§e/" + label + " bank deposit <amount> §7- Deposit to town bank");
		player.sendMessage("§e/" + label + " bank withdraw <amount> §7- Withdraw from town bank (mayor)");
		player.sendMessage("§e/" + label + " invite <player> §7- Invite a player to town");
		player.sendMessage("§e/" + label + " accept §7- Accept a town invite");
		player.sendMessage("§e/" + label + " leave §7- Leave your town");
		player.sendMessage("§e/" + label + " kick <player> §7- Kick from town (mayor)");
		player.sendMessage("§e/" + label + " settax <amount> §7- Set per-resident tax (mayor)");
		player.sendMessage("§e/" + label + " setmayor <player> §7- Transfer mayorship");
		player.sendMessage("§e/" + label + " setjail §7- Set jail to your location (mayor)");
		player.sendMessage("§e/" + label + " jail <player> §7- Jail/Unjail a player");
		player.sendMessage("§e/" + label + " outlaw <player> §7- Toggle outlaw status");
	}

	private void handleCompass(Player player) {
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) { player.sendMessage("§cYou are not in a town."); return; }
		Town town = optTown.get();
		java.util.Optional<org.bukkit.Location> loc = townService.getTownCompassLocation(town, player.getServer());
		if (loc.isEmpty()) { player.sendMessage("§cYour town has no claims or set jail to point to."); return; }
		player.setCompassTarget(loc.get());
		player.sendMessage("§aCompass now points to §e" + town.getName() + "§a.");
	}

	private void handleCreate(Player player, String name) {
		Resident res = townService.getOrCreateResident(player);
		if (res.getTownId() != null) {
			player.sendMessage("§cYou are already in a town.");
			return;
		}
		double cost = plugin.getConfig().getDouble("costs.create_town", 500.0);
		if (plugin.getEconomy().getBalance(player) < cost) {
			player.sendMessage("§cYou need $" + cost + " to create a town.");
			return;
		}
		plugin.getEconomy().withdrawPlayer(player, cost);
		Town town = townService.createTown(player, name);
		player.sendMessage("§aCreated town §e" + town.getName() + " §7(id: " + town.getId() + ")");
	}

	private void handleInvite(Player player, String[] args) {
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) { player.sendMessage("§cYou are not in a town."); return; }
		Town town = optTown.get();
		if (!townService.isMayor(res, town) && !player.hasPermission("tntowns.invite")) { player.sendMessage("§cOnly mayor can invite."); return; }
		if (args.length < 2) { player.sendMessage("§eUsage: /tntown invite <player>"); return; }
		Player target = player.getServer().getPlayerExact(args[1]);
		if (target == null) { player.sendMessage("§cPlayer not found."); return; }
		plugin.getInviteManager().invite(target.getUniqueId(), town.getId());
		player.sendMessage("§aInvited §e" + target.getName());
		target.sendMessage("§eYou were invited to town §6" + town.getName() + "§e. Use §a/tntown accept§e to join.");
	}

	private void handleAccept(Player player) {
		String townId = plugin.getInviteManager().getInvite(player.getUniqueId());
		if (townId == null) { player.sendMessage("§cYou have no pending town invite."); return; }
		Optional<Town> optTown = townService.getTownById(townId);
		if (optTown.isEmpty()) { player.sendMessage("§cThat town no longer exists."); return; }
		Resident res = townService.getOrCreateResident(player);
		if (res.getTownId() != null) { player.sendMessage("§cLeave your current town first."); return; }
		Town town = optTown.get();
		res.setTownId(town.getId());
		town.getResidentUuids().add(player.getUniqueId().toString());
		plugin.getInviteManager().clearInvite(player.getUniqueId());
		player.sendMessage("§aJoined town §e" + town.getName());
	}

	private void handleLeave(Player player) {
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) { player.sendMessage("§cYou are not in a town."); return; }
		Town town = optTown.get();
		boolean isMayor = townService.isMayor(res, town);
		if (town.getResidentUuids().size() <= 1) {
			// Player is alone in the town -> disband
			townService.disbandTown(town);
			player.sendMessage("§aYou left and disbanded the town since you were the only member.");
			return;
		}
		if (isMayor) { player.sendMessage("§cTransfer mayorship before leaving."); return; }
		town.getResidentUuids().remove(player.getUniqueId().toString());
		res.setTownId(null);
		res.setRole(com.tntowns.model.ResidentRole.MEMBER);
		player.sendMessage("§aYou left the town.");
	}

	private void handleKick(Player player, String[] args) {
		if (args.length < 2) { player.sendMessage("§eUsage: /tntown kick <player>"); return; }
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) { player.sendMessage("§cYou are not in a town."); return; }
		Town town = optTown.get();
		if (!townService.isMayor(res, town)) { player.sendMessage("§cOnly the mayor can kick members."); return; }
		Player target = player.getServer().getPlayerExact(args[1]);
		if (target == null) { player.sendMessage("§cPlayer not found."); return; }
		if (!town.getResidentUuids().contains(target.getUniqueId().toString())) { player.sendMessage("§cThey are not in your town."); return; }
		town.getResidentUuids().remove(target.getUniqueId().toString());
		townService.getResident(target.getUniqueId()).ifPresent(r -> { r.setTownId(null); r.setRole(com.tntowns.model.ResidentRole.MEMBER); });
		player.sendMessage("§aKicked §e" + target.getName());
		target.sendMessage("§cYou were kicked from the town.");
	}

	private void handleSetTax(Player player, String[] args) {
		if (args.length < 2) { player.sendMessage("§eUsage: /tntown settax <amount>"); return; }
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) { player.sendMessage("§cYou are not in a town."); return; }
		Town town = optTown.get();
		if (!townService.isMayor(res, town)) { player.sendMessage("§cOnly the mayor can set tax."); return; }
		try {
			double amount = Double.parseDouble(args[1]);
			if (amount < 0) { player.sendMessage("§cAmount must be non-negative."); return; }
			town.setTaxPerResident(amount);
			player.sendMessage("§aSet per-resident daily tax to $" + amount);
		} catch (NumberFormatException ex) {
			player.sendMessage("§cInvalid amount.");
		}
	}

	private void handleSetMayor(Player player, String[] args) {
		if (args.length < 2) { player.sendMessage("§eUsage: /tntown setmayor <player>"); return; }
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) { player.sendMessage("§cYou are not in a town."); return; }
		Town town = optTown.get();
		if (!townService.isMayor(res, town)) { player.sendMessage("§cOnly the current mayor can transfer mayorship."); return; }
		Player target = player.getServer().getPlayerExact(args[1]);
		if (target == null) { player.sendMessage("§cPlayer not found."); return; }
		if (!town.getResidentUuids().contains(target.getUniqueId().toString())) { player.sendMessage("§cThey are not in your town."); return; }
		town.setMayorUuid(target.getUniqueId().toString());
		townService.getResident(target.getUniqueId()).ifPresent(r -> r.setRole(com.tntowns.model.ResidentRole.MAYOR));
		player.sendMessage("§aTransferred mayorship to §e" + target.getName());
		target.sendMessage("§aYou are now the mayor.");
	}

	private void handleSetJail(Player player) {
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) { player.sendMessage("§cYou are not in a town."); return; }
		Town town = optTown.get();
		if (!townService.isMayor(res, town)) { player.sendMessage("§cOnly the mayor can set jail."); return; }
		player.sendMessage("§aSet jail to your current location.");
		townService.setJail(town, player.getWorld().getName(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
	}

	private void handleJail(Player player, String[] args) {
		if (args.length < 2) { player.sendMessage("§eUsage: /tntown jail <player>"); return; }
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) { player.sendMessage("§cYou are not in a town."); return; }
		Town town = optTown.get();
		if (!townService.isMayor(res, town)) { player.sendMessage("§cOnly the mayor can jail."); return; }
		Player target = player.getServer().getPlayerExact(args[1]);
		if (target == null) { player.sendMessage("§cPlayer not found."); return; }
		Resident tres = townService.getOrCreateResident(target);
		if (tres.isJailed()) {
			tres.setJailed(false);
			player.sendMessage("§aUnjailed §e" + target.getName());
			target.sendMessage("§aYou have been unjailed.");
		} else {
			tres.setJailed(true);
			target.sendMessage("§cYou have been jailed.");
			player.sendMessage("§aJailed §e" + target.getName());
		}
	}

	private void handleOutlaw(Player player, String[] args) {
		if (args.length < 2) { player.sendMessage("§eUsage: /tntown outlaw <player>"); return; }
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) { player.sendMessage("§cYou are not in a town."); return; }
		Town town = optTown.get();
		if (!townService.isMayor(res, town)) { player.sendMessage("§cOnly the mayor can toggle outlaw."); return; }
		Player target = player.getServer().getPlayerExact(args[1]);
		if (target == null) { player.sendMessage("§cPlayer not found."); return; }
		boolean nowOutlaw = !town.getOutlawUuids().contains(target.getUniqueId().toString());
		townService.setOutlaw(town, target.getUniqueId(), nowOutlaw);
		player.sendMessage((nowOutlaw ? "§cMarked §e" : "§aUnmarked §e") + target.getName() + (nowOutlaw ? " §cas outlaw." : " §aas outlaw."));
	}

	private void handleClaim(Player player) {
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) {
			player.sendMessage("§cYou are not in a town.");
			return;
		}
		Town town = optTown.get();
		if (!townService.isMayor(res, town) && !player.hasPermission("tntowns.claim")) {
			player.sendMessage("§cOnly the mayor can claim unless you have permission.");
			return;
		}
		Chunk chunk = player.getLocation().getChunk();
        // Nation territory cap enforcement
        if (town.getNationId() != null) {
            com.tntowns.service.NationService ns = plugin.getNationService();
            com.tntowns.model.Nation nation = ns.getNationById(town.getNationId()).orElse(null);
            if (nation != null) {
                boolean adjacent = ns.isAdjacentToNation(nation, chunk);
                if (!adjacent) {
                    int territories = ns.countNationTerritories(nation);
                    int allowed = ns.getAllowedTerritories(plugin.getConfig(), nation);
                    if (territories >= allowed) {
                        player.sendMessage("§cThis claim would start a new territory, but your nation has reached its limit (" + territories + "/" + allowed + "). Use §e/nation expand§c.");
                        return;
                    }
                }
            }
        }
		if (townService.isChunkClaimed(chunk)) {
			player.sendMessage("§cThis chunk is already claimed.");
			return;
		}
		double cost = plugin.getConfig().getDouble("costs.claim_chunk", 50.0);
		if (plugin.getEconomy().getBalance(player) < cost) {
			player.sendMessage("§cYou need $" + cost + " to claim this chunk.");
			return;
		}
		plugin.getEconomy().withdrawPlayer(player, cost);
		if (townService.claimChunk(player, town, chunk)) {
			player.sendMessage("§aClaimed chunk §e(" + chunk.getX() + "," + chunk.getZ() + ")");
		} else {
			player.sendMessage("§cFailed to claim chunk.");
		}
	}

	private void handleUnclaim(Player player) {
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) {
			player.sendMessage("§cYou are not in a town.");
			return;
		}
		Town town = optTown.get();
		if (!townService.isMayor(res, town) && !player.hasPermission("tntowns.unclaim")) {
			player.sendMessage("§cOnly the mayor can unclaim unless you have permission.");
			return;
		}
		Chunk chunk = player.getLocation().getChunk();
		Optional<Town> owner = townService.findTownByChunk(chunk);
		if (owner.isEmpty() || !owner.get().getId().equalsIgnoreCase(town.getId())) {
			player.sendMessage("§cYour town does not own this chunk.");
			return;
		}
		if (townService.unclaimChunk(town, chunk)) {
			double refund = plugin.getConfig().getDouble("costs.unclaim_refund", 25.0);
			plugin.getEconomy().depositPlayer(player, refund);
			player.sendMessage("§aUnclaimed chunk. Refunded $" + refund + ".");
		} else {
			player.sendMessage("§cFailed to unclaim chunk.");
		}
	}

	private void handleBank(Player player, String[] args) {
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) {
			player.sendMessage("§cYou are not in a town.");
			return;
		}
		Town town = optTown.get();
		if (args.length < 2) {
			player.sendMessage("§6Town Bank:");
			player.sendMessage("§7Balance: §a$" + town.getBankBalance());
			player.sendMessage("§7Use §e/tntown bank deposit <amount> §7or §e/tntown bank withdraw <amount>");
			return;
		}
		String action = args[1].toLowerCase(Locale.ROOT);
		if (args.length < 3) {
			player.sendMessage("§eProvide an amount.");
			return;
		}
		double amount;
		try {
			amount = Double.parseDouble(args[2]);
		} catch (NumberFormatException ex) {
			player.sendMessage("§cInvalid amount.");
			return;
		}
		if (amount <= 0) {
			player.sendMessage("§cAmount must be positive.");
			return;
		}
		switch (action) {
			case "deposit":
				if (plugin.getEconomy().getBalance(player) < amount) {
					player.sendMessage("§cInsufficient funds.");
					return;
				}
				plugin.getEconomy().withdrawPlayer(player, amount);
				townService.deposit(town, amount);
				player.sendMessage("§aDeposited $" + amount + ". Town bank: $" + town.getBankBalance());
				return;
			case "withdraw":
				if (!townService.isMayor(res, town)) {
					player.sendMessage("§cOnly the mayor can withdraw.");
					return;
				}
				if (!townService.withdraw(town, amount)) {
					player.sendMessage("§cTown bank has insufficient funds.");
					return;
				}
				plugin.getEconomy().depositPlayer(player, amount);
				player.sendMessage("§aWithdrew $" + amount + ". Town bank: $" + town.getBankBalance());
				return;
			default:
				player.sendMessage("§eUsage: /tntown bank <deposit|withdraw> <amount>");
		}
	}

	private void handleInfo(Player player) {
		Resident res = townService.getOrCreateResident(player);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (optTown.isEmpty()) {
			player.sendMessage("§cYou are not in a town.");
			player.sendMessage("§7Create one with §e/t create <name>§7 or accept an invite.");
			return;
		}
		Town town = optTown.get();
		String mayorName = "-";
		if (town.getMayorUuid() != null) {
			try {
				UUID mayorId = UUID.fromString(town.getMayorUuid());
				OfflinePlayer op = player.getServer().getOfflinePlayer(mayorId);
				if (op != null && op.getName() != null) mayorName = op.getName();
			} catch (IllegalArgumentException ignored) {}
		}
		String nationName = "-";
		if (town.getNationId() != null) {
			com.tntowns.service.NationService ns = plugin.getNationService();
			com.tntowns.model.Nation n = ns.getNationById(town.getNationId()).orElse(null);
			if (n != null && n.getName() != null) nationName = n.getName();
		}
		int residents = town.getResidentUuids() != null ? town.getResidentUuids().size() : 0;
		int claims = town.getClaimKeys() != null ? town.getClaimKeys().size() : 0;
		player.sendMessage("§6Town Info:");
		player.sendMessage("§7Name: §e" + town.getName() + " §8(#" + town.getId() + ")");
		player.sendMessage("§7Mayor: §e" + mayorName);
		player.sendMessage("§7Nation: §e" + nationName);
		player.sendMessage("§7Residents: §e" + residents + " §7| Claims: §e" + claims);
		player.sendMessage("§7Bank: §a$" + town.getBankBalance() + " §7| Tax/resident: §a$" + town.getTaxPerResident());
		player.sendMessage("§7PVP: " + (town.isPvpEnabled() ? "§cON" : "§aOFF"));
	}

	private String joinArgs(String[] arr, int start) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < arr.length; i++) {
			if (i > start) sb.append(' ');
			sb.append(arr[i]);
		}
		return sb.toString();
	}
    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
		List<String> subs = Arrays.asList("help","compass","create","claim","unclaim","bank","invite","accept","leave","kick","settax","setmayor","setjail","jail","outlaw");
        if (args.length == 1) {
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "bank":
                if (args.length == 2) return Arrays.asList("deposit","withdraw").stream().filter(v -> v.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                return Collections.emptyList();
            case "invite":
            case "kick":
            case "setmayor":
            case "jail":
            case "outlaw": {
                if (args.length == 2) {
                    List<String> names = new ArrayList<>();
                    for (Player p : ((Player) sender).getServer().getOnlinePlayers()) names.add(p.getName());
                    String prefix = args[1].toLowerCase(Locale.ROOT);
                    return names.stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
            default:
                return Collections.emptyList();
        }
    }
}



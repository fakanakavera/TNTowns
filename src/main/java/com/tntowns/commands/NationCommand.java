package com.tntowns.commands;

import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.NationService;
import com.tntowns.service.TownService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import java.util.Optional;

public class NationCommand implements CommandExecutor, TabCompleter {

	private final TownService townService;
	private final NationService nationService;

	public NationCommand(TownService townService, NationService nationService) {
		this.townService = townService;
		this.nationService = nationService;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
		Player p = (Player) sender;
		if (args.length == 0) {
			Resident res = townService.getOrCreateResident(p);
			java.util.Optional<Town> optTown = townService.getResidentTown(res);
			if (optTown.isEmpty() || optTown.get().getNationId() == null) { p.sendMessage("§cYou are not in a nation."); return true; }
			com.tntowns.model.Nation nation = nationService.getNationById(optTown.get().getNationId()).orElse(null);
			if (nation == null) { p.sendMessage("§cNation not found."); return true; }
			p.sendMessage("§6Nation Info:");
			p.sendMessage("§7Name: §e" + nation.getName() + " §8(#" + nation.getId() + ")");
			p.sendMessage("§7Towns: §e" + nation.getTownIds().size());
			p.sendMessage("§7Allies: §e" + nation.getAllyNationIds().size() + " §7| Enemies: §e" + nation.getEnemyNationIds().size());
			p.sendMessage("§7Extra territory slots: §e" + nation.getExtraTerritorySlots());
			return true;
		}
		String sub = args[0].toLowerCase();
		Resident res = townService.getOrCreateResident(p);
		Optional<Town> optTown = townService.getResidentTown(res);
		if (sub.equals("accept")) {
			String invite = com.tntowns.TNtownsPlugin.getInstance().getInviteManager().getInvite(p.getUniqueId());
			if (invite == null) { p.sendMessage("§cYou have no pending nation invite."); return true; }
			com.tntowns.model.Nation nation = nationService.getNationById(invite).orElse(null);
			if (nation == null) { p.sendMessage("§cNation not found."); return true; }
			nation.getMemberRanks().put(p.getUniqueId().toString(), com.tntowns.model.NationRank.E1.name());
			com.tntowns.TNtownsPlugin.getInstance().getInviteManager().clearInvite(p.getUniqueId());
			p.sendMessage("§aJoined nation.");
			return true;
		}
		if (optTown.isEmpty()) { p.sendMessage("§cYou are not in a town."); return true; }
		Town town = optTown.get();
		switch (sub) {
			case "create":
				if (args.length < 2) { p.sendMessage("§eUsage: /" + label + " create <name>"); return true; }
				if (!townService.isMayor(res, town)) { p.sendMessage("§cOnly the mayor can create a nation."); return true; }
				nationService.createNation(joinArgs(args, 1), town);
				p.sendMessage("§aNation created.");
				return true;
			case "addtown":
				if (args.length < 2) { p.sendMessage("§eUsage: /" + label + " addtown <townId>"); return true; }
				if (town.getNationId() == null) { p.sendMessage("§cYour town is not in a nation."); return true; }
				Optional<Town> add = townService.getTownById(args[1]);
				if (add.isEmpty()) { p.sendMessage("§cTown not found."); return true; }
				nationService.getNationById(town.getNationId()).ifPresent(n -> { nationService.addTown(n, add.get()); });
				p.sendMessage("§aTown added to nation.");
				return true;
			case "ally":
			case "enemy":
				if (args.length < 2) { p.sendMessage("§eUsage: /" + label + " " + sub + " <nationId>"); return true; }
				if (town.getNationId() == null) { p.sendMessage("§cYour town is not in a nation."); return true; }
				Optional<com.tntowns.model.Nation> my = nationService.getNationById(town.getNationId());
				Optional<com.tntowns.model.Nation> other = nationService.getNationById(args[1]);
				if (my.isEmpty() || other.isEmpty()) { p.sendMessage("§cNation not found."); return true; }
				if (sub.equals("ally")) nationService.ally(my.get(), other.get()); else nationService.enemy(my.get(), other.get());
				p.sendMessage("§aUpdated relations.");
				return true;
			case "bank": {
				if (town.getNationId() == null) { p.sendMessage("§cYour town is not in a nation."); return true; }
				com.tntowns.model.Nation nation = nationService.getNationById(town.getNationId()).orElse(null);
				if (nation == null) { p.sendMessage("§cNation not found."); return true; }
				if (args.length == 1) {
					double mine = nationService.getMemberDeposit(nation, p.getUniqueId());
					p.sendMessage("§6Your nation bank deposit: §e$" + String.format(java.util.Locale.US, "%.2f", mine));
					return true;
				}
				if (args.length >= 2 && args[1].equalsIgnoreCase("transfer")) {
					if (args.length < 4) { p.sendMessage("§eUsage: /" + label + " bank transfer <player> <amount>"); return true; }
					Player target = p.getServer().getPlayerExact(args[2]);
					if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
					double amt;
					try { amt = Double.parseDouble(args[3]); } catch (NumberFormatException ex) { p.sendMessage("§cInvalid amount."); return true; }
					if (amt <= 0) { p.sendMessage("§cAmount must be positive."); return true; }
					// Withdraw from caller's member deposit into target's member deposit
					if (nationService.getMemberDeposit(nation, p.getUniqueId()) < amt) { p.sendMessage("§cYou don't have that much in the nation bank."); return true; }
					// Perform transfer via withdraw+deposit to keep totals consistent
					boolean ok = nationService.withdraw(nation, p.getUniqueId(), amt);
					if (!ok) { p.sendMessage("§cTransfer failed."); return true; }
					nationService.deposit(nation, target.getUniqueId(), amt);
					p.sendMessage("§aTransferred §e$" + amt + " §ato §e" + target.getName() + "§a within nation bank.");
					target.sendMessage("§aYou received §e$" + amt + " §ain your nation bank from §e" + p.getName());
					return true;
				}
				if (args.length < 3) { p.sendMessage("§eUsage: /" + label + " bank <deposit|withdraw> <amount> | transfer <player> <amount>"); return true; }
				double amt;
				try { amt = Double.parseDouble(args[2]); } catch (NumberFormatException ex) { p.sendMessage("§cInvalid amount."); return true; }
				if (amt <= 0) { p.sendMessage("§cAmount must be positive."); return true; }
				if (args[1].equalsIgnoreCase("deposit")) {
					if (com.tntowns.TNtownsPlugin.getInstance().getEconomy().getBalance(p) < amt) { p.sendMessage("§cInsufficient funds."); return true; }
					com.tntowns.TNtownsPlugin.getInstance().getEconomy().withdrawPlayer(p, amt);
					nationService.deposit(nation, p.getUniqueId(), amt);
					p.sendMessage("§aDeposited §e$" + amt + "§a into nation bank.");
					return true;
				} else if (args[1].equalsIgnoreCase("withdraw")) {
					if (!nationService.withdraw(nation, p.getUniqueId(), amt)) { p.sendMessage("§cYou cannot withdraw more than your deposit or bank has."); return true; }
					com.tntowns.TNtownsPlugin.getInstance().getEconomy().depositPlayer(p, amt);
					p.sendMessage("§aWithdrew §e$" + amt + "§a from nation bank.");
					return true;
				} else {
					p.sendMessage("§eUsage: /" + label + " bank <deposit|withdraw> <amount> | transfer <player> <amount>");
					return true;
				}
			}
			case "market": {
				if (town.getNationId() == null) { p.sendMessage("§cYour town is not in a nation."); return true; }
				com.tntowns.model.Nation nation = nationService.getNationById(town.getNationId()).orElse(null);
				if (nation == null) { p.sendMessage("§cNation not found."); return true; }
				com.tntowns.service.MarketService ms = com.tntowns.TNtownsPlugin.getInstance().getMarketService();
				com.tntowns.model.NationMarket m = ms.getOrCreate(nation.getId());
				if (args.length == 1) { // info
					p.sendMessage("§6Nation Market:");
					p.sendMessage("§7Price: §e$" + String.format(java.util.Locale.US, "%.2f", ms.getSpotPrice(m)) + " §7| Liquidity: §e$" + String.format(java.util.Locale.US, "%.2f", m.getCurrencyReserve()) + " §7/ Shares: §e" + String.format(java.util.Locale.US, "%.2f", m.getShareReserve()));
					p.sendMessage("§7Your shares: §e" + String.format(java.util.Locale.US, "%.4f", m.getHolderShares().getOrDefault(p.getUniqueId().toString(), 0.0)));
					return true;
				}
				if (args.length >= 2 && args[1].equalsIgnoreCase("price")) {
					p.sendMessage("§6Current price: §e$" + String.format(java.util.Locale.US, "%.4f", ms.getSpotPrice(m)) + " §7(per share)");
					return true;
				}
				if (args.length >= 3 && args[1].equalsIgnoreCase("buy")) {
					double amount;
					try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException ex) { p.sendMessage("§cInvalid amount."); return true; }
					if (amount <= 0) { p.sendMessage("§cAmount must be positive."); return true; }
					double minShares = (args.length >= 4 ? safeDouble(args[3], 0.0) : 0.0);
					// Policy: nation members cannot buy their own nation's shares
					if (town.getNationId() != null && town.getNationId().equalsIgnoreCase(nation.getId())) {
						p.sendMessage("§cNation members cannot buy their own nation's shares.");
						return true;
					}
					double quote = ms.quoteBuy(m, amount);
					p.sendMessage("§7Quote: buy with §e$" + String.format(java.util.Locale.US, "%.2f", amount) + " §7→ receive ≈ §e" + String.format(java.util.Locale.US, "%.4f", quote) + " §7shares.");
					if (com.tntowns.TNtownsPlugin.getInstance().getEconomy().getBalance(p) < amount) { p.sendMessage("§cInsufficient funds."); return true; }
					boolean ok = ms.buy(nation, m, p.getUniqueId(), amount, minShares);
					p.sendMessage(ok ? "§aTrade executed." : "§cTrade failed.");
					return true;
				}
				if (args.length >= 3 && args[1].equalsIgnoreCase("sell")) {
					double shares;
					try { shares = Double.parseDouble(args[2]); } catch (NumberFormatException ex) { p.sendMessage("§cInvalid amount."); return true; }
					if (shares <= 0) { p.sendMessage("§cAmount must be positive."); return true; }
					double minOut = (args.length >= 4 ? safeDouble(args[3], 0.0) : 0.0);
					double quote = ms.quoteSell(m, shares);
					p.sendMessage("§7Quote: sell §e" + String.format(java.util.Locale.US, "%.4f", shares) + " §7shares → receive ≈ §e$" + String.format(java.util.Locale.US, "%.2f", quote));
					boolean ok = ms.sell(nation, m, p.getUniqueId(), shares, minOut);
					p.sendMessage(ok ? "§aTrade executed." : "§cTrade failed.");
					return true;
				}
				if (args.length >= 2 && args[1].equalsIgnoreCase("admin")) {
					boolean isAdmin = nationService.isAdminOrKingTownMayor(nation, town, p.getUniqueId());
					if (!isAdmin) { p.sendMessage("§cYou are not a nation admin."); return true; }
					if (args.length >= 3 && args[2].equalsIgnoreCase("ipo")) {
						if (args.length < 5) { p.sendMessage("§eUsage: /" + label + " market admin ipo <currencySeed> <shareSeed>"); return true; }
						double c = safeDouble(args[3], -1);
						double s = safeDouble(args[4], -1);
						if (c <= 0 || s <= 0) { p.sendMessage("§cSeeds must be positive."); return true; }
						try {
							com.tntowns.TNtownsPlugin.getInstance().getMarketService().seedIpo(nation, m, c, s);
							p.sendMessage("§aIPO seeded. Price ≈ §e$" + String.format(java.util.Locale.US, "%.4f", com.tntowns.TNtownsPlugin.getInstance().getMarketService().getSpotPrice(m)));
						} catch (Exception ex) {
							p.sendMessage("§cIPO failed: " + ex.getMessage());
						}
						return true;
					}
					p.sendMessage("§eUsage: /" + label + " market admin <ipo>");
					return true;
				}
				p.sendMessage("§eUsage: /" + label + " market [info|price|buy <$> [minShares]|sell <shares> [min$]|admin <ipo>]");
				return true;
			}
			// 'expand' moved under admin
			case "admin": {
				if (args.length == 1) { p.sendMessage("§eUsage: /" + label + " admin <invite|kick|rank|bank [list]|expand|tax set <amount>|tax get|debug gettaxes>"); return true; }
				String a = args[1].toLowerCase();
				if (town.getNationId() == null) { p.sendMessage("§cYour town is not in a nation."); return true; }
				com.tntowns.model.Nation nationAdmin = nationService.getNationById(town.getNationId()).orElse(null);
				if (nationAdmin == null) { p.sendMessage("§cNation not found."); return true; }
				boolean isAdmin = nationService.isAdminOrKingTownMayor(nationAdmin, town, p.getUniqueId());
				if (!isAdmin) { p.sendMessage("§cYou are not a nation admin."); return true; }
				switch (a) {
					case "invite": {
						if (args.length < 3) { p.sendMessage("§eUsage: /" + label + " admin invite <player>"); return true; }
						Player target = p.getServer().getPlayerExact(args[2]);
						if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
						// Check if target already in a nation
						if (nationService.findNationOfMember(target.getUniqueId()).isPresent()) {
							p.sendMessage("§cThat player is already in a nation.");
							return true;
						}
						com.tntowns.TNtownsPlugin.getInstance().getInviteManager().invite(target.getUniqueId(), town.getNationId());
						p.sendMessage("§aInvite sent to §e" + target.getName());
						return true;
					}
					case "kick": {
						if (args.length < 3) { p.sendMessage("§eUsage: /" + label + " admin kick <player>"); return true; }
						Player target = p.getServer().getPlayerExact(args[2]);
						if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
						com.tntowns.model.Nation nation = nationService.getNationById(town.getNationId()).orElse(null);
						if (nation == null) { p.sendMessage("§cNation not found."); return true; }
						nation.getMemberRanks().remove(target.getUniqueId().toString());
						p.sendMessage("§aKicked §e" + target.getName() + " §afrom the nation.");
						return true;
					}
					case "rank": {
						if (args.length < 4) { p.sendMessage("§eUsage: /" + label + " admin rank <player> <E1-E9>"); return true; }
						Player target = p.getServer().getPlayerExact(args[2]);
						if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
						com.tntowns.model.NationRank rank = com.tntowns.model.NationRank.fromString(args[3]);
						com.tntowns.model.Nation nation = nationService.getNationById(town.getNationId()).orElse(null);
						if (nation == null) { p.sendMessage("§cNation not found."); return true; }
						nation.getMemberRanks().put(target.getUniqueId().toString(), rank.name());
						p.sendMessage("§aSet nation rank of §e" + target.getName() + " §ato §e" + rank.name());
						return true;
					}
					case "bank": {
						com.tntowns.model.Nation nation = nationService.getNationById(town.getNationId()).orElse(null);
						if (nation == null) { p.sendMessage("§cNation not found."); return true; }
						p.sendMessage("§6Nation Bank Info:");
						p.sendMessage("§7Total bank: §e$" + String.format(java.util.Locale.US, "%.2f", nation.getBankBalance()));
						p.sendMessage("§7Account holders: §e" + nation.getMemberDeposits().size());
						if (args.length >= 3 && args[2].equalsIgnoreCase("list")) {
							p.sendMessage("§6Member deposits:");
							for (java.util.Map.Entry<String, Double> e : nation.getMemberDeposits().entrySet()) {
								java.util.UUID id = java.util.UUID.fromString(e.getKey());
								String name = p.getServer().getOfflinePlayer(id).getName();
								p.sendMessage("§e" + (name != null ? name : id.toString()) + "§7: §a$" + String.format(java.util.Locale.US, "%.2f", e.getValue()));
							}
						}
						return true;
					}
					case "expand": {
						com.tntowns.model.Nation nation = nationService.getNationById(town.getNationId()).orElse(null);
						if (nation == null) { p.sendMessage("§cNation not found."); return true; }
						double cost = nationService.getNextTerritoryCost(com.tntowns.TNtownsPlugin.getInstance().getConfig(), nation);
						if (nation.getBankBalance() < cost) { p.sendMessage("§cNation bank requires $" + cost + " to buy a new territory slot."); return true; }
						// Evenly divide cost among members with accounts
						nationService.spendFromMembers(nation, cost);
						nation.setExtraTerritorySlots(nation.getExtraTerritorySlots() + 1);
						p.sendMessage("§aPurchased new nation territory slot for §e$" + cost + ".");
						return true;
					}
					case "tax": {
						if (args.length < 3) { p.sendMessage("§eUsage: /" + label + " admin tax <set <amount>|get|debug gettaxes>"); return true; }
						com.tntowns.model.Nation nation = nationAdmin;
						if (args[2].equalsIgnoreCase("set")) {
							if (args.length < 4) { p.sendMessage("§eUsage: /" + label + " admin tax set <amount>"); return true; }
							try {
								double amt = Double.parseDouble(args[3]);
								nation.setPerMemberTax(amt);
								p.sendMessage("§aSet nation per-member tax to §e$" + amt);
							} catch (NumberFormatException ex) { p.sendMessage("§cInvalid amount."); }
							return true;
						} else if (args[2].equalsIgnoreCase("get")) {
							double amt = nation.getPerMemberTax();
							p.sendMessage("§6Nation per-member tax: §e$" + amt);
							return true;
						} else if (args[2].equalsIgnoreCase("debug") && args.length >= 4 && args[3].equalsIgnoreCase("gettaxes")) {
							double per = nation.getPerMemberTax();
							int count = nationService.collectTaxesPerMember(nation, per);
							p.sendMessage("§aCollected taxes: §e$" + per + " §afrom §e" + count + "§a members. New bank: §e$" + String.format(java.util.Locale.US, "%.2f", nation.getBankBalance()));
							return true;
						}
						p.sendMessage("§eUsage: /" + label + " admin tax <set <amount>|get|debug gettaxes>");
						return true;
					}
					case "permission": {
						if (args.length < 4) { p.sendMessage("§eUsage: /" + label + " admin permission <give|take> <player>"); return true; }
						String action = args[2].toLowerCase();
						Player target = p.getServer().getPlayerExact(args[3]);
						if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
						if (action.equals("give")) {
							nationService.grantAdmin(nationAdmin, target.getUniqueId());
							p.sendMessage("§aGranted nation admin to §e" + target.getName());
							return true;
						} else if (action.equals("take")) {
							nationService.revokeAdmin(nationAdmin, target.getUniqueId());
							p.sendMessage("§aRevoked nation admin from §e" + target.getName());
							return true;
						} else {
							p.sendMessage("§eUsage: /" + label + " admin permission <give|take> <player>");
							return true;
						}
					}
					default:
						p.sendMessage("§cUnknown admin subcommand.");
						return true;
				}
			}
			default:
				p.sendMessage("§cUnknown subcommand.");
				return true;
		}
	}

	private double safeDouble(String s, double def) {
		try { return Double.parseDouble(s); } catch (Exception ex) { return def; }
	}

	private String joinArgs(String[] arr, int start) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < arr.length; i++) { if (i > start) sb.append(' '); sb.append(arr[i]); }
		return sb.toString();
	}
	@Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
		if (!(sender instanceof Player)) return Collections.emptyList();
		List<String> subs = Arrays.asList("create","addtown","ally","enemy","bank","expand","admin","accept");
		if (args.length == 1) {
			return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(java.util.stream.Collectors.toList());
		}
		String sub = args[0].toLowerCase();
		switch (sub) {
			case "bank":
				if (args.length == 2) return Arrays.asList("deposit","withdraw").stream().filter(v -> v.startsWith(args[1].toLowerCase())).collect(java.util.stream.Collectors.toList());
				return Collections.emptyList();
			case "admin":
				if (args.length == 2) return Arrays.asList("invite","kick","rank","bank","tax").stream().filter(v -> v.startsWith(args[1].toLowerCase())).collect(java.util.stream.Collectors.toList());
				if (args.length == 3 && (args[1].equalsIgnoreCase("invite") || args[1].equalsIgnoreCase("kick") || args[1].equalsIgnoreCase("rank"))) {
					String pref = args[2].toLowerCase();
					return ((Player) sender).getServer().getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(pref)).collect(java.util.stream.Collectors.toList());
				}
				if (args.length == 4 && args[1].equalsIgnoreCase("rank")) {
					List<String> ranks = Arrays.asList("E1","E2","E3","E4","E5","E6","E7","E8","E9");
					String pref = args[3].toUpperCase();
					return ranks.stream().filter(r -> r.startsWith(pref)).collect(java.util.stream.Collectors.toList());
				}
				if (args.length == 3 && args[1].equalsIgnoreCase("bank")) {
					return Collections.singletonList("list");
				}
				if (args.length == 3 && args[1].equalsIgnoreCase("tax")) {
					return Arrays.asList("set","get","debug");
				}
				if (args.length == 4 && args[1].equalsIgnoreCase("tax") && args[2].equalsIgnoreCase("debug")) {
					return Collections.singletonList("gettaxes");
				}
				return Collections.emptyList();
			default:
				return Collections.emptyList();
		}
	}
}



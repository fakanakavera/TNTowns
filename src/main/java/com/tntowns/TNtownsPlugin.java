package com.tntowns;

import com.tntowns.service.TownService;
import com.tntowns.storage.DataStore;
import com.tntowns.commands.TNTownCommand;
import com.tntowns.listeners.ProtectionListener;
import com.tntowns.listeners.ResidentListener;
import com.tntowns.listeners.ChatChannelListener;
import com.tntowns.listeners.PvpListener;
import com.tntowns.listeners.JailOutlawListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class TNtownsPlugin extends JavaPlugin {

	private static TNtownsPlugin instance;
	private Economy economy;
	private DataStore dataStore;
	private TownService townService;
	private InviteManager inviteManager;
	private com.tntowns.service.NationService nationService;
	private com.tntowns.service.PlotService plotService;

	public static TNtownsPlugin getInstance() {
		return instance;
	}

	public Economy getEconomy() {
		return economy;
	}

	public TownService getTownService() {
		return townService;
	}

	public InviteManager getInviteManager() {
		return inviteManager;
	}

	public com.tntowns.service.NationService getNationService() {
		return nationService;
	}

	@Override
	public void onEnable() {
		instance = this;
		saveDefaultConfig();

		if (!setupEconomy()) {
			getLogger().severe("Vault economy not found. Disabling TNtowns.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// Storage and services
		this.dataStore = new DataStore(getDataFolder());
		this.townService = new TownService(dataStore);
		this.inviteManager = new InviteManager();
		this.nationService = new com.tntowns.service.NationService(dataStore);
		this.plotService = new com.tntowns.service.PlotService(dataStore);
		try {
			dataStore.loadAll();
			getLogger().info("Loaded residents, towns, nations.");
		} catch (Exception ex) {
			getLogger().warning("Failed to load data: " + ex.getMessage());
		}

		// Register command
		if (getCommand("tntown") != null) {
			TNTownCommand handler = new TNTownCommand(this, townService);
			getCommand("tntown").setExecutor(handler);
			getCommand("tntown").setTabCompleter(handler);
		}
		if (getCommand("tntownchat") != null) {
			com.tntowns.commands.ChatToggleCommand handler = new com.tntowns.commands.ChatToggleCommand(townService);
			getCommand("tntownchat").setExecutor(handler);
			getCommand("tntownchat").setTabCompleter(handler);
		}
		if (getCommand("nation") != null) {
			com.tntowns.commands.NationCommand handler = new com.tntowns.commands.NationCommand(townService, nationService);
			getCommand("nation").setExecutor(handler);
			getCommand("nation").setTabCompleter(handler);
		}
		if (getCommand("plot") != null) {
			com.tntowns.commands.PlotCommand handler = new com.tntowns.commands.PlotCommand(this, townService, plotService);
			getCommand("plot").setExecutor(handler);
			getCommand("plot").setTabCompleter(handler);
		}

		// Register listeners
		getServer().getPluginManager().registerEvents(new ProtectionListener(townService), this);
		getServer().getPluginManager().registerEvents(new ResidentListener(townService), this);
		getServer().getPluginManager().registerEvents(new ChatChannelListener(townService), this);
		getServer().getPluginManager().registerEvents(new PvpListener(townService), this);
		getServer().getPluginManager().registerEvents(new JailOutlawListener(townService), this);
		getServer().getPluginManager().registerEvents(new com.tntowns.listeners.CompassListener(townService), this);

		// Schedule upkeep task
		int minutes = getConfig().getInt("scheduler.upkeep_interval_minutes", 1440);
		long ticks = minutes * 60L * 20L;
		getServer().getScheduler().runTaskTimer(this, new com.tntowns.tasks.UpkeepTask(this), ticks, ticks);

		getLogger().info("TNtowns enabled.");
	}

	@Override
	public void onDisable() {
		if (dataStore != null) {
			try {
				dataStore.saveAll();
				getLogger().info("Saved residents, towns, nations.");
			} catch (Exception ex) {
				getLogger().severe("Failed to save data: " + ex.getMessage());
			}
		}
		getLogger().info("TNtowns disabled.");
	}

	private boolean setupEconomy() {
		if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		economy = rsp.getProvider();
		return economy != null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("tntown")) {
			return false;
		}
		if (!sender.hasPermission("tntowns.use")) {
			sender.sendMessage("§cYou do not have permission to use TNtowns.");
			return true;
		}
		if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
			sender.sendMessage("§6TNtowns Commands:");
			sender.sendMessage("§e/tntown help §7- Show this help.");
			return true;
		}
		sender.sendMessage("§eUnknown subcommand. Use /tntown help");
		return true;
	}
}



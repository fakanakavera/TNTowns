package com.tntowns.listeners;

import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.TownService;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public class ProtectionListener implements Listener {

	private final TownService townService;

	public ProtectionListener(TownService townService) {
		this.townService = townService;
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent event) {
		if (shouldCancel(event.getPlayer(), event.getBlock().getChunk())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("§cYou cannot build here.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBreak(BlockBreakEvent event) {
		if (shouldCancel(event.getPlayer(), event.getBlock().getChunk())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("§cYou cannot break blocks here.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getClickedBlock() == null) return;
		if (shouldCancel(event.getPlayer(), event.getClickedBlock().getChunk())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("§cYou cannot interact here.");
		}
	}

	private boolean shouldCancel(Player player, Chunk chunk) {
		Optional<Town> owner = townService.findTownByChunk(chunk);
		if (owner.isEmpty()) return false; // wilderness allowed
		Resident res = townService.getOrCreateResident(player);
		Town town = owner.get();
		// Allow members of owning town
		if (res.getTownId() != null && town.getId().equalsIgnoreCase(res.getTownId())) {
			return false;
		}
		// Allow plot owners
		com.tntowns.model.Plot plot = townService.getDataStore().getPlots().get(chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ());
		if (plot != null && plot.getOwnerUuid() != null && plot.getOwnerUuid().equalsIgnoreCase(player.getUniqueId().toString())) {
			return false;
		}
		// Optionally allow allies
		boolean allowAllies = com.tntowns.TNtownsPlugin.getInstance().getConfig().getBoolean("protection.allow_allies_build", false);
		if (allowAllies && res.getTownId() != null) {
			Town actorTown = townService.getTownById(res.getTownId()).orElse(null);
			if (actorTown != null) {
				if (town.getAllyTownIds().contains(actorTown.getId())) return false;
				if (town.getNationId() != null && actorTown.getNationId() != null && town.getNationId().equalsIgnoreCase(actorTown.getNationId())) return false;
			}
		}
		// Deny by default
		return true;
	}
}



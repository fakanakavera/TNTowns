package com.tntowns.listeners;

import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.TownService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

import java.util.Optional;

public class CompassListener implements Listener {

	private final TownService townService;

	public CompassListener(TownService townService) {
		this.townService = townService;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		updateCompass(event.getPlayer());
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		updateCompass(event.getPlayer());
	}

	@EventHandler
	public void onItemHeld(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		org.bukkit.inventory.ItemStack item = player.getInventory().getItem(event.getNewSlot());
		if (item != null && item.getType() == Material.COMPASS) {
			updateCompass(player);
		}
	}

	private void updateCompass(Player player) {
		try {
			Resident res = townService.getOrCreateResident(player);
			Optional<Town> optTown = townService.getResidentTown(res);
			if (optTown.isEmpty()) return;
			Optional<org.bukkit.Location> loc = townService.getTownCompassLocation(optTown.get(), player.getServer());
			loc.ifPresent(player::setCompassTarget);
		} catch (Exception ignored) {}
	}
}



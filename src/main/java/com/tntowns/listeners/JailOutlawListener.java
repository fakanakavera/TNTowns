package com.tntowns.listeners;

import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.TownService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

public class JailOutlawListener implements Listener {

    private final TownService townService;

    public JailOutlawListener(TownService townService) {
        this.townService = townService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Resident resident = townService.getOrCreateResident(player);
        if (resident.isJailed()) {
            Optional<Town> optTown = townService.getResidentTown(resident);
            if (optTown.isPresent()) {
                Town town = optTown.get();
                if (town.getJailWorld() != null) {
                    Location jail = new Location(player.getServer().getWorld(town.getJailWorld()), town.getJailX() + 0.5, town.getJailY(), town.getJailZ() + 0.5);
                    if (event.getTo() != null && !event.getTo().getWorld().equals(jail.getWorld())) {
                        player.teleport(jail);
                        return;
                    }
                    if (event.getTo() != null && event.getTo().distanceSquared(jail) > (16 * 16)) {
                        player.teleport(jail);
                    }
                }
            }
        }
    }
}



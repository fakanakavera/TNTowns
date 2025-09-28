package com.tntowns.listeners;

import com.tntowns.model.Town;
import com.tntowns.service.TownService;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class PvpListener implements Listener {

    private final TownService townService;

    public PvpListener(TownService townService) {
        this.townService = townService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        if (!(damager instanceof Player) || !(victim instanceof Player)) return;

        Player attacker = (Player) damager;
        Player defender = (Player) victim;

        Location loc = defender.getLocation();
        Optional<Town> optTown = townService.findTownByChunk(loc.getChunk());
        if (optTown.isEmpty()) return; // wilderness, allow
        Town town = optTown.get();
        if (!town.isPvpEnabled()) {
            event.setCancelled(true);
            return;
        }
        // Allies cannot hurt each other when inside town borders
        String attackerTownId = townService.getResident(attacker.getUniqueId()).flatMap(townService::getResidentTown).map(Town::getId).orElse(null);
        String defenderTownId = townService.getResident(defender.getUniqueId()).flatMap(townService::getResidentTown).map(Town::getId).orElse(null);
        if (attackerTownId != null && defenderTownId != null) {
            if (attackerTownId.equalsIgnoreCase(defenderTownId)) {
                event.setCancelled(true);
                return;
            }
            if (town.getAllyTownIds().contains(attackerTownId) || town.getAllyTownIds().contains(defenderTownId)) {
                event.setCancelled(true);
            }
        }
    }
}



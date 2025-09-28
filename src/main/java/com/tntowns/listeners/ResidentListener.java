package com.tntowns.listeners;

import com.tntowns.service.TownService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ResidentListener implements Listener {

    private final TownService townService;

    public ResidentListener(TownService townService) {
        this.townService = townService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        townService.getOrCreateResident(event.getPlayer());
    }
}



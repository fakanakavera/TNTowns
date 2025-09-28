package com.tntowns.listeners;

import com.tntowns.model.ChatChannel;
import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.TownService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;

import java.util.Iterator;
import java.util.Optional;

public class ChatChannelListener implements Listener {

    private final TownService townService;

    public ChatChannelListener(TownService townService) {
        this.townService = townService;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Resident res = townService.getOrCreateResident(player);
        ChatChannel channel = res.getChatChannel();
        switch (channel) {
            case GLOBAL:
                return; // let it through
            case TOWN: {
                Optional<Town> optTown = townService.getResidentTown(res);
                if (optTown.isEmpty()) return;
                Town town = optTown.get();
                filterRecipientsToTown(event, town);
                return;
            }
            case NATION: {
                Optional<Town> optTown = townService.getResidentTown(res);
                if (optTown.isEmpty()) return;
                Town town = optTown.get();
                filterRecipientsToNation(event, town);
                return;
            }
            case LOCAL: {
                filterRecipientsToLocal(event, player, 100.0);
                return;
            }
        }
    }

    private void filterRecipientsToTown(AsyncChatEvent event, Town town) {
        Iterator<Audience> it = event.viewers().iterator();
        while (it.hasNext()) {
            Audience a = it.next();
            if (!(a instanceof Player)) { it.remove(); continue; }
            Player p = (Player) a;
            Resident r = townService.getOrCreateResident(p);
            if (r.getTownId() == null || !r.getTownId().equalsIgnoreCase(town.getId())) {
                it.remove();
            }
        }
    }

    private void filterRecipientsToNation(AsyncChatEvent event, Town senderTown) {
        String nationId = senderTown.getNationId();
        Iterator<Audience> it = event.viewers().iterator();
        while (it.hasNext()) {
            Audience a = it.next();
            if (!(a instanceof Player)) { it.remove(); continue; }
            Player p = (Player) a;
            Resident r = townService.getOrCreateResident(p);
            if (r.getTownId() == null) { it.remove(); continue; }
            Optional<Town> t = townService.getTownById(r.getTownId());
            if (t.isEmpty() || t.get().getNationId() == null || !t.get().getNationId().equalsIgnoreCase(nationId)) {
                it.remove();
            }
        }
    }

    private void filterRecipientsToLocal(AsyncChatEvent event, Player player, double radius) {
        Iterator<Audience> it = event.viewers().iterator();
        while (it.hasNext()) {
            Audience a = it.next();
            if (!(a instanceof Player)) { it.remove(); continue; }
            Player p = (Player) a;
            if (!p.getWorld().equals(player.getWorld()) || p.getLocation().distanceSquared(player.getLocation()) > radius * radius) {
                it.remove();
            }
        }
    }
}



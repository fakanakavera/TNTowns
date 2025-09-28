package com.tntowns;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InviteManager {

    private final Map<UUID, String> pendingTownInvites = new HashMap<>(); // player -> townId

    public void invite(UUID playerId, String townId) {
        pendingTownInvites.put(playerId, townId.toLowerCase());
    }

    public String getInvite(UUID playerId) {
        return pendingTownInvites.get(playerId);
    }

    public void clearInvite(UUID playerId) {
        pendingTownInvites.remove(playerId);
    }
}



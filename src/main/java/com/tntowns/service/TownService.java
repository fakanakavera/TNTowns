package com.tntowns.service;

import com.tntowns.model.Resident;
import com.tntowns.model.ResidentRole;
import com.tntowns.model.Town;
import com.tntowns.storage.DataStore;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Server;

import java.util.Optional;
import java.util.UUID;

public class TownService {

    private final DataStore dataStore;

    public TownService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public Resident getOrCreateResident(Player player) {
        return dataStore.getResidents().computeIfAbsent(player.getUniqueId(), id -> new Resident(id, player.getName()));
    }

    public Optional<Resident> getResident(UUID uuid) {
        return Optional.ofNullable(dataStore.getResidents().get(uuid));
    }

    public Optional<Town> getTownById(String id) {
        return Optional.ofNullable(dataStore.getTowns().get(id));
    }

    public Optional<Town> getResidentTown(Resident resident) {
        if (resident.getTownId() == null) return Optional.empty();
        return getTownById(resident.getTownId());
    }

    public Town createTown(Player mayor, String townName) {
        Resident resident = getOrCreateResident(mayor);
        if (resident.getTownId() != null) {
            throw new IllegalStateException("Already in a town");
        }
        Town town = new Town();
        town.setId(dataStore.generateNextTownId());
        town.setName(townName);
        town.setMayorUuid(mayor.getUniqueId().toString());
        town.getResidentUuids().add(mayor.getUniqueId().toString());
        dataStore.getTowns().put(town.getId(), town);

        resident.setTownId(town.getId());
        resident.setRole(ResidentRole.MAYOR);
        return town;
    }

    public boolean isChunkClaimed(Chunk chunk) {
        String key = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
        for (Town town : dataStore.getTowns().values()) {
            if (town.getClaimKeys().contains(key)) return true;
        }
        return false;
    }

    public Optional<Town> findTownByChunk(Chunk chunk) {
        String key = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
        for (Town town : dataStore.getTowns().values()) {
            if (town.getClaimKeys().contains(key)) return Optional.of(town);
        }
        return Optional.empty();
    }

    public boolean claimChunk(Player player, Town town, Chunk chunk) {
        String key = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
        if (isChunkClaimed(chunk)) return false;
        town.getClaimKeys().add(key);
        return true;
    }

    public boolean unclaimChunk(Town town, Chunk chunk) {
        String key = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
        return town.getClaimKeys().remove(key);
    }

    public void deposit(Town town, double amount) {
        town.setBankBalance(Math.max(0.0, town.getBankBalance() + amount));
    }

    public boolean withdraw(Town town, double amount) {
        if (town.getBankBalance() < amount) return false;
        town.setBankBalance(town.getBankBalance() - amount);
        return true;
    }

    public boolean isMayor(Resident resident, Town town) {
        return town.getMayorUuid() != null && town.getMayorUuid().equalsIgnoreCase(resident.getUuid().toString());
    }

    public boolean addAllyTown(Town town, String allyTownId) {
        boolean changed = town.getAllyTownIds().add(allyTownId.toLowerCase());
        town.getEnemyTownIds().remove(allyTownId.toLowerCase());
        return changed;
    }

    public boolean addEnemyTown(Town town, String enemyTownId) {
        boolean changed = town.getEnemyTownIds().add(enemyTownId.toLowerCase());
        town.getAllyTownIds().remove(enemyTownId.toLowerCase());
        return changed;
    }

    public void setOutlaw(Town town, UUID playerId, boolean outlaw) {
        String id = playerId.toString();
        if (outlaw) town.getOutlawUuids().add(id); else town.getOutlawUuids().remove(id);
    }

    public void setJail(Town town, String world, int x, int y, int z) {
        town.setJailWorld(world);
        town.setJailX(x);
        town.setJailY(y);
        town.setJailZ(z);
    }

    public void disbandTown(Town town) {
        // Snapshot claim keys before modifications to ensure dependent removals work
        java.util.Set<String> claimKeysSnapshot = new java.util.HashSet<>(town.getClaimKeys());

        // Clear residents
        for (String uuidStr : new java.util.HashSet<>(town.getResidentUuids())) {
            try {
                UUID id = UUID.fromString(uuidStr);
                Resident r = dataStore.getResidents().get(id);
                if (r != null) {
                    r.setTownId(null);
                    r.setRole(ResidentRole.MEMBER);
                }
            } catch (IllegalArgumentException ignored) {}
        }
        // Remove plots belonging to this town's claims
        dataStore.getPlots().keySet().removeIf(claimKeysSnapshot::contains);
        // Remove claims
        town.getClaimKeys().clear();
        // Remove from map
        dataStore.getTowns().remove(town.getId());
    }

    public java.util.Optional<Location> getTownCompassLocation(Town town, Server server) {
        if (town == null) return java.util.Optional.empty();
        // Prefer center of claims if available
        java.util.Set<String> claimKeys = town.getClaimKeys();
        if (claimKeys != null && !claimKeys.isEmpty()) {
            // Group by world and pick the world with most claims
            java.util.Map<String, java.util.List<int[]>> byWorld = new java.util.HashMap<>();
            for (String key : claimKeys) {
                if (key == null) continue;
                String[] parts = key.split(":");
                if (parts.length != 3) continue;
                String worldName = parts[0];
                try {
                    int cx = Integer.parseInt(parts[1]);
                    int cz = Integer.parseInt(parts[2]);
                    byWorld.computeIfAbsent(worldName, k -> new java.util.ArrayList<>()).add(new int[] { cx, cz });
                } catch (NumberFormatException ignored) {}
            }
            String selectedWorld = null;
            java.util.List<int[]> coords = null;
            for (java.util.Map.Entry<String, java.util.List<int[]>> e : byWorld.entrySet()) {
                if (selectedWorld == null || coords == null || e.getValue().size() > coords.size()) {
                    selectedWorld = e.getKey();
                    coords = e.getValue();
                }
            }
            if (selectedWorld != null && coords != null && !coords.isEmpty()) {
                long sumX = 0;
                long sumZ = 0;
                for (int[] c : coords) { sumX += c[0]; sumZ += c[1]; }
                int avgChunkX = (int) Math.round(sumX / (double) coords.size());
                int avgChunkZ = (int) Math.round(sumZ / (double) coords.size());
                World world = server.getWorld(selectedWorld);
                if (world != null) {
                    int blockX = avgChunkX * 16 + 8;
                    int blockZ = avgChunkZ * 16 + 8;
                    int y = world.getHighestBlockYAt(blockX, blockZ);
                    return java.util.Optional.of(new Location(world, blockX + 0.5, y, blockZ + 0.5));
                }
            }
        }
        // Fallback to jail location if set
        if (town.getJailWorld() != null) {
            World world = server.getWorld(town.getJailWorld());
            if (world != null) {
                return java.util.Optional.of(new Location(world, town.getJailX() + 0.5, Math.max(1, town.getJailY()), town.getJailZ() + 0.5));
            }
        }
        return java.util.Optional.empty();
    }

    public boolean isTownNameTaken(String townName) {
        for (Town t : dataStore.getTowns().values()) {
            if (t.getName() != null && t.getName().equalsIgnoreCase(townName)) return true;
        }
        return false;
    }
}



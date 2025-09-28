package com.tntowns.service;

import com.tntowns.model.Nation;
import com.tntowns.model.Town;
import com.tntowns.storage.DataStore;
import org.bukkit.Chunk;

import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.Deque;

public class NationService {

    private final DataStore dataStore;

    public NationService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Optional<Nation> getNationById(String id) {
        return Optional.ofNullable(dataStore.getNations().get(id.toLowerCase()));
    }

    public Nation createNation(String name, Town capital) {
        String nid = dataStore.generateNextNationId();
        Nation n = new Nation();
        n.setId(nid);
        n.setName(name);
        n.setKingTownId(capital.getId());
        n.getTownIds().add(capital.getId());
        dataStore.getNations().put(nid, n);
        capital.setNationId(nid);
        return n;
    }

    public boolean addTown(Nation nation, Town town) {
        if (nation.getTownIds().add(town.getId())) {
            town.setNationId(nation.getId());
            return true;
        }
        return false;
    }

    public void ally(Nation a, Nation b) {
        a.getAllyNationIds().add(b.getId());
        b.getAllyNationIds().add(a.getId());
        a.getEnemyNationIds().remove(b.getId());
        b.getEnemyNationIds().remove(a.getId());
    }

    public void enemy(Nation a, Nation b) {
        a.getEnemyNationIds().add(b.getId());
        b.getEnemyNationIds().add(a.getId());
        a.getAllyNationIds().remove(b.getId());
        b.getAllyNationIds().remove(a.getId());
    }

    public boolean areAllied(Town t1, Town t2) {
        if (t1.getId().equalsIgnoreCase(t2.getId())) return true;
        if (t1.getAllyTownIds().contains(t2.getId()) || t2.getAllyTownIds().contains(t1.getId())) return true;
        if (t1.getNationId() == null || t2.getNationId() == null) return false;
        Nation n1 = dataStore.getNations().get(t1.getNationId());
        Nation n2 = dataStore.getNations().get(t2.getNationId());
        if (n1 == null || n2 == null) return false;
        return n1.getAllyNationIds().contains(n2.getId());
    }

    // ===== Territory helpers =====

    public Set<String> getNationClaimKeys(Nation nation) {
        Set<String> keys = new HashSet<>();
        for (String townId : nation.getTownIds()) {
            Town t = dataStore.getTowns().get(townId);
            if (t != null) keys.addAll(t.getClaimKeys());
        }
        return keys;
    }

    private static String keyOf(String world, int x, int z) {
        return world + ":" + x + ":" + z;
    }

    public int countNationTerritories(Nation nation) {
        Set<String> keys = getNationClaimKeys(nation);
        Set<String> visited = new HashSet<>();
        int components = 0;
        for (String key : keys) {
            if (visited.contains(key)) continue;
            components++;
            // BFS
            Deque<String> dq = new ArrayDeque<>();
            dq.add(key);
            visited.add(key);
            while (!dq.isEmpty()) {
                String cur = dq.poll();
                String[] parts = cur.split(":");
                if (parts.length != 3) continue;
                String world = parts[0];
                int x = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                String[] neighbors = new String[] {
                    keyOf(world, x + 1, z),
                    keyOf(world, x - 1, z),
                    keyOf(world, x, z + 1),
                    keyOf(world, x, z - 1)
                };
                for (String nb : neighbors) {
                    if (keys.contains(nb) && visited.add(nb)) {
                        dq.add(nb);
                    }
                }
            }
        }
        return components;
    }

    public boolean isAdjacentToNation(Nation nation, Chunk chunk) {
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        Set<String> keys = getNationClaimKeys(nation);
        return keys.contains(keyOf(world, x + 1, z)) ||
               keys.contains(keyOf(world, x - 1, z)) ||
               keys.contains(keyOf(world, x, z + 1)) ||
               keys.contains(keyOf(world, x, z - 1));
    }

    public int getAllowedTerritories(org.bukkit.configuration.file.FileConfiguration config, Nation nation) {
        int base = config.getInt("nation.territory.base_slots", 1);
        return base + Math.max(0, nation.getExtraTerritorySlots());
    }

    public double getNextTerritoryCost(org.bukkit.configuration.file.FileConfiguration config, Nation nation) {
        java.util.List<Double> costs = config.getDoubleList("nation.territory.expansion_costs");
        if (costs == null || costs.isEmpty()) return 0.0;
        int idx = nation.getExtraTerritorySlots();
        if (idx < 0) idx = 0;
        if (idx >= costs.size()) return costs.get(costs.size() - 1);
        return costs.get(idx);
    }

    // ===== Bank helpers =====

    public double getMemberDeposit(Nation nation, java.util.UUID playerId) {
        return nation.getMemberDeposits().getOrDefault(playerId.toString(), 0.0);
    }

    public void deposit(Nation nation, java.util.UUID playerId, double amount) {
        nation.setBankBalance(nation.getBankBalance() + amount);
        nation.getMemberDeposits().put(playerId.toString(), getMemberDeposit(nation, playerId) + amount);
    }

    public boolean withdraw(Nation nation, java.util.UUID playerId, double amount) {
        double allowed = getMemberDeposit(nation, playerId);
        if (amount > allowed) return false;
        if (amount > nation.getBankBalance()) return false;
        nation.setBankBalance(nation.getBankBalance() - amount);
        nation.getMemberDeposits().put(playerId.toString(), allowed - amount);
        return true;
    }

    public void spendFromMembers(Nation nation, double amount) {
        // Prefer splitting across actual account holders; fallback to ranked members if no accounts yet
        java.util.Set<String> accounts = new java.util.HashSet<>(nation.getMemberDeposits().keySet());
        if (accounts.isEmpty()) accounts = new java.util.HashSet<>(nation.getMemberRanks().keySet());
        if (accounts.isEmpty()) return;
        double share = amount / accounts.size();
        for (String uuidStr : accounts) {
            double current = nation.getMemberDeposits().getOrDefault(uuidStr, 0.0);
            nation.getMemberDeposits().put(uuidStr, current - share);
        }
        nation.setBankBalance(nation.getBankBalance() - amount);
    }

    public int collectTaxesPerMember(Nation nation, double perMemberAmount) {
        java.util.Set<String> members = new java.util.HashSet<>(nation.getMemberRanks().keySet());
        // Include all residents of towns in the nation as taxable members
        for (String townId : nation.getTownIds()) {
            com.tntowns.model.Town t = dataStore.getTowns().get(townId);
            if (t != null) members.addAll(t.getResidentUuids());
        }
        if (members.isEmpty()) return 0;
        for (String uuidStr : members) {
            double current = nation.getMemberDeposits().getOrDefault(uuidStr, 0.0);
            nation.getMemberDeposits().put(uuidStr, current - perMemberAmount);
            nation.setBankBalance(nation.getBankBalance() + perMemberAmount);
        }
        return members.size();
    }

    public Optional<Nation> findNationOfMember(java.util.UUID playerId) {
        String key = playerId.toString();
        for (Nation n : dataStore.getNations().values()) {
            if (n.getMemberRanks().containsKey(key)) return Optional.of(n);
        }
        return Optional.empty();
    }

    public boolean isAdminOrKingTownMayor(Nation nation, com.tntowns.model.Town playerTown, java.util.UUID playerId) {
        if (nation == null) return false;
        if (playerTown != null && nation.getKingTownId() != null && nation.getKingTownId().equalsIgnoreCase(playerTown.getId())) {
            // Mayor of the king town is implicitly admin
            com.tntowns.model.Resident r = dataStore.getResidents().get(playerId);
            if (r != null && playerTown.getMayorUuid() != null && playerTown.getMayorUuid().equalsIgnoreCase(playerId.toString())) return true;
        }
        return nation.getAdminUuids().contains(playerId.toString());
    }

    public void grantAdmin(Nation nation, java.util.UUID playerId) {
        nation.getAdminUuids().add(playerId.toString());
    }

    public void revokeAdmin(Nation nation, java.util.UUID playerId) {
        nation.getAdminUuids().remove(playerId.toString());
    }
}



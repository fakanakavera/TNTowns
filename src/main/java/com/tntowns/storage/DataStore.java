package com.tntowns.storage;

import com.tntowns.model.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataStore {

    private final File residentsFile;
    private final File townsFile;
    private final File nationsFile;
    private final File plotsFile;
    private final File metaFile;
    private final File marketsFile;

    private Map<UUID, Resident> residents = new HashMap<>();
    private Map<String, Town> towns = new HashMap<>();
    private Map<String, Nation> nations = new HashMap<>();
    private Map<String, Plot> plots = new HashMap<>(); // key = claimKey
    private Map<String, com.tntowns.model.NationMarket> markets = new HashMap<>(); // nationId -> market
    private int nextTownId = 1;
    private int nextNationId = 1;

    public DataStore(File dataFolder) {
        this.residentsFile = new File(dataFolder, "residents.yml");
        this.townsFile = new File(dataFolder, "towns.yml");
        this.nationsFile = new File(dataFolder, "nations.yml");
        this.plotsFile = new File(dataFolder, "plots.yml");
        this.metaFile = new File(dataFolder, "meta.yml");
        this.marketsFile = new File(dataFolder, "markets.yml");
    }

    public Map<UUID, Resident> getResidents() {
        return residents;
    }

    public Map<String, Town> getTowns() {
        return towns;
    }

    public Map<String, Nation> getNations() {
        return nations;
    }

    public Map<String, Plot> getPlots() {
        return plots;
    }

    public Map<String, com.tntowns.model.NationMarket> getMarkets() {
        return markets;
    }

    public void loadAll() throws IOException {
        loadMeta();
        loadResidents();
        loadTowns();
        loadNations();
        loadPlots();
        loadMarkets();
    }

    public void saveAll() throws IOException {
        saveResidents();
        saveTowns();
        saveNations();
        savePlots();
        saveMarkets();
        saveMeta();
    }

    private void loadResidents() throws IOException {
        residents.clear();
        if (!residentsFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(residentsFile);
        for (String key : cfg.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            Resident r = new Resident();
            r.setUuid(uuid);
            r.setName(cfg.getString(key + ".name", uuid.toString()));
            String role = cfg.getString(key + ".role", "MEMBER");
            try {
                r.setRole(com.tntowns.model.ResidentRole.valueOf(role));
            } catch (IllegalArgumentException ex) {
                r.setRole(com.tntowns.model.ResidentRole.MEMBER);
            }
            r.setTownId(cfg.getString(key + ".townId", null));
            String channel = cfg.getString(key + ".chat", ChatChannel.GLOBAL.name());
            try { r.setChatChannel(ChatChannel.valueOf(channel)); } catch (Exception ignored) {}
            r.setJailed(cfg.getBoolean(key + ".jailed", false));
            residents.put(uuid, r);
        }
    }

    private void saveResidents() throws IOException {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Resident> e : residents.entrySet()) {
            String base = e.getKey().toString();
            Resident r = e.getValue();
            cfg.set(base + ".name", r.getName());
            cfg.set(base + ".role", r.getRole().name());
            cfg.set(base + ".townId", r.getTownId());
            cfg.set(base + ".chat", r.getChatChannel().name());
            cfg.set(base + ".jailed", r.isJailed());
        }
        cfg.save(residentsFile);
    }

    private void loadTowns() throws IOException {
        towns.clear();
        if (!townsFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(townsFile);
        for (String id : cfg.getKeys(false)) {
            Town t = new Town();
            t.setId(id);
            t.setName(cfg.getString(id + ".name", id));
            t.setMayorUuid(cfg.getString(id + ".mayorUuid"));
            t.setNationId(cfg.getString(id + ".nationId", null));
            t.setBankBalance(cfg.getDouble(id + ".bankBalance", 0.0));
            t.setTaxPerResident(cfg.getDouble(id + ".taxPerResident", 0.0));
            t.setPvpEnabled(cfg.getBoolean(id + ".pvpEnabled", false));
            t.setResidentUuids(new java.util.HashSet<>(cfg.getStringList(id + ".residentUuids")));
            t.setClaimKeys(new java.util.HashSet<>(cfg.getStringList(id + ".claimKeys")));
            t.setAllyTownIds(new java.util.HashSet<>(cfg.getStringList(id + ".allyTownIds")));
            t.setEnemyTownIds(new java.util.HashSet<>(cfg.getStringList(id + ".enemyTownIds")));
            t.setOutlawUuids(new java.util.HashSet<>(cfg.getStringList(id + ".outlawUuids")));
            t.setJailWorld(cfg.getString(id + ".jail.world", null));
            t.setJailX(cfg.getInt(id + ".jail.x", 0));
            t.setJailY(cfg.getInt(id + ".jail.y", 0));
            t.setJailZ(cfg.getInt(id + ".jail.z", 0));
            towns.put(id, t);
        }
    }

    private void saveTowns() throws IOException {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Town> e : towns.entrySet()) {
            String id = e.getKey();
            Town t = e.getValue();
            cfg.set(id + ".name", t.getName());
            cfg.set(id + ".mayorUuid", t.getMayorUuid());
            cfg.set(id + ".nationId", t.getNationId());
            cfg.set(id + ".bankBalance", t.getBankBalance());
            cfg.set(id + ".taxPerResident", t.getTaxPerResident());
            cfg.set(id + ".pvpEnabled", t.isPvpEnabled());
            cfg.set(id + ".residentUuids", new java.util.ArrayList<>(t.getResidentUuids()));
            cfg.set(id + ".claimKeys", new java.util.ArrayList<>(t.getClaimKeys()));
            cfg.set(id + ".allyTownIds", new java.util.ArrayList<>(t.getAllyTownIds()));
            cfg.set(id + ".enemyTownIds", new java.util.ArrayList<>(t.getEnemyTownIds()));
            cfg.set(id + ".outlawUuids", new java.util.ArrayList<>(t.getOutlawUuids()));
            cfg.set(id + ".jail.world", t.getJailWorld());
            cfg.set(id + ".jail.x", t.getJailX());
            cfg.set(id + ".jail.y", t.getJailY());
            cfg.set(id + ".jail.z", t.getJailZ());
        }
        cfg.save(townsFile);
    }

    private void loadNations() throws IOException {
        nations.clear();
        if (!nationsFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(nationsFile);
        for (String id : cfg.getKeys(false)) {
            Nation n = new Nation();
            n.setId(id);
            n.setName(cfg.getString(id + ".name", id));
            n.setKingTownId(cfg.getString(id + ".kingTownId"));
            n.setBankBalance(cfg.getDouble(id + ".bankBalance", 0.0));
            n.setTownIds(new java.util.HashSet<>(cfg.getStringList(id + ".townIds")));
            n.setAllyNationIds(new java.util.HashSet<>(cfg.getStringList(id + ".allyNationIds")));
            n.setEnemyNationIds(new java.util.HashSet<>(cfg.getStringList(id + ".enemyNationIds")));
            n.setExtraTerritorySlots(cfg.getInt(id + ".extraTerritorySlots", 0));
            n.setPerMemberTax(cfg.getDouble(id + ".perMemberTax", 0.0));
            n.setAdminUuids(new java.util.HashSet<>(cfg.getStringList(id + ".adminUuids")));
            java.util.Map<String, Object> deposits = cfg.getConfigurationSection(id + ".memberDeposits") != null ? cfg.getConfigurationSection(id + ".memberDeposits").getValues(false) : java.util.Collections.emptyMap();
            java.util.Map<String, Double> mapped = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Object> en : deposits.entrySet()) {
                if (en.getValue() instanceof Number) mapped.put(en.getKey(), ((Number) en.getValue()).doubleValue());
            }
            n.setMemberDeposits(mapped);
            java.util.Map<String, Object> ranks = cfg.getConfigurationSection(id + ".memberRanks") != null ? cfg.getConfigurationSection(id + ".memberRanks").getValues(false) : java.util.Collections.emptyMap();
            java.util.Map<String, String> rankMap = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Object> en : ranks.entrySet()) {
                rankMap.put(en.getKey(), String.valueOf(en.getValue()));
            }
            n.setMemberRanks(rankMap);
            nations.put(id, n);
        }
    }

    private void saveNations() throws IOException {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Nation> e : nations.entrySet()) {
            String id = e.getKey();
            Nation n = e.getValue();
            cfg.set(id + ".name", n.getName());
            cfg.set(id + ".kingTownId", n.getKingTownId());
            cfg.set(id + ".bankBalance", n.getBankBalance());
            cfg.set(id + ".townIds", new java.util.ArrayList<>(n.getTownIds()));
            cfg.set(id + ".allyNationIds", new java.util.ArrayList<>(n.getAllyNationIds()));
            cfg.set(id + ".enemyNationIds", new java.util.ArrayList<>(n.getEnemyNationIds()));
            cfg.set(id + ".extraTerritorySlots", n.getExtraTerritorySlots());
            cfg.set(id + ".perMemberTax", n.getPerMemberTax());
            cfg.set(id + ".adminUuids", new java.util.ArrayList<>(n.getAdminUuids()));
            for (java.util.Map.Entry<String, Double> en : n.getMemberDeposits().entrySet()) {
                cfg.set(id + ".memberDeposits." + en.getKey(), en.getValue());
            }
            for (java.util.Map.Entry<String, String> en : n.getMemberRanks().entrySet()) {
                cfg.set(id + ".memberRanks." + en.getKey(), en.getValue());
            }
        }
        cfg.save(nationsFile);
    }

    private void loadPlots() throws IOException {
        plots.clear();
        if (!plotsFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(plotsFile);
        for (String key : cfg.getKeys(false)) {
            Plot p = new Plot();
            p.setClaimKey(key);
            p.setOwnerUuid(cfg.getString(key + ".ownerUuid", null));
            p.setForSale(cfg.getBoolean(key + ".forSale", false));
            p.setPrice(cfg.getDouble(key + ".price", 0.0));
            plots.put(key, p);
        }
    }

    private void savePlots() throws IOException {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Plot> e : plots.entrySet()) {
            String key = e.getKey();
            Plot p = e.getValue();
            cfg.set(key + ".ownerUuid", p.getOwnerUuid());
            cfg.set(key + ".forSale", p.isForSale());
            cfg.set(key + ".price", p.getPrice());
        }
        cfg.save(plotsFile);
    }

    private void loadMarkets() throws IOException {
        markets.clear();
        if (!marketsFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(marketsFile);
        for (String nationId : cfg.getKeys(false)) {
            com.tntowns.model.NationMarket m = new com.tntowns.model.NationMarket();
            m.setNationId(nationId);
            m.setCurrencyReserve(cfg.getDouble(nationId + ".currencyReserve", 0.0));
            m.setShareReserve(cfg.getDouble(nationId + ".shareReserve", 0.0));
            m.setTotalSharesIssued(cfg.getDouble(nationId + ".totalSharesIssued", 0.0));
            m.setFeeBps(cfg.getInt(nationId + ".feeBps", 30));
            java.util.Map<String, Object> holders = cfg.getConfigurationSection(nationId + ".holders") != null ? cfg.getConfigurationSection(nationId + ".holders").getValues(false) : java.util.Collections.emptyMap();
            java.util.Map<String, Double> mapped = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Object> en : holders.entrySet()) {
                if (en.getValue() instanceof Number) mapped.put(en.getKey(), ((Number) en.getValue()).doubleValue());
            }
            m.setHolderShares(mapped);
            markets.put(nationId.toLowerCase(), m);
        }
    }

    private void saveMarkets() throws IOException {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, com.tntowns.model.NationMarket> e : markets.entrySet()) {
            String id = e.getKey();
            com.tntowns.model.NationMarket m = e.getValue();
            cfg.set(id + ".currencyReserve", m.getCurrencyReserve());
            cfg.set(id + ".shareReserve", m.getShareReserve());
            cfg.set(id + ".totalSharesIssued", m.getTotalSharesIssued());
            cfg.set(id + ".feeBps", m.getFeeBps());
            for (java.util.Map.Entry<String, Double> en : m.getHolderShares().entrySet()) {
                cfg.set(id + ".holders." + en.getKey(), en.getValue());
            }
        }
        cfg.save(marketsFile);
    }

    private void loadMeta() throws IOException {
        if (!metaFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(metaFile);
        this.nextTownId = cfg.getInt("nextTownId", 1);
        this.nextNationId = cfg.getInt("nextNationId", 1);
    }

    private void saveMeta() throws IOException {
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("nextTownId", this.nextTownId);
        cfg.set("nextNationId", this.nextNationId);
        cfg.save(metaFile);
    }

    public synchronized String generateNextTownId() {
        String id = String.valueOf(nextTownId);
        nextTownId++;
        try { saveMeta(); } catch (IOException ignored) {}
        return id;
    }

    public synchronized String generateNextNationId() {
        String id = String.valueOf(nextNationId);
        nextNationId++;
        try { saveMeta(); } catch (IOException ignored) {}
        return id;
    }
}



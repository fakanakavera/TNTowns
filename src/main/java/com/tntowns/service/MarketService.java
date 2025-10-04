package com.tntowns.service;

import com.tntowns.model.Nation;
import com.tntowns.model.NationMarket;
import com.tntowns.storage.DataStore;

import java.util.Optional;

public class MarketService {

    private final DataStore dataStore;

    public MarketService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public NationMarket getOrCreate(String nationId) {
        return dataStore.getMarkets().computeIfAbsent(nationId.toLowerCase(), id -> {
            NationMarket m = new NationMarket();
            m.setNationId(nationId.toLowerCase());
            m.setCurrencyReserve(0.0);
            m.setShareReserve(0.0);
            m.setTotalSharesIssued(0.0);
            m.setMarketRank(1);
            return m;
        });
    }

    public Optional<NationMarket> get(String nationId) {
        return Optional.ofNullable(dataStore.getMarkets().get(nationId.toLowerCase()));
    }

    // Constant product pricing helpers (x*y=k) with fee on input
    public double getSpotPrice(NationMarket m) {
        if (m.getShareReserve() <= 0 || m.getCurrencyReserve() <= 0) return 0.0;
        return m.getCurrencyReserve() / m.getShareReserve();
    }

    public double quoteBuy(NationMarket m, double currencyIn) {
        if (currencyIn <= 0) return 0.0;
        double feeMultiplier = 1.0 - (m.getFeeBps() / 10000.0);
        double effectiveIn = currencyIn * feeMultiplier;
        double x = m.getCurrencyReserve();
        double y = m.getShareReserve();
        if (x <= 0 || y <= 0) {
            // No liquidity yet; 1:1 bootstrap
            return effectiveIn <= 0 ? 0.0 : effectiveIn; 
        }
        double k = x * y;
        double newX = x + effectiveIn;
        double newY = k / newX;
        return y - newY; // shares out
    }

    public double quoteSell(NationMarket m, double sharesIn) {
        if (sharesIn <= 0) return 0.0;
        double x = m.getCurrencyReserve();
        double y = m.getShareReserve();
        if (x <= 0 || y <= 0) return 0.0;
        double k = x * y;
        double newY = y + sharesIn;
        double newX = k / newY;
        double currencyOut = x - newX;
        double fee = currencyOut * (m.getFeeBps() / 10000.0);
        return Math.max(0.0, currencyOut - fee);
    }

    public boolean buy(Nation nation, NationMarket m, java.util.UUID buyer, double currencyIn, double minSharesOut) {
        if (currencyIn <= 0) return false;
        double sharesOut = quoteBuy(m, currencyIn);
        if (sharesOut <= 0 || sharesOut < minSharesOut) return false;
        // Move currency from player to nation bank, and shares from pool to holder
        com.tntowns.TNtownsPlugin.getInstance().getEconomy().withdrawPlayer(com.tntowns.TNtownsPlugin.getInstance().getServer().getPlayer(buyer), currencyIn);
        nation.setBankBalance(nation.getBankBalance() + currencyIn);
        m.setCurrencyReserve(m.getCurrencyReserve() + currencyIn * (1.0 - (m.getFeeBps() / 10000.0)));
        m.setShareReserve(Math.max(0.0, m.getShareReserve() - sharesOut));
        m.getHolderShares().put(buyer.toString(), m.getHolderShares().getOrDefault(buyer.toString(), 0.0) + sharesOut);
        return true;
    }

    public boolean sell(Nation nation, NationMarket m, java.util.UUID seller, double sharesIn, double minCurrencyOut) {
        if (sharesIn <= 0) return false;
        double current = m.getHolderShares().getOrDefault(seller.toString(), 0.0);
        if (current < sharesIn) return false;
        double currencyOut = quoteSell(m, sharesIn);
        if (currencyOut <= 0 || currencyOut < minCurrencyOut) return false;
        // Move shares from holder to pool, currency from nation bank to player
        if (nation.getBankBalance() < currencyOut) return false; // treasury must have funds
        nation.setBankBalance(nation.getBankBalance() - currencyOut);
        com.tntowns.TNtownsPlugin.getInstance().getEconomy().depositPlayer(com.tntowns.TNtownsPlugin.getInstance().getServer().getPlayer(seller), currencyOut);
        m.setCurrencyReserve(Math.max(0.0, m.getCurrencyReserve() - currencyOut));
        m.setShareReserve(m.getShareReserve() + sharesIn);
        m.getHolderShares().put(seller.toString(), current - sharesIn);
        return true;
    }

    public void seedIpo(Nation nation, NationMarket m, double currencySeed, double shareSeed) {
        // Nation commits seed currency and mints initial shares to the pool
        if (currencySeed < 0 || shareSeed < 0) return;
        // Treasury contributes currency into pool
        if (nation.getBankBalance() < currencySeed) throw new IllegalStateException("Insufficient nation bank for IPO seed");
        nation.setBankBalance(nation.getBankBalance() - currencySeed);
        m.setCurrencyReserve(m.getCurrencyReserve() + currencySeed);
        m.setShareReserve(m.getShareReserve() + shareSeed);
        m.setTotalSharesIssued(m.getTotalSharesIssued() + shareSeed);
    }

    public boolean canMintMoreShares(org.bukkit.configuration.file.FileConfiguration cfg, NationMarket m, double additional) {
        double cap = getShareCapForRank(cfg, m.getMarketRank());
        return (m.getTotalSharesIssued() + Math.max(0.0, additional)) <= cap;
    }

    public double getShareCapForRank(org.bukkit.configuration.file.FileConfiguration cfg, int rank) {
        java.util.List<Double> caps = cfg.getDoubleList("nation.market.rank_share_caps");
        if (caps == null || caps.isEmpty()) return Double.MAX_VALUE;
        int idx = Math.max(1, rank) - 1;
        if (idx >= caps.size()) return caps.get(caps.size() - 1);
        return caps.get(idx);
    }

    public double getRankUpCost(org.bukkit.configuration.file.FileConfiguration cfg, int nextRank) {
        java.util.List<Double> costs = cfg.getDoubleList("nation.market.rank_up_costs");
        if (costs == null || costs.isEmpty()) return 0.0;
        int idx = Math.max(1, nextRank) - 1;
        if (idx >= costs.size()) return costs.get(costs.size() - 1);
        return costs.get(idx);
    }

    public boolean rankUp(Nation nation, NationMarket m, org.bukkit.configuration.file.FileConfiguration cfg) {
        int next = m.getMarketRank() + 1;
        double cost = getRankUpCost(cfg, next);
        if (nation.getBankBalance() < cost) return false;
        nation.setBankBalance(nation.getBankBalance() - cost);
        m.setMarketRank(next);
        return true;
    }

    public boolean mint(NationMarket m, double shares, org.bukkit.configuration.file.FileConfiguration cfg) {
        if (shares <= 0) return false;
        if (!canMintMoreShares(cfg, m, shares)) return false;
        m.setShareReserve(m.getShareReserve() + shares);
        m.setTotalSharesIssued(m.getTotalSharesIssued() + shares);
        return true;
    }
}



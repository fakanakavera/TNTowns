package com.tntowns.model;

import java.util.HashMap;
import java.util.Map;

public class NationMarket {

    private String nationId;
    private double currencyReserve; // AMM currency reserve (Vault money)
    private double shareReserve;    // AMM share reserve (pool-held shares)
    private double totalSharesIssued; // Cumulative minted shares
    private int feeBps = 30; // 0.30% default trading fee

    private Map<String, Double> holderShares = new HashMap<>(); // uuid -> shares

    public String getNationId() {
        return nationId;
    }

    public void setNationId(String nationId) {
        this.nationId = nationId;
    }

    public double getCurrencyReserve() {
        return currencyReserve;
    }

    public void setCurrencyReserve(double currencyReserve) {
        this.currencyReserve = currencyReserve;
    }

    public double getShareReserve() {
        return shareReserve;
    }

    public void setShareReserve(double shareReserve) {
        this.shareReserve = shareReserve;
    }

    public double getTotalSharesIssued() {
        return totalSharesIssued;
    }

    public void setTotalSharesIssued(double totalSharesIssued) {
        this.totalSharesIssued = totalSharesIssued;
    }

    public int getFeeBps() {
        return feeBps;
    }

    public void setFeeBps(int feeBps) {
        this.feeBps = feeBps;
    }

    public Map<String, Double> getHolderShares() {
        return holderShares;
    }

    public void setHolderShares(Map<String, Double> holderShares) {
        this.holderShares = holderShares;
    }
}



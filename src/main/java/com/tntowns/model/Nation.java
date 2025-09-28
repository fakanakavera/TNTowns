package com.tntowns.model;

import java.util.HashSet;
import java.util.Set;

public class Nation {
    private String id; // unique string id
    private String name;
    private String kingTownId; // the capital/mains town id
    private Set<String> townIds = new HashSet<>();
    private Set<String> allyNationIds = new HashSet<>();
    private Set<String> enemyNationIds = new HashSet<>();
    private double bankBalance;
    private int extraTerritorySlots;
    private java.util.Map<String, Double> memberDeposits = new java.util.HashMap<>();
    private java.util.Map<String, String> memberRanks = new java.util.HashMap<>(); // uuid -> NationRank name
    private double perMemberTax;
    private java.util.Set<String> adminUuids = new java.util.HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKingTownId() {
        return kingTownId;
    }

    public void setKingTownId(String kingTownId) {
        this.kingTownId = kingTownId;
    }

    public Set<String> getTownIds() {
        return townIds;
    }

    public void setTownIds(Set<String> townIds) {
        this.townIds = townIds;
    }

    public Set<String> getAllyNationIds() {
        return allyNationIds;
    }

    public void setAllyNationIds(Set<String> allyNationIds) {
        this.allyNationIds = allyNationIds;
    }

    public Set<String> getEnemyNationIds() {
        return enemyNationIds;
    }

    public void setEnemyNationIds(Set<String> enemyNationIds) {
        this.enemyNationIds = enemyNationIds;
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }

    public int getExtraTerritorySlots() {
        return extraTerritorySlots;
    }

    public void setExtraTerritorySlots(int extraTerritorySlots) {
        this.extraTerritorySlots = extraTerritorySlots;
    }

    public java.util.Map<String, Double> getMemberDeposits() {
        return memberDeposits;
    }

    public void setMemberDeposits(java.util.Map<String, Double> memberDeposits) {
        this.memberDeposits = memberDeposits;
    }

    public java.util.Map<String, String> getMemberRanks() {
        return memberRanks;
    }

    public void setMemberRanks(java.util.Map<String, String> memberRanks) {
        this.memberRanks = memberRanks;
    }

    public double getPerMemberTax() {
        return perMemberTax;
    }

    public void setPerMemberTax(double perMemberTax) {
        this.perMemberTax = perMemberTax;
    }

    public java.util.Set<String> getAdminUuids() {
        return adminUuids;
    }

    public void setAdminUuids(java.util.Set<String> adminUuids) {
        this.adminUuids = adminUuids;
    }
}



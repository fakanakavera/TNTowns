package com.tntowns.model;

import java.util.HashSet;
import java.util.Set;

public class Town {
    private String id; // unique string id
    private String name;
    private String mayorUuid; // UUID as string
    private Set<String> residentUuids = new HashSet<>();
    private Set<String> claimKeys = new HashSet<>();
    private String nationId; // nullable
    private double bankBalance;
    private double taxPerResident;
    private boolean pvpEnabled;
    private Set<String> allyTownIds = new HashSet<>();
    private Set<String> enemyTownIds = new HashSet<>();
    private Set<String> outlawUuids = new HashSet<>();
    private String jailWorld; // nullable
    private int jailX;
    private int jailY;
    private int jailZ;

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

    public String getMayorUuid() {
        return mayorUuid;
    }

    public void setMayorUuid(String mayorUuid) {
        this.mayorUuid = mayorUuid;
    }

    public Set<String> getResidentUuids() {
        return residentUuids;
    }

    public void setResidentUuids(Set<String> residentUuids) {
        this.residentUuids = residentUuids;
    }

    public Set<String> getClaimKeys() {
        return claimKeys;
    }

    public void setClaimKeys(Set<String> claimKeys) {
        this.claimKeys = claimKeys;
    }

    public String getNationId() {
        return nationId;
    }

    public void setNationId(String nationId) {
        this.nationId = nationId;
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }

    public double getTaxPerResident() {
        return taxPerResident;
    }

    public void setTaxPerResident(double taxPerResident) {
        this.taxPerResident = taxPerResident;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public Set<String> getAllyTownIds() {
        return allyTownIds;
    }

    public void setAllyTownIds(Set<String> allyTownIds) {
        this.allyTownIds = allyTownIds;
    }

    public Set<String> getEnemyTownIds() {
        return enemyTownIds;
    }

    public void setEnemyTownIds(Set<String> enemyTownIds) {
        this.enemyTownIds = enemyTownIds;
    }

    public Set<String> getOutlawUuids() {
        return outlawUuids;
    }

    public void setOutlawUuids(Set<String> outlawUuids) {
        this.outlawUuids = outlawUuids;
    }

    public String getJailWorld() {
        return jailWorld;
    }

    public void setJailWorld(String jailWorld) {
        this.jailWorld = jailWorld;
    }

    public int getJailX() {
        return jailX;
    }

    public void setJailX(int jailX) {
        this.jailX = jailX;
    }

    public int getJailY() {
        return jailY;
    }

    public void setJailY(int jailY) {
        this.jailY = jailY;
    }

    public int getJailZ() {
        return jailZ;
    }

    public void setJailZ(int jailZ) {
        this.jailZ = jailZ;
    }
}



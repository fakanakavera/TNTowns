package com.tntowns.model;

public class Plot {
    private String claimKey; // world:x:z of parent chunk
    private String ownerUuid; // nullable for unowned
    private boolean forSale;
    private double price;

    public String getClaimKey() {
        return claimKey;
    }

    public void setClaimKey(String claimKey) {
        this.claimKey = claimKey;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(String ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public boolean isForSale() {
        return forSale;
    }

    public void setForSale(boolean forSale) {
        this.forSale = forSale;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}



package com.tntowns.model;

import java.util.Objects;

public class Claim {
    private String world;
    private int chunkX;
    private int chunkZ;

    public Claim() {
    }

    public Claim(String world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int getChunkX() {
        return chunkX;
    }

    public void setChunkX(int chunkX) {
        this.chunkX = chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public void setChunkZ(int chunkZ) {
        this.chunkZ = chunkZ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Claim claim = (Claim) o;
        return chunkX == claim.chunkX && chunkZ == claim.chunkZ && Objects.equals(world, claim.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, chunkX, chunkZ);
    }

    public String toKey() {
        return world + ":" + chunkX + ":" + chunkZ;
    }
}



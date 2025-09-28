package com.tntowns.service;

import com.tntowns.model.Plot;
import com.tntowns.storage.DataStore;
import org.bukkit.Chunk;

import java.util.Optional;

public class PlotService {

    private final DataStore dataStore;

    public PlotService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public String chunkKey(Chunk c) {
        return c.getWorld().getName() + ":" + c.getX() + ":" + c.getZ();
    }

    public Optional<Plot> getPlot(Chunk c) {
        return Optional.ofNullable(dataStore.getPlots().get(chunkKey(c)));
    }

    public Plot getOrCreate(Chunk c) {
        return dataStore.getPlots().computeIfAbsent(chunkKey(c), k -> {
            Plot p = new Plot();
            p.setClaimKey(k);
            return p;
        });
    }

    public boolean isOwner(Plot plot, String uuid) {
        return plot.getOwnerUuid() != null && plot.getOwnerUuid().equalsIgnoreCase(uuid);
    }
}



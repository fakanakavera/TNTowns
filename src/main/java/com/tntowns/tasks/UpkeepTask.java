package com.tntowns.tasks;

import com.tntowns.TNtownsPlugin;
import com.tntowns.model.Town;

public class UpkeepTask implements Runnable {

    private final TNtownsPlugin plugin;

    public UpkeepTask(TNtownsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        double perResident = plugin.getConfig().getDouble("taxes.upkeep_per_resident_daily", 10.0);
        for (Town town : new java.util.ArrayList<>(plugin.getTownService().getDataStore().getTowns().values())) {
            double total = perResident * town.getResidentUuids().size();
            double before = town.getBankBalance();
            if (before >= total) {
                town.setBankBalance(before - total);
            } else {
                // Disband on debt
                plugin.getLogger().warning("Disbanding town due to unpaid upkeep: " + town.getName());
                plugin.getTownService().disbandTown(town);
            }
        }
    }
}



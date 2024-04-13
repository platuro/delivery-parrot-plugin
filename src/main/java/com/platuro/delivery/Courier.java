package com.platuro.delivery;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Courier {
    private Villager villager;
    private Plugin plugin;

    // Create a list of the next locations to visit
    private Location[] locations;
    private BukkitRunnable moveTask;

    public Courier(Plugin plugin) {
        this.plugin = plugin;
    }

    public void spawnVillager(Location spawnLocation) {
        villager = (Villager) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.VILLAGER);
        villager.customName(Component.text("Courier"));
        villager.setCustomNameVisible(true);
        villager.setAdult(); // Ensure the villager is an adult.
        villager.setAI(true); // Enable AI to allow walking.
        InitLocation();
    }

    public void InitLocation() {
        // Get all the locations of the chests
        locations = new Location[Deliveryman.chests.addressChests.size() + Deliveryman.chests.senderChests.size() + 1];
        int i = 0;
        for (Location location : Deliveryman.chests.senderChests.keySet()) {
            locations[i] = location;
            i++;
        }
        for (Location location : Deliveryman.chests.addressChests.keySet()) {
            locations[i] = location;
            i++;
        }
        // Add the spawn location to the end of the list
        locations[i] = villager.getLocation();
        moveVillagerToNextLocation();
    }

    public void moveVillagerToNextLocation() {
        moveVillager(locations[0]);
        new BukkitRunnable() {
            int i = 0;
            public void run() {
                if (villager != null && !villager.isDead()) {
                    // Move the villager to the next location if he visited the current one
                    if (villager.getLocation().distance(locations[i]) < 2.0) {
                        // Open the chest if the villager is near it
                        if (locations[i].getBlock().getType().name().contains("CHEST")) {
                            Deliveryman.chests.openChest(locations[i]);
                        }


                        i++;
                        if (i >= locations.length) {
                            i = 0;
                            despawnVillager();
                            this.cancel();
                        }else {
                            moveVillager(locations[i]);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 3); // Move every 3 seconds.
    }

    public void moveVillager(Location newLocation) {
        if (moveTask != null) {
            moveTask.cancel();
        }
        moveTask = new BukkitRunnable() {
            public void run() {
                if (villager != null && !villager.isDead() && villager.getWorld().equals(newLocation.getWorld())) {
                    villager.getPathfinder().moveTo(newLocation);
                }
            }
        };
        moveTask.runTaskTimer(plugin, 0L, 20L); // Move every 1 second.
    }

    public void despawnVillager() {
        if (villager != null && !villager.isDead()) {
            villager.remove();
        }
        if (moveTask != null) {
            moveTask.cancel();
        }
        locations = null;
    }
}

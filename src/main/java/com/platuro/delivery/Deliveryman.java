package com.platuro.delivery;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Deliveryman extends JavaPlugin {
    public static Chests chests;
    public Courier courier;

    // sun rise checker
    BukkitRunnable sunriseChecker;

    @Override
    public void onEnable() {
        // Plugin startup logic
        chests = new Chests(this);
        chests.startSignCheckTask();
        startSunriseChecker();
    }

    private void startSunriseChecker() {
        sunriseChecker = new BukkitRunnable() {
            private boolean isDay = false; // Flag to check if it's currently day to avoid multiple triggers

            @Override
            public void run() {
                World world = Bukkit.getServer().getWorlds().get(0); // Get the default world
                long time = world.getTime(); // Get the current time in the world

                // Check if the time is between 0 and 1000 and it was previously night
                if (time >= 0 && time <= 1000 && !isDay) {
                    isDay = true; // Set it to day to avoid re-triggering
                    onSunrise(world); // Trigger the event
                } else if (time > 1000) {
                    isDay = false; // Reset to night once past sunrise time
                }
            }
        };
        sunriseChecker.runTaskTimer(this, 0L, 20L * 60); // Run the task every minute
    }

    private void onSunrise(World world) {
        // Handle the sunrise event, this can be anything you need
        getLogger().info("Sunrise in world " + world.getName());
        // You can trigger any specific events or functions here
        // Create a new Courier instance
        courier = new Courier(this);
        // Spawn at the near the spawn point
        courier.spawnVillager(getServer().getWorld("world").getSpawnLocation().add(0, 1, 0));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        sunriseChecker.cancel(); // Cancel the sunrise checker task
        courier.despawnVillager();
    }
}

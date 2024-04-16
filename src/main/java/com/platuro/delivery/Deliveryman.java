package com.platuro.delivery;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public final class Deliveryman extends JavaPlugin implements Listener {
    public static Chests chests;
    public static Courier courier;

    // sun rise checker
    BukkitRunnable sunriseChecker;
    CourierInventory courierInventory;
    CommandListener commandListener;
    SignListener signListener;
    public static PostOffice postOffice;

    @Override
    public void onEnable() {
        // Plugin startup logic
        chests = new Chests(this, this);
        chests.startSignCheckTask();
        //startSunriseChecker();
        courierInventory = new CourierInventory(this);
        commandListener = new CommandListener(this);
        postOffice = new PostOffice(this);
        postOffice.Init();
        signListener = new SignListener(this);
        this.getCommand("deliveryman").setExecutor(commandListener);
        CreateCourier(Bukkit.getServer().getWorlds().get(0));
    }

    private void startSunriseChecker() {
        sunriseChecker = new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getServer().getWorlds().get(0); // Get the default world
                CreateCourier(world); // Trigger the event
            }
        };
        sunriseChecker.runTaskTimer(this, 0L, 20L * 60); // Run the task every minute
    }

    private void CreateCourier(World world) {
        // You can trigger any specific events or functions here
        // if there is a courier, delete it
        if(courier != null && courier.isAlive())
            return;

        if(courier != null){
            courier.despawnVillager();
        }

        Location spawnLocation = world.getSpawnLocation().add(0, 1, 0);
        // Check if there is a sender chest existing
        if(chests.addressChests.size() > 0) {
            // Get the first sender chest
            for (Location location : chests.addressChests.keySet()) {
                spawnLocation = location;
                break;
            }
        }

        // Create a new Courier instance
        courier = new Courier(this);
        // Spawn at the near the spawn point
        courier.spawnVillager(spawnLocation);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        // Save the chests to the config
        chests.OnDisable();

        //ProtocolManager
        //Check if ProtocolLib is installed
        Plugin protocolLib = getServer().getPluginManager().getPlugin("ProtocolLib");
        if (protocolLib == null || !protocolLib.isEnabled()) {
            getLogger().warning("ProtocolLib is not installed. Disabling the plugin.");
            getServer().getPluginManager().disablePlugin(this);
        }else {
            getLogger().info("ProtocolLib is installed. Disabling the plugin.");
            //ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            //protocolManager.removePacketListeners(this);
        }

        postOffice.OnStop();

        // Disable the listener
        courierInventory.Dispose();
        signListener.Dispose();

        if(sunriseChecker != null)
            sunriseChecker.cancel();

        if(courier != null)
            courier.despawnVillager();
    }
}

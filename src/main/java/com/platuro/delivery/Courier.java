package com.platuro.delivery;

import com.destroystokyo.paper.entity.Pathfinder;
import com.platuro.delivery.Until.ChunkHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class Courier {
    private Parrot villager;
    private Plugin plugin;
    private Chunk currentChunk;
    private Chunk oldChunk;

    Chunk destinationChunk;

    ChunkHandler chunkHandler = new ChunkHandler();

    // Create a list of the next locations to visit
    private List<Location> locations = new ArrayList<>();
    private BukkitRunnable moveTask;

    public Courier(Plugin plugin) {
        this.plugin = plugin;
    }

    public void spawnVillager(Location spawnLocation) {
        // Set Chunk
        currentChunk = spawnLocation.getChunk();
        chunkHandler.loadAdjacentChunks(spawnLocation.getWorld(), currentChunk.getX(), currentChunk.getZ());

        villager = (Parrot) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.PARROT);
        villager.customName(Component.text("Courier"));
        villager.setCustomNameVisible(true);
        villager.setAdult(); // Ensure the villager is an adult.
        villager.setAI(true); // Enable AI to allow walking.
        // The villager will not be able to breed.
        villager.setBreed(false);
        villager.setTamed(true);
        villager.setOwner(null);
        villager.getPathfinder().setCanOpenDoors(true);
        // The villager wont die
        //villager.setInvulnerable(true);
        villager.setVariant(Parrot.Variant.RED);

        InitLocation();
    }

    public void InitLocation() {
        // Get all the locations of the chests
        int i = 0;
        for (Location location : Deliveryman.chests.senderChests.keySet()) {
            if(Deliveryman.chests.hasItemsInChest(location)) {
                locations.add(location);
            }
        }
        for (Location location : Deliveryman.chests.addressChests.keySet()) {
            if(Deliveryman.chests.hasCourierItems()) {
                locations.add(location);
            }
        }
        // Add the spawn location to the end of the list
        locations.add(villager.getLocation());
        moveVillager();
    }

    public void moveVillager() {
        if (moveTask != null) {
            moveTask.cancel();
        }
        moveTask = new BukkitRunnable() {
            public void run() {
                if(locations.isEmpty()) {
                    InitLocation();
                    this.cancel();
                }

                Location currentLocation = locations.get(0);

                if (villager != null && !villager.isDead() && villager.getWorld().equals(currentLocation.getWorld())) {
                    villager.getPathfinder().moveTo(currentLocation, 10.0D);
                    // Print
                    plugin.getLogger().info("Moving to " + currentLocation);
                    // Load the chunk of the villager
                    chunkHandler.loadAdjacentChunks(villager.getWorld(), villager.getLocation().getChunk().getX(), villager.getLocation().getChunk().getZ());

                    // Load the location of the next chest
                    chunkHandler.loadAdjacentChunks(villager.getWorld(), currentLocation.getChunk().getX(), currentLocation.getChunk().getZ());

                    //if it reaches the destination open the chest
                    if (villager.getLocation().distance(currentLocation) < 2.0) {
                        // Open the chest if the villager is near it
                        if (currentLocation.getBlock().getType().name().contains("CHEST")) {
                            Deliveryman.chests.openChest(currentLocation);
                            // check if the next location is the last one
                            locations.remove(0);
                            InitLocation();
                            this.cancel();
                        }
                    }
                }else {
                    despawnVillager();
                    this.cancel();
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
        chunkHandler.unloadChunks();
        locations = null;
    }

    boolean isAlive() {
        return villager != null && !villager.isDead();
    }
}

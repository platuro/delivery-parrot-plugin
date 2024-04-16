package com.platuro.delivery;

import com.destroystokyo.paper.entity.Pathfinder;
import com.platuro.delivery.Until.ChunkHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Villager;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

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
    public List<Location> locations = new ArrayList<>();
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
        villager.setInvulnerable(true);
        villager.getPathfinder().setCanOpenDoors(true);
        // The villager wont die
        //villager.setInvulnerable(true);
        villager.setVariant(Parrot.Variant.RED);
        // Set metadata
        villager.setMetadata("Courier", new FixedMetadataValue(plugin, true));

        //create new location list
        locations = new ArrayList<>();

        InitLocation();
        CheckNextJob();
    }

    public void AddLocation(Location location) {
        //check if the location is already in the list
        if (!locations.contains(location)) {
            locations.add(location);
            moveVillager();
        }
    }

    public void Teleport(Location location) {
        if(locations.size() == 1) {
            if (villager != null && !villager.isDead()) {
                villager.teleport(location);
                // Load the chunk of the villager
                chunkHandler.loadAdjacentChunks(villager.getWorld(), villager.getLocation().getChunk().getX(), villager.getLocation().getChunk().getZ());
            }
        }
    }

    public void InitLocation() {
        // clear the locations
        locations.clear();
        // Get all the locations of the chests
        for (Location location : Deliveryman.chests.senderChests.keySet()) {
            if(Deliveryman.chests.hasItemsInChest(location, Deliveryman.chests.senderChests)) {
                locations.add(location);
            }
        }
        // Check if there are address chests with courier name
        for (Location location : Deliveryman.chests.addressChests.keySet()) {
            if(Deliveryman.chests.hasCourierItems(location)) {
                locations.add(location);
            }
        }
        // Check if there are post offices with items
        for (Location location : Deliveryman.postOffice.postofficeLocations.keySet()) {
            if(Deliveryman.postOffice.hasItemsInPostOffice(location)) {
                //locations.add(location);
            }
        }
        //Print the locations
        plugin.getLogger().info("Locations: " + locations);
        // Sort the locations after the nearest
        locations = SortLocationsAfterNearest();
        moveVillager();
    }

    public List<Location> SortLocationsAfterNearest() {
        // Sort the locations after the nearest
        locations.sort((location1, location2) -> {
            if (villager == null || villager.isDead()) {
                return 0;
            }
            return (int) (villager.getLocation().distance(location1) - villager.getLocation().distance(location2));
        });
        return locations;
    }

    public void CheckNextJob() {
        // check every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                if(locations == null) {
                    locations = new ArrayList<>();
                }
                // print size
                plugin.getLogger().info("Locations size: " + locations.size());
                if(locations.isEmpty()) {
                    Deliveryman.chests.ScanForChests();
                    InitLocation();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 30); // Run every 1 minute

    }

    public void moveVillager() {
        if (moveTask != null) {
            moveTask.cancel();
        }
        moveTask = new BukkitRunnable() {
            public void run() {
                if(locations.isEmpty()) {
                    this.cancel();
                    return;
                }
                Location currentLocation = locations.get(0);

                if (villager != null && !villager.isDead() && villager.getWorld().equals(currentLocation.getWorld())) {
                    villager.getPathfinder().moveTo(currentLocation);
                    VilliagerFlyTo(currentLocation);

                    // Print
                    //plugin.getLogger().info("Moving to " + currentLocation);
                    // Load the chunk of the villager
                    chunkHandler.loadAdjacentChunks(villager.getWorld(), villager.getLocation().getChunk().getX(), villager.getLocation().getChunk().getZ());

                    // Load the location of the next chest
                    chunkHandler.loadAdjacentChunks(villager.getWorld(), currentLocation.getChunk().getX(), currentLocation.getChunk().getZ());

                    //if it reaches the destination open the chest
                    if (villager.getLocation().distance(currentLocation) < 2.0) {
                        // Print it is near the location
                        plugin.getLogger().info("Near the location: " + currentLocation);
                        // Open the chest if the villager is near it
                        if (currentLocation.getBlock().getType().name().contains("CHEST")) {
                            Deliveryman.chests.openChest(currentLocation);
                        }
                        // check if its near a PostOffice
                        if (currentLocation.getBlock().getType().name().contains("VILLAGER")) {
                            //Deliveryman.postOffice.TransferToCourier();
                        }
                        locations.remove(0);
                    }
                }else {
                    despawnVillager();
                    this.cancel();
                }
            }
        };
        moveTask.runTaskTimer(plugin, 0L, 1L); // Move every 1 second.
    }

    Location old_location = null;
    int ticks = 0;
    void VilliagerFlyTo(Location location) {
        if (!villager.isValid() || villager.isDead()) {
            return;
        }
        Location currentLocation = villager.getLocation().clone();
        Location location1 = location.clone();
        float speed = 0.5f;

        // if the distance is less than 2 chunk, return
        if (currentLocation.distance(location) < 16 && currentLocation.getChunk().isLoaded()) {
            speed = 0.1f;
        }

        if (currentLocation.distance(location) >= 16) {
            // Only adjust the height if we are significantly horizontally distant
            float incrementY = 0.3F;  // Define the Y-increment
            int maxY = 255;       // Define the maximum Y to stop increasing
            int significantDistanceX = 10;  // Define what is considered significant horizontal distance

            // Check if we need to increase Y based on X distance
            if (Math.abs(location1.getX() - currentLocation.getX()) > significantDistanceX) {
                // Calculate the new potential Y
                float newY = (float) (currentLocation.getY() + incrementY);
                if (newY < maxY) {
                    currentLocation.setY(newY);
                } else {
                    currentLocation.setY(maxY);  // Ensure we do not exceed the maximum Y
                }
            }
        }

        //Check if its stuck since the last location
        if(old_location != null && old_location.distance(currentLocation) < 1) {
            ticks++;
        }

        if (ticks > 10) {
            Vector direction = currentLocation.toVector().subtract(location1.toVector()).normalize(); // Calculate the direction vector towards location1
            // Move the villager slightly (0.1 blocks) towards the direction of location1
            currentLocation.add(direction.multiply(-0.1));
            currentLocation.setY(currentLocation.getY() + 0.1); // Slightly raise the Y position to avoid sinking into the ground
            // Teleport the villager to the new location
            villager.teleport(currentLocation);
            // Reset ticks after the operation
            ticks = 0;
        }

        if (currentLocation.distance(location) < 1.5) {
            villager.setAI(true); // Re-enable AI upon arrival
            return;
        }

        @NotNull Vector direction = location.toVector().subtract(currentLocation.toVector()).normalize();
        direction.multiply(speed); // Set the speed of the villager's flight
        villager.setVelocity(direction);

        // Set villager's head direction
        float yaw = getYaw(direction);
        float pitch = getPitch(direction);
        currentLocation.add(direction);
        currentLocation.setYaw(yaw);
        currentLocation.setPitch(pitch);
        villager.teleport(currentLocation);

        //villager.teleport(currentLocation);
        old_location = currentLocation.clone();
    }

    public void despawnVillager() {
        if (villager != null && !villager.isDead()) {
            villager.remove();
        }
        if (moveTask != null) {
            moveTask.cancel();
        }
        // clear the locations
        locations.clear();
        chunkHandler.unloadChunks();
        locations = null;
    }

    boolean isAlive() {
        return villager != null && !villager.isDead();
    }

    private float getYaw(Vector direction) {
        double dx = direction.getX();
        double dz = direction.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        return yaw;
    }

    private float getPitch(Vector direction) {
        double dx = direction.getX();
        double dy = direction.getY();
        double dz = direction.getZ();
        double distanceXZ = Math.sqrt(dx*dx + dz*dz);
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, distanceXZ));
        return pitch;
    }
}

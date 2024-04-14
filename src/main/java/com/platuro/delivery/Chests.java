package com.platuro.delivery;

import com.platuro.delivery.Until.InventoryStorageUtil;
import com.platuro.delivery.Until.LocationStorageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.apache.logging.log4j.LogManager.getLogger;

public class Chests {
    // Add the Address chests here
    public Map<Location, String> addressChests;
    public Map<Location, String> senderChests;

    // Add a list of Inventories for the sender chests
    public Map<String, Inventory> courierStack = new HashMap<>();

    private Plugin plugin;
    private JavaPlugin javaPlugin;
    private LocationStorageUtil addressChestslocationStorage;
    private LocationStorageUtil senderChestslocationStorage;

    private InventoryStorageUtil inventoryStorageUtil;

    public Chests(Plugin plugin, JavaPlugin javaPlugin) {
        this.plugin = plugin;
        this.javaPlugin = javaPlugin;
        // Initialize the location storage utilities
        addressChestslocationStorage = new LocationStorageUtil(javaPlugin, "addressChests");
        senderChestslocationStorage = new LocationStorageUtil(javaPlugin, "senderChests");
        // Load the chests from the config
        addressChests = addressChestslocationStorage.loadLocations();
        senderChests = senderChestslocationStorage.loadLocations();
        // Initialize the courierStack
        inventoryStorageUtil = new InventoryStorageUtil(javaPlugin, "courierStack");
        courierStack = inventoryStorageUtil.loadInventories();

        ScanForChests();
    }

    public void OnDisable() {
        // Save the chests to the config
        addressChestslocationStorage.saveLocations(addressChests);
        senderChestslocationStorage.saveLocations(senderChests);
        inventoryStorageUtil.saveInventories(courierStack);
    }

    void startSignCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Chunk chunk : Bukkit.getWorld("world").getLoadedChunks()) {
                    for (BlockState state : chunk.getTileEntities()) {
                        if (state instanceof Sign) {
                            checkSign((Sign) state);
                        }
                    }
                }
                // Send items from sender chests to address chests
                for (Map.Entry<Location, String> senderChest : senderChests.entrySet()) {
                    for (Map.Entry<Location, String> addressChest : addressChests.entrySet()) {
                        if (senderChest.getValue().equals(addressChest.getValue())) {
                            //TransferItems(senderChest.getKey(), addressChest.getKey());
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 60 * 5); // Run every 5 minutes
    }

    private void ScanForChests() {
        for (Chunk chunk : Bukkit.getWorld("world").getLoadedChunks()) {
            for (BlockState state : chunk.getTileEntities()) {
                if (state instanceof Sign) {
                    checkSign((Sign) state);
                }
            }
        }
    }

    private void checkSign(Sign sign) {
        if (sign.getBlockData() instanceof WallSign) {
            Block attached = sign.getBlock().getRelative(((WallSign) sign.getBlockData()).getFacing().getOppositeFace());
            if (attached.getType() == Material.CHEST) {
                String role = sign.getLine(0).toLowerCase();
                String address = sign.getLine(1);
                if (role.contains("[address]")) {
                    addressChests.put(attached.getLocation(), address);
                    getLogger().info("Registered address chest at " + attached.getLocation());
                } else if (role.contains("[targetaddress]")) {
                    senderChests.put(attached.getLocation(), address);
                    getLogger().info("Registered sender chest at " + attached.getLocation());
                }
                addressChestslocationStorage.saveLocations(addressChests);
                senderChestslocationStorage.saveLocations(senderChests);
            }
        }
    }

    private void TransferItems(Inventory fromInventory, Inventory toInventory) {
            // Attempt to move each item from the 'from' chest to the 'to' chest
            for (ItemStack item : fromInventory.getContents()) {
                if (item != null) {
                    // Check how much of this item can be added to the 'to' inventory
                    HashMap<Integer, ItemStack> unmovable = toInventory.addItem(item.clone());
                    if (!unmovable.isEmpty()) {
                        // If not all items could be moved, leave the remainder in the 'from' chest
                        item.setAmount(unmovable.get(0).getAmount());
                    } else {
                        // If all items were moved, remove them from the 'from' chest
                        fromInventory.remove(item);
                    }
                }
            }
    }

    boolean hasItemsInChest(Location location) {
        if(senderChests.containsKey(location)) {
            return Arrays.stream(((Chest) location.getBlock().getState()).getInventory().getContents()).anyMatch(item -> item != null);
        }else {
            return false;
        }
    }

    boolean hasCourierItems() {
        return courierStack.values().stream().anyMatch(inventory -> Arrays.stream(inventory.getContents()).anyMatch(item -> item != null));
    }

    public void openChest(Location location) {
        // Handle Address Chest Logic: Retrieve items from courierStack and place them into the chest
        if (addressChests.containsKey(location)) {
            Inventory chestInventory = ((Chest) location.getBlock().getState()).getInventory();
            String address = addressChests.get(location);
            if (courierStack.containsKey(address)) {
                Inventory courierInventory = courierStack.get(address);
                TransferItems(courierInventory, chestInventory);
            }
        }
        // Handle Sender Chest Logic: Retrieve items from the chest and place them into the courierStack
        if (senderChests.containsKey(location)) {
            Inventory chestInventory = ((Chest) location.getBlock().getState()).getInventory();
            String address = senderChests.get(location);
            if (!courierStack.containsKey(address)) {
                courierStack.put(address, Bukkit.createInventory(null, 27, "Courier Stack"));
            }
            Inventory courierInventory = courierStack.get(address);
            TransferItems(chestInventory, courierInventory);
        }


        /* ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer openChest = protocolManager.createPacket(PacketType.Play.Server.BLOCK_ACTION);
        BlockPosition position = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        openChest.getBlockPositionModifier().write(0, (com.comphenix.protocol.wrappers.BlockPosition) position);
        openChest.getIntegers()
                .write(0, 1)  // Action ID for opening a chest
                .write(1, 1); // Action parameter for opening

        location.getWorld().getPlayers().forEach(player -> {
            try {
                protocolManager.sendServerPacket(player, openChest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Close the chest after 3 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                PacketContainer closeChest = protocolManager.createPacket(PacketType.Play.Server.BLOCK_ACTION);
                closeChest.getBlockPositionModifier().write(0, position);
                closeChest.getIntegers()
                        .write(0, 1)  // Action ID for closing a chest
                        .write(1, 0); // Action parameter for closing

                location.getWorld().getPlayers().forEach(player -> {
                    try {
                        protocolManager.sendServerPacket(player, closeChest);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                this.cancel();
            }
        }.runTaskLater(plugin, 20L * 3);*/
    }


}

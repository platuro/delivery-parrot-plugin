package com.platuro.delivery;

import com.platuro.delivery.Until.InventoryStorageUtil;
import com.platuro.delivery.Until.LocationStorageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostOffice implements Listener {
    private final Deliveryman plugin;
    //Create inventories for the post office
    public Map<Location, String> postofficeLocations = new HashMap<>();
    public Map<String, Inventory> postofficeInventory = new HashMap<>();
    private InventoryStorageUtil inventoryStorageUtil;
    private LocationStorageUtil postofficeLocationStorageUtil;
    List<Villager> postmen = new ArrayList<>();
    public PostOffice(Deliveryman plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        inventoryStorageUtil = new InventoryStorageUtil(plugin, "postoffice.inventories");
        postofficeLocationStorageUtil = new LocationStorageUtil(plugin, "postoffice.locations");
    }

    public void Init(){
        postofficeLocations = postofficeLocationStorageUtil.loadLocations();
        postofficeInventory = inventoryStorageUtil.loadInventories("Mailbox");
        // for each location in the postofficeLocations, create a post office
        for (Location location : postofficeLocations.keySet()) {
            //print the location
            System.out.println(location);
            CreatePostOffice(location);
        }
    }

    public void OnStop() {
        // Save the inventories
        inventoryStorageUtil.saveInventories(postofficeInventory);
        // Save the locations
        // Only save if the villager is still alive
        for (Villager postman : postmen) {
            if (postman.isDead()) {
                postofficeLocations.remove(postman.getLocation());
            }
        }
        postofficeLocationStorageUtil.saveLocations(postofficeLocations);
        // Unregister the listener
        PlayerInteractEntityEvent.getHandlerList().unregister(this);
        // Kill all the postmen
        for (Villager postman : postmen) {
            postman.damage(1000);
            postman.remove();
        }
    }

    public void CreatePostOffice(Location location) {
        CreatePostOffice(location, false);
    }

    public void CreatePostOffice(Location location, boolean ai) {
        // Create a new post office
        // Spawn a villager at the position of the player
        // The villager will be the postman
        Villager postman;
        postman = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        postman.setCustomName("Postman");
        postman.setCustomNameVisible(true);
        postman.setAI(ai);
        postman.setProfession(Villager.Profession.LIBRARIAN);
        postman.setVillagerType(Villager.Type.PLAINS);
        postman.setSilent(true);
        postman.setCanPickupItems(false);
        postman.setCollidable(false);

        postman.setMetadata("Postman", new FixedMetadataValue(plugin, true));

        postmen.add(postman);
        // Add location to the postofficeLocations
        postofficeLocations.put(location, "PostOffice");
        // Save the postofficeLocations
        postofficeLocationStorageUtil.saveLocations(postofficeLocations);
    }

    @EventHandler
    public void onEntityRightClick(PlayerInteractEntityEvent event) {
        // Check if the player right-clicked on a villager
        if (event.getRightClicked() instanceof Villager) {
            Villager villager = (Villager) event.getRightClicked();
            // Check if the villager is the postman
            if (!villager.getMetadata("Postman").isEmpty()) {
                event.setCancelled(true);  // Cancel the event to avoid default interactions
                // Open the post office inventory
                OpenPostOfficeInventory(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Check if there is a post office in the new chunk
        if(hasItemsInPostOffice(event.getChunk().getBlock(8, 8, 8).getLocation()))
            return;

        // Only spawn with a 1/100 chance
        if (Math.random() > 0.01)
            return;

        // Example: Spawn a creeper at the center of the new chunk
        Location spawnLocation = event.getChunk().getBlock(8, event.getWorld().getHighestBlockYAt(8, 8) + 1, 8).getLocation();
        CreatePostOffice(spawnLocation, true);
    }

    public void OpenPostOfficeInventory(Player player) {
        Inventory postOfficeInventory = plugin.getServer().createInventory(null, 27, "Post Office");

        // Assuming Deliveryman.chests.getAddresses() returns a list of strings (addresses)
        for (String address : Deliveryman.chests.GetAddresses()) {
            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("Mailbox: " + address);
            item.setItemMeta(meta);
            if(postOfficeInventory.contains(item)){
                continue;
            }
            postOfficeInventory.addItem(item);
        }

        player.openInventory(postOfficeInventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Post Office")) {
            event.setCancelled(true); // Prevent moving items in the inventory

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta()) {
                Player player = (Player) event.getWhoClicked();
                String address = clickedItem.getItemMeta().getDisplayName().replace("Mailbox: ", "");

                // Check if there is a inventory for the address
                if (postofficeInventory.containsKey(address)) {
                    Inventory inventory = postofficeInventory.get(address);
                    // Set Title to Mailbox: address
                    player.openInventory(postofficeInventory.get(address));
                } else {
                    // Create a new inventory for the address
                    Inventory addressInventory = plugin.getServer().createInventory(null, 27, "Mailbox: " + address);
                    postofficeInventory.put(address, addressInventory);
                    player.openInventory(addressInventory);
                    inventoryStorageUtil.saveInventories(postofficeInventory);
                }
            }
        }
    }

    // Handle on Inventory Close
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().contains("Mailbox")) {
            Inventory inventory = event.getInventory();
            String address = postofficeInventory.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(inventory))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            // If there were Items send a message to the player
            if (inventory.firstEmpty() != 0) {
                event.getPlayer().sendMessage("Items have been sent to the courier");
            }
            postofficeInventory.put(address, inventory);
            inventoryStorageUtil.saveInventories(postofficeInventory);
            TransferToCourier();
        }
    }

    public void TransferToCourier(){
        // Transfer the items from the post office to the courier
        for (String address : postofficeInventory.keySet()) {
            Inventory inventory = postofficeInventory.get(address);
            Deliveryman.chests.AddCourierItems(address, inventory);
            // Clear the inventory
            inventory.clear();
        }
        inventoryStorageUtil.saveInventories(postofficeInventory);
    }

    public boolean hasItemsInPostOffice(Location location) {
        return postofficeInventory.containsKey(location);
    }
}

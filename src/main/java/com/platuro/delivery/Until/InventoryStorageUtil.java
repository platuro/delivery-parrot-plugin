package com.platuro.delivery.Until;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class InventoryStorageUtil {
    private final JavaPlugin plugin;
    private final String configPath;

    public InventoryStorageUtil(JavaPlugin plugin, String configPath) {
        this.plugin = plugin;
        this.configPath = configPath;
    }

    public void saveInventories(Map<String, Inventory> inventories) {
        FileConfiguration config = plugin.getConfig();
        for (Map.Entry<String, Inventory> entry : inventories.entrySet()) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

                ItemStack[] contents = entry.getValue().getContents();
                dataOutput.writeInt(contents.length);

                for (ItemStack stack : contents) {
                    dataOutput.writeObject(stack);
                }

                dataOutput.close();
                String serializedInventory = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                config.set(configPath + "." + entry.getKey(), serializedInventory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        plugin.saveConfig();
    }

    public Map<String, Inventory> loadInventories() {
        return loadInventories("Chest");
    }

    public Map<String, Inventory> loadInventories(String Title) {
        Map<String, Inventory> inventories = new HashMap<>();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection(configPath);

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String serializedInventory = section.getString(key);
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(serializedInventory));
                    BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
                    Inventory inventory =   Bukkit.getServer().createInventory(null, dataInput.readInt(), Title);

                    for (int i = 0; i < inventory.getSize(); i++) {
                        inventory.setItem(i, (ItemStack) dataInput.readObject());
                    }

                    dataInput.close();
                    inventories.put(key, inventory);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return inventories;
    }
}
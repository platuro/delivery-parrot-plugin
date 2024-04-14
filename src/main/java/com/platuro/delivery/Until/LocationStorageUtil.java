package com.platuro.delivery.Until;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class LocationStorageUtil {
    private final JavaPlugin plugin;
    private final String configPath;

    public LocationStorageUtil(JavaPlugin plugin, String configPath) {
        this.plugin = plugin;
        this.configPath = configPath;
    }

    public void saveLocations(Map<Location, String> locations) {
        FileConfiguration config = plugin.getConfig();
        // clear the existing locations
        config.set(configPath, null);
        for (Map.Entry<Location, String> entry : locations.entrySet()) {
            Location loc = entry.getKey();
            String locString = loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
            config.set(configPath + "." + locString, entry.getValue());
        }
        plugin.saveConfig();
    }

    public Map<Location, String> loadLocations() {
        Map<Location, String> locations = new HashMap<>();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection(configPath);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String[] parts = key.split(";");
                if (parts.length < 4) {
                    // Log error or skip this entry if it does not conform to expected format
                    System.err.println("Skipping malformed location key: " + key);
                    continue;
                }
                World world = Bukkit.getWorld(parts[0]);
                if (world == null) {
                    // Handle case where world is not found
                    System.err.println("World not found: " + parts[0]);
                    continue;
                }
                try {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    Location loc = new Location(world, x, y, z);
                    locations.put(loc, section.getString(key));
                } catch (NumberFormatException e) {
                    // Handle malformed number inputs
                    System.err.println("Number format error in location key: " + key);
                    continue;
                }
            }
        }
        return locations;
    }
}
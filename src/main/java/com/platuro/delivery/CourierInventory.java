package com.platuro.delivery;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.bukkit.Bukkit.getServer;

public class CourierInventory implements Listener, CommandExecutor {

    public CourierInventory(Deliveryman plugin) {
        getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("openinventory").setExecutor(this);
    }

    @EventHandler
    public void onEntityRightClick(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Parrot) {
            Parrot courier = (Parrot) event.getRightClicked();
            // Check if the villager is your courier
            if (!courier.getMetadata("Courier").isEmpty()){
                event.setCancelled(true);  // Optional: cancel the event to avoid default interactions
                openCourierInventory(event.getPlayer());
            }
        }
    }

    private void openCourierInventory(Player player) {
        // Check if there is a courierStack with the player's name
        Inventory courierInventory = Deliveryman.chests.CreateInventoryByName(player.getName());
        player.openInventory(courierInventory);
    }

    public void Dispose() {
        // Unregister the listener
        PlayerInteractEntityEvent.getHandlerList().unregister(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if(command.getName().equalsIgnoreCase("call")){
            Player player = (Player) sender;
            //Deliveryman.courier.AddLocation(player.getLocation());
            return true;
        }else if(command.getName().equalsIgnoreCase("openinventory")){
            Player player = (Player) sender;
            openCourierInventory(player);
            return true;
        }
        return false;
    }
}

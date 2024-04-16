package com.platuro.delivery;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Arrays;

public class SignListener implements Listener {
    Deliveryman deliveryman;

    SignListener(Deliveryman deliveryman){
        this.deliveryman = deliveryman;
        // Register the listener
        deliveryman.getServer().getPluginManager().registerEvents(this, deliveryman);
    }

    // Check if a new sign has been placed and check the text
    // Check if this address already exists

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.getLines();
        String[] adresses = Deliveryman.chests.GetAddresses();
        if(lines[0].equals("[address]")){
            Deliveryman.chests.ScanForChests();
        }

        // Check if the sign has a username
        if(lines[0].equals("[address]") && checkIfUsernameExists(lines[1]) && !lines[1].equals("")){
            // Check if the username is the Players username
            if(!lines[1].equals(event.getPlayer().getName())){
                event.getPlayer().sendMessage("You can only add your own address or a fictional address, not someone else's address");
                event.setCancelled(true);
                return;
            }
        }

        if (Arrays.asList(adresses).contains(lines[1]) && !lines[1].equals("") && lines[0].equals("[address]")) {
            event.getPlayer().sendMessage("This address already exists");
            event.setCancelled(true);
            return;
        }
        if(lines[0].equals("[address]")){
            event.getPlayer().sendMessage("Address added");
        }
    }

    public boolean checkIfUsernameExists(String username) {
        // Check if the Server has a player with the given username
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        return offlinePlayer.hasPlayedBefore();
    }

    public void Dispose() {
        // Unregister the listener
        SignChangeEvent.getHandlerList().unregister(this);
    }
}

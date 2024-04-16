package com.platuro.delivery;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CommandListener implements CommandExecutor {
    Deliveryman deliveryman;

    CommandListener(Deliveryman deliveryman){
        this.deliveryman = deliveryman;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        var argsList = List.of(args);

        if(argsList.contains("call")){
            Player player = (Player) sender;
            Deliveryman.courier.AddLocation(player.getLocation());
            return true;
        }else if(argsList.contains("openinventory")){
            Player player = (Player) sender;
            deliveryman.courierInventory.openCourierInventory(player);
            return true;
        }else if(argsList.contains("postoffice")){
            deliveryman.postOffice.CreatePostOffice(((Player) sender).getLocation());
            return true;
        }
        return false;
    }
}

package me.cynadyde.simplemachines;

import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleMachinesPlugin extends JavaPlugin implements Listener {

    // TODO change up hopper mechanics a little bit:
    //   should be able to serve items in round-robin order

    Block target = null;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new AutoCrafter(this), this);
        getServer().getPluginManager().registerEvents(new HopperFilter(this), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command == getCommand("target")) {
            if (sender instanceof Player) {
                Block block = ((Player) sender).getTargetBlockExact(16);
                target = (block != null && block.getState() instanceof Container) ? block : null;
                sender.sendMessage("§f[§3SM§f] §aNow targeting: §e" + (target == null ? "NOTHING" : target));
            }
        }
        return true;
    }
}

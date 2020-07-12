package me.cynadyde.simplemachines;

import me.cynadyde.simplemachines.machine.AutoCrafter;
import me.cynadyde.simplemachines.machine.HopperFilter;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleMachinesPlugin extends JavaPlugin implements Listener {

    public Block target = null;  // debug purposes

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new AutoCrafter(this), this);
        getServer().getPluginManager().registerEvents(new HopperFilter(this), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command == getCommand("target")) {  // debug purposes
            if (sender instanceof Player) {
                Block block = ((Player) sender).getTargetBlockExact(16);
                target = (block != null && block.getState() instanceof Container) ? block : null;
                sender.sendMessage("§f[§3SM§f] §aNow targeting: §e" + (target == null ? "NOTHING" : target));
            }
        }
        return true;
    }
}

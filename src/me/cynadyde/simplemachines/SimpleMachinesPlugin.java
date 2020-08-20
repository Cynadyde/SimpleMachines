package me.cynadyde.simplemachines;

import me.cynadyde.simplemachines.gui.ItemFilterGui;
import me.cynadyde.simplemachines.machine.*;
import me.cynadyde.simplemachines.util.PluginKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleMachinesPlugin extends JavaPlugin implements Listener {

    public Block target = null;  // debug purposes

    private AutoCrafter autoCrafterModule;
    private ItemFilter itemFilterModule;
    private ItemFilterGui itemFilterGuiModule;
    private BlockBreaker blockBreakerModule;
    private BlockPlacer blockPlacerModule;
    private RedstoneClock redstoneClockModule;

    @Override
    public void onEnable() {
        PluginKey.refresh(this);

        getServer().getPluginManager().registerEvents(autoCrafterModule = new AutoCrafter(this), this);
        getServer().getPluginManager().registerEvents(itemFilterModule = new ItemFilter(this), this);
        getServer().getPluginManager().registerEvents(itemFilterGuiModule = new ItemFilterGui(this), this);
        getServer().getPluginManager().registerEvents(blockBreakerModule = new BlockBreaker(this), this);
        getServer().getPluginManager().registerEvents(blockPlacerModule = new BlockPlacer(this), this);
        getServer().getPluginManager().registerEvents(redstoneClockModule = new RedstoneClock(this), this);
    }

    @Override
    public void onDisable() {
        itemFilterGuiModule.closeAllGuis();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender.isOp()) {
            if (command == getCommand("target")) {  // debug purposes
                if (sender instanceof Player) {
                    Block block = ((Player) sender).getTargetBlockExact(16);
                    target = (block != null && block.getState() instanceof Container) ? block : null;
                    sender.sendMessage("§f[§3SM§f] §aNow targeting: §e" + (target == null ? "NOTHING" : target));
                }
            }
            else if (command == getCommand("state")) {  // debug purposes
                if (sender instanceof Player) {
                    Block block = ((Player) sender).getTargetBlockExact(16);
                    if (block != null) {
                        sender.sendMessage("§f[§3SM§f] §aLooking at: §b" + block.getState());
                    }
                }
            }
        }
        return true;
    }
}

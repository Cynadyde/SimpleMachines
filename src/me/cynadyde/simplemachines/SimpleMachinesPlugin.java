package me.cynadyde.simplemachines;

import me.cynadyde.simplemachines.gui.ItemTransfererGui;
import me.cynadyde.simplemachines.machine.*;
import me.cynadyde.simplemachines.util.PluginKey;
import me.cynadyde.simplemachines.util.ReflectiveUtils;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleMachinesPlugin extends JavaPlugin implements Listener {

    // TODO iron trapdoor for liqwuids mode and jungle for normal order (so that dispenser behavior can be overriden!)

    // TODO wrench module (right click hoe (non-sneak) to rotate blocks!)

    // TODO separate method from itemInput for POUR_INTO policy so that stacks of empty buckets can be used

    // TODO retire the hopper-suck-liquid and interact-container-pour functionality

    // TODO very simple dropper-into-glass item piping system

    public Block target = null;  // debug purposes

    private AutoCrafter autoCrafterModule;
    private ItemTransferer itemTransfererModule;
    private ItemTransfererGui itemTransfererGuiModule;
    private BlockBreaker blockBreakerModule;
    private BlockPlacer blockPlacerModule;
    private RedstoneClock redstoneClockModule;

    @Override
    public void onEnable() {
        PluginKey.refresh(this);
        ReflectiveUtils.setLogger(getLogger());
        ReflectiveUtils.reflect();

        getServer().getPluginManager().registerEvents(autoCrafterModule = new AutoCrafter(this), this);
        getServer().getPluginManager().registerEvents(itemTransfererModule = new ItemTransferer(this), this);
        getServer().getPluginManager().registerEvents(itemTransfererGuiModule = new ItemTransfererGui(this), this);
        getServer().getPluginManager().registerEvents(blockBreakerModule = new BlockBreaker(this), this);
        getServer().getPluginManager().registerEvents(blockPlacerModule = new BlockPlacer(this), this);
        getServer().getPluginManager().registerEvents(redstoneClockModule = new RedstoneClock(this), this);
    }

    @Override
    public void onDisable() {
        itemTransfererModule.cancelTasks();
        itemTransfererGuiModule.closeAllGuis();
        blockBreakerModule.cancelAllJobs();
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

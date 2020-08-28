package me.cynadyde.simplemachines;

import me.cynadyde.simplemachines.gui.ItemTransfererGui;
import me.cynadyde.simplemachines.machine.*;
import me.cynadyde.simplemachines.tool.BlockRotater;
import me.cynadyde.simplemachines.util.PluginKey;
import me.cynadyde.simplemachines.util.ReflectiveUtils;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleMachinesPlugin extends JavaPlugin implements Listener {

    // TODO apply transfer scheme to dispenser / dropper launched items

    // TODO iron trapdoor for liqwuids mode and jungle for normal order (so that dispenser behavior can be overriden!)

    // TODO separate method from itemInput for POUR_INTO policy so that stacks of empty buckets can be used

    // TODO retire the hopper-suck-liquid and interact-container-pour functionality

    // TODO very simple dropper-into-glass item piping system

    private AutoCrafter autoCrafterModule;
    private ItemTransferer itemTransfererModule;
    private ItemTransfererGui itemTransfererGuiModule;
    private BlockBreaker blockBreakerModule;
    private BlockPlacer blockPlacerModule;
    private RedstoneClock redstoneClockModule;
    private BlockRotater blockRotaterModule;
    private ItemPiper itemPiperModule;

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
        getServer().getPluginManager().registerEvents(blockRotaterModule = new BlockRotater(this), this);
        getServer().getPluginManager().registerEvents(itemPiperModule = new ItemPiper(this), this);
    }

    @Override
    public void onDisable() {
        itemTransfererModule.cancelTasks();
        itemTransfererGuiModule.closeAllGuis();
        blockBreakerModule.cancelAllJobs();
    }
}

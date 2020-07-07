package me.cynadyde.simplemachines;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleMachinesPlugin extends JavaPlugin implements Listener {

    // TODO change up hopper mechanics a little bit:
    //   should be able to serve items in round-robin order

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new AutoCrafter(this), this);
        getServer().getPluginManager().registerEvents(new HopperFilter(this), this);
    }
}

package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import org.bukkit.event.Listener;

public class BlockBreaker implements Listener {

    private final SimpleMachinesPlugin plugin;

    public BlockBreaker(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
    }
}

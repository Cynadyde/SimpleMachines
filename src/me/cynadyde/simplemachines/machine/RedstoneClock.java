package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import org.bukkit.event.Listener;

/*
 * Maybe just make the clock and delay block as originally planned?
 * Player heads with a nice texture
 * Could have them be sort of like a new "repeater" or "comparator" piece
 * Adjacent redstone dust tries to "connect" visually (if i can manage that)
 * Can only place on top face of blocks- and rotation is fixed to N E S W
 * Would essentially add them to a repeating task based on when their tile entity is loaded / unloaded in chunk
 * Should actually reduce quite a bit of lag :u
 */

public class RedstoneClock implements Listener {

    private final SimpleMachinesPlugin plugin;

    public RedstoneClock(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
    }
}

package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import org.bukkit.event.Listener;

/*
 * here are my ideas:
 *
 * So, you outline an 8x8 area with gold blocks.
 * You can build with basic redstone blocks in this area (4 tall)
 * Craft a special empty map and activate it in this area to create the circuit board
 * A map is created with the components rendered to it
 * It can then be placed on the top of a block (item frame is automatic) (cant be put into item frame)
 *
 *
 * Craft the circuit board to get a map that you can place on top of blocks (same idea as the last)
 * The edges of the map are drawn, and there are 4 pins for I/O
 * Click inside this map with redstone components to add them to the circuit
 * Mine them back out like you would actual blocks
 * No height - single layer
 *
 *
 * Maybe just make the clock and delay block as originally planned?
 * Player heads with a nice texture
 * Could have them be sort of like a new "repeater" or "comparator" piece
 * Adjacent redstone dust tries to "connect" visually (if i can manage that)
 * Can only place on top face of blocks- and rotation is fixed to N E S W
 * Would essentially add them to a repeating task based on when their tile entity is loaded / unloaded in chunk
 * Should actually reduce quite a bit of lag :u
 */

public class CircuitBoard implements Listener {

    private final SimpleMachinesPlugin plugin;

    public CircuitBoard(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
    }
}

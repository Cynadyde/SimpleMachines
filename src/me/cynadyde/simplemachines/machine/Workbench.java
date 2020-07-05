package me.cynadyde.simplemachines.machine;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Dropper;

/**
 * A crafting table that stores its contents and
 * can be viewed by multiple players at a time.
 * <p>
 * It is formed by a crafting table on top of a dropper
 * pointing up into it.
 */
public class Workbench {

    private final Block table;
    private final Dropper dropper;

    public Workbench(Block table, Dropper dropper) {
        this.table = table;
        this.dropper = dropper;
    }

    public Block getLocation() {
        return table;
    }

    public Dropper getDropper() {
        return dropper;
    }
}

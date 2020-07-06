package me.cynadyde.simplemachines.machine;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public abstract class Machine {

    public abstract Block getPos();

    public abstract void interactedBy(Player player);
}

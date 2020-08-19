package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;

public class BlockPlacer implements Listener {

    private final SimpleMachinesPlugin plugin;

    public BlockPlacer(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        if (isBlockPlacingMachine(event.getBlock())) {
            event.setCancelled(true);

            final Block machine = event.getBlock();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> doBlockPlace(machine), 0L);
        }
    }

    public boolean isBlockPlacingMachine(Block block) {
        if (block.getType() == Material.DROPPER) {
            Block part = block.getRelative(BlockFace.UP);
            if (part.getType() == Material.PISTON
                    && ((Directional) part.getBlockData()).getFacing() == BlockFace.DOWN) {
                return true;
            }
        }
        return false;
    }

    public void doBlockPlace(Block machine) {

        // TODO
    }
}

package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import me.cynadyde.simplemachines.util.ReflectiveUtils;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dropper;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.Iterator;

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

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        InventoryHolder holder = event.getSource().getHolder();
        if (holder instanceof BlockState) {

            final Block machine = ((BlockState) holder).getBlock();
            if (isBlockPlacingMachine(machine)) {
                event.setCancelled(true);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> doBlockPlace(machine), 0L);
            }
        }
    }

    public boolean isBlockPlacingMachine(Block block) {
        if (block.getType() == Material.DROPPER) {
            Block part = block.getRelative(BlockFace.UP);
            return part.getType() == Material.PISTON
                    && ((Directional) part.getBlockData()).getFacing() == BlockFace.DOWN;
        }
        return false;
    }

    public void doBlockPlace(Block machine) {
        if (isBlockPlacingMachine(machine)) {

            Dropper dropper = (Dropper) machine.getState();
            BlockFace facing = ((Directional) dropper.getBlockData()).getFacing();

            Block dest = machine.getRelative(facing, 1);
            if (dest.getType().isAir() || dest.isLiquid()) {

                boolean waterlogged = dest.getType() == Material.WATER;

                // find the next block to place, if any...
                int slot = -1;
                ItemStack[] contents = dropper.getInventory().getContents();
                Iterator<Integer> iterator = new RandomPermuteIterator(0, contents.length);
                while (iterator.hasNext()) {
                    int i = iterator.next();
                    ItemStack item = contents[i];
                    if (item != null && item.getType().isBlock()) {
                        slot = i;
                        break;
                    }
                }
                if (slot != -1) {
                    ItemStack item = contents[slot];

                    dest.setType(item.getType());
                    BlockData destData = dest.getBlockData();

                    if (destData instanceof Directional) {
                        ((Directional) destData).setFacing(facing.getOppositeFace());
                    }
                    if (destData instanceof Rotatable) {
                        ((Rotatable) destData).setRotation(facing.getOppositeFace());
                    }
                    if (destData instanceof Waterlogged) {
                        ((Waterlogged) destData).setWaterlogged(waterlogged);
                    }
                    dest.setBlockData(destData);

                    if (item.getItemMeta() instanceof BlockStateMeta) {
                        BlockStateMeta itemMeta = (BlockStateMeta) item.getItemMeta();
                        ReflectiveUtils.copyBlockState(dest.getState(), itemMeta.getBlockState());
                    }
                    dest.getWorld().playEffect(dest.getLocation().add(0.5, 0.5, 0.5), Effect.STEP_SOUND, dest.getType());

                    ItemStack altered = item.clone();
                    altered.setAmount(item.getAmount() - 1);
                    dropper.getInventory().setItem(slot, altered);
                }
            }
        }
    }
}

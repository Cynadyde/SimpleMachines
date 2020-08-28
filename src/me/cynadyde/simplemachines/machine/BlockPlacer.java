package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import me.cynadyde.simplemachines.util.ReflectiveUtils;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

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
        if (block.getType() == Material.DISPENSER) {
            Block part = block.getRelative(BlockFace.UP);
            return part.getType() == Material.PISTON
                    && ((Directional) part.getBlockData()).getFacing() == BlockFace.DOWN;
        }
        return false;
    }

    public void doBlockPlace(Block machine) {
        if (isBlockPlacingMachine(machine)) {

            Dispenser dropper = (Dispenser) machine.getState();
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
                    if (item != null && canPlaceItemAt(item, dest)) {
                        slot = i;
                        break;
                    }
                }
                if (slot != -1) {
                    ItemStack item = contents[slot];
                    BlockState oldState = dest.getState();

                    dest.setType(item.getType());
                    BlockData data = dest.getBlockData();

                    if (data instanceof Directional) {
                        ((Directional) data).setFacing(facing);
                    }
                    if (data instanceof Rotatable) {
                        ((Rotatable) data).setRotation(facing);
                    }
                    if (data instanceof Waterlogged) {
                        ((Waterlogged) data).setWaterlogged(waterlogged);
                    }
                    dest.setBlockData(data);
                    BlockState state = dest.getState();

                    oldState.update(true, true);

                    ItemMeta itemMeta = item.getItemMeta();
                    if (itemMeta instanceof BlockStateMeta) {
                        BlockStateMeta meta = (BlockStateMeta) itemMeta;
                        ReflectiveUtils.copyBlockState(meta.getBlockState(), state);
                        if (state instanceof Container && meta.hasDisplayName()) {
                            String name = meta.getDisplayName();
                            ReflectiveUtils.setCustomContainerName((Container) state, name);
                        }
                    }
                    else if ((itemMeta instanceof BannerMeta) && (state instanceof Banner)) {
                        BannerMeta meta = (BannerMeta) itemMeta;
                        Banner banner = (Banner) state;
                        banner.setPatterns(meta.getPatterns());
                    }
                    else if ((itemMeta instanceof SkullMeta) && (state instanceof Skull)) {
                        SkullMeta meta = (SkullMeta) itemMeta;
                        Skull skull = (Skull) state;
                        ReflectiveUtils.copySkullProfile(meta, skull);
                    }

                    BlockFormEvent event = new BlockFormEvent(dest, state);
                    plugin.getServer().getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {

                        event.getNewState().update(true, true);

                        ReflectiveUtils.playBlockPlaceSound(dest);

                        ItemStack altered = item.clone();
                        altered.setAmount(item.getAmount() - 1);
                        dropper.getInventory().setItem(slot, altered);
                    }
                }
            }
        }
    }

    private boolean canPlaceItemAt(ItemStack item, Block block) {
        return item.getType().isBlock() && ReflectiveUtils.canPlaceOn(item, block);
    }
}

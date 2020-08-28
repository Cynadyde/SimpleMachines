package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.util.ItemUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemPiper implements Listener {

    private final SimpleMachinesPlugin plugin;
    private int maxPipeLength = 32;

    public ItemPiper(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (isItemPiper(event.getBlock())) {
            Block block = event.getBlock();
            Container piper = (Container) block.getState();
            BlockFace facing = ((Directional) block.getBlockData()).getFacing();
            Block pipe = block.getRelative(facing, 1);
            if (ItemUtils.STAINED_GLASS.contains(pipe.getType())) {
                Container exit = findPipeDest(pipe, facing.getOppositeFace());
                if (exit != null) {
                    ItemStack item = event.getItem();
                    Inventory source = piper.getInventory();
                    Inventory dest = exit.getInventory();
                    System.out.println("wanted item: " + item);
                    for (ItemStack unremoved : source.removeItem(item).values()) {
                        if (unremoved.isSimilar(item)) {
                            item.setAmount(item.getAmount() - unremoved.getAmount());
                        }
                    }
                    System.out.println("given item: " + item);
                    dest.addItem(item);
                    piper.update(false, false);
                    exit.update(false, false);
                }
            }
            event.setCancelled(true);
        }
    }

    public boolean isItemPiper(Block block) {
        return block.getType() == Material.DISPENSER || block.getType() == Material.DROPPER;
    }

    private Container findPipeDest(Block entrance, BlockFace comingFrom) {
        List<BlockFace> allFaces = new ArrayList<>(ItemUtils.FACES);
        List<BlockFace> validFaces = allFaces.subList(1, allFaces.size());
        List<BlockFace> randFaces = allFaces.subList(2, allFaces.size());
        Set<Block> visited = new HashSet<>();
        visited.add(entrance);


        Material pipeType = entrance.getType();
        Block current = entrance;
        Block next;
        BlockFace facingTo = comingFrom.getOppositeFace();
        BlockFace facingFrom = comingFrom;
        pathfinding:
        for (int i = 0; i < maxPipeLength; i++) {

            Collections.swap(allFaces, 0, allFaces.indexOf(facingFrom));
            Collections.swap(allFaces, 1, allFaces.lastIndexOf(facingTo));
            Collections.shuffle(randFaces);
            for (BlockFace face : validFaces) {
                next = current.getRelative(face, 1);

                if (!visited.contains(next)) {
                    if (next.getType() == pipeType) {
                        current = next;
                        facingTo = face;
                        facingFrom = face.getOppositeFace();
                        visited.add(current);
                        continue pathfinding;
                    }
                    else if (isItemPiper(next)) {
                        return (Container) next.getState();
                    }
                }
            }
            break;
        }
        return null;
    }
}

package me.cynadyde.simplemachines;

import me.cynadyde.simplemachines.machine.Workbench;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dropper;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SimpleMachinesPlugin extends JavaPlugin implements Listener {

    // use set of crafting inv if players cant share it
    private Map<Block, Set<CraftingInventory>> openedWorkbenches = new HashMap<>();
    private Map<CraftingInventory, Block> openedWorkbenchInvs = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        // player is (non-sneak) left-clicking a block
        if (event.useInteractedBlock() != Event.Result.DENY
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && !event.getPlayer().isSneaking()) {

            // that block is part of a multi-block machine structure
            Workbench workbench = getWorkbench(event.getClickedBlock());
            if (workbench != null) {

                // cancel the event and open a crafting grid manually to keep track of it
                event.setUseInteractedBlock(Event.Result.DENY);

                InventoryView view = event.getPlayer().openWorkbench(workbench.getPos().getLocation(), true);
                if (view != null) {
                    CraftingInventory openedInv = (CraftingInventory) view.getTopInventory();

                    Set<CraftingInventory> openedInvs = openedWorkbenches.computeIfAbsent(
                            workbench.getPos(), k -> new HashSet<>());
                    openedInvs.add(openedInv);

                    openedWorkbenchInvs.put(openedInv, workbench.getPos());
                }
            }
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {

        CraftingInventory inv = event.getInventory();
        Block pos = openedWorkbenchInvs.get(inv);
        if (pos != null) {

            boolean updated = false;
            Workbench workbench = getWorkbench(pos);
            if (workbench != null) {

                ItemStack[] matrix = inv.getMatrix();
                for (int i = 0; i < 9; i++) {
                    workbench.getDropper().getInventory().setItem(i, matrix[i]);
                }
                updated = workbench.getDropper().update(false, false);
            }
            if (!updated) {
                openedWorkbenches.remove(pos);
                openedWorkbenchInvs.remove(inv);
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {

    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        Inventory inv = event.getInventory();
        if (inv instanceof CraftingInventory) {

            List<HumanEntity> viewers = new ArrayList<>(inv.getViewers());
            viewers.remove(event.getPlayer());
            if (viewers.isEmpty()) {
                openedWorkbenches.values().remove(inv);
                openedWorkbenchInvs.remove(inv);
            }
        }
    }

    /**
     * Gets the workbench multi-block structure at the given location.
     *
     * @param block the crafting table part of the multi-block
     * @return the workbench if it exists, else null
     */
    public Workbench getWorkbench(Block block) {

        if (block != null) {
            if (block.getType() == Material.CRAFTING_TABLE) {
                Block below = block.getRelative(BlockFace.DOWN);

                if (below.getType() == Material.DROPPER
                        && ((Directional) below.getBlockData()).getFacing() == BlockFace.UP) {

                    return new Workbench(block, ((Dropper) below.getState()));
                }
            }
            else if (block.getType() == Material.DROPPER) {
                if (((Directional) block.getBlockData()).getFacing() == BlockFace.UP) {

                    Block above = block.getRelative(BlockFace.UP);
                    if (above.getType() == Material.CRAFTING_TABLE) {

                        return new Workbench(above, ((Dropper) block.getState()));
                    }
                }
            }
        }
        return null;
    }
}

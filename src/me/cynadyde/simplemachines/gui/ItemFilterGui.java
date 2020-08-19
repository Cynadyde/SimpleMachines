package me.cynadyde.simplemachines.gui;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.transfer.PolicyType;
import me.cynadyde.simplemachines.util.Utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemFilterGui {

    private final SimpleMachinesPlugin plugin;
    private final Map<Inventory, Block> viewedGuis = new HashMap<>();
    private final Map<Block, Inventory> viewedBlocks = new HashMap<>();

    private final List<Integer> guiTrapdoorSlots = Arrays.asList(11, 12, 13, 14);

    private final ItemStack gui1 = Utils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
    private final ItemStack gui2 = Utils.createGuiItem(Material.WHITE_STAINED_GLASS_PANE, " ", "");

    private final ItemStack guiI = Utils.createGuiItem(
            Material.OAK_SIGN, "§2Input Policy",
            "§ePlace one of the following trapdoors",
            "§e to change which slots can take items.",
            "§fSPRUCE  §8->§b  only to empty slots",
            "§fBIRCH   §8->§b  only to non-empty slots"
    );

    private final ItemStack guiO = Utils.createGuiItem(
            Material.SPRUCE_SIGN, "§2Output Policy",
            "§ePlace one of the following trapdoors",
            "§e to change which slots can give items.",
            "§fOAK       §8->§b  only from solo slots",
            "§fDARK OAK  §8->§b  only from non-solo slots"
    );

    private final ItemStack guiS = Utils.createGuiItem(
            Material.BIRCH_SIGN, "§2Serve Policy",
            "§ePlace one of the following trapdoors",
            "§e to change the order to give out items.",
            "§fJUNGLE   §8->§b  in reversed slot order",
            "§fCRIMSON  §8->§b  in random slot order"
    );

    private final ItemStack guiR = Utils.createGuiItem(
            Material.DARK_OAK_SIGN, "§2Receive Policy",
            "§ePlace one of the following trapdoors",
            "§e to change the order to take in items.",
            "§fACACIA  §8->§b  in reversed slot order",
            "§fWARPED  §8->§b  in random slot order"
    );

    public ItemFilterGui(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getItem() != null) {

            if (event.getItem().getType() == Material.REDSTONE_TORCH) {
                if (!event.getPlayer().isSneaking()) {
                    if (event.getClickedBlock().getState() instanceof Container) {

                        Block block = event.getClickedBlock();
                        Player player = event.getPlayer();

                        event.setCancelled(true);

                        if (!viewedBlocks.containsKey(block)) {
                            Inventory inv = plugin.getServer().createInventory(player, 2 * 9, "Set Transfer Policy");
                            inv.setContents(new ItemStack[] {
                                    gui1, gui1, guiI, guiO, guiS, guiR, gui1, gui1, gui1,
                                    gui2, gui2, null, null, null, null, gui2, gui2, gui2
                            });
                            player.openInventory(inv);
                            viewedBlocks.put(block, inv);
                            viewedGuis.put(inv, block);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (viewedGuis.containsKey(event.getInventory())) {
            Inventory inv = event.getInventory();
            Block block = viewedGuis.get(inv);

            viewedGuis.remove(inv);
            viewedBlocks.remove(block);

            BlockState state = block.getState();
            if (state instanceof Container) {
                PersistentDataContainer data = ((Container) state).getPersistentDataContainer();

                for (int i : guiTrapdoorSlots) {
                    ItemStack item = inv.getItem(i);
                    if (item != null) {
                        Material trapdoor = item.getType();

//                        if (trapdoor == Material.OAK_TRAPDOOR) // TODO
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (viewedGuis.containsKey(event.getView().getTopInventory())) {
            Inventory inv = event.getView().getTopInventory();

            if (event.getClickedInventory() == inv) {
                if (guiTrapdoorSlots.contains(event.getSlot())) {
                    inv.setMaxStackSize(1);

                    ItemStack cursor = event.getCursor();

                    if (cursor != null && !cursor.getType().isAir() && cursor.getAmount() > 0) {

                        // get the first matching trapdoor group, if any...
                        for (PolicyType policyType : PolicyType.values()) {
                            if (policyType.hasTrapdoor(cursor.getType())) {

                                // if trapdoors from this group are already present, cancel the event...
                                for (int i : guiTrapdoorSlots) {
                                    ItemStack item = inv.getItem(i);
                                    if (item != null && policyType.hasTrapdoor(item.getType())) {
                                        event.setCancelled(true);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
                else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (viewedGuis.containsKey(event.getInventory())) {
            event.setCancelled(true);
        }
    }
}

package me.cynadyde.simplemachines.gui;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.transfer.*;
import me.cynadyde.simplemachines.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ItemTransfererGui implements Listener {

    private final SimpleMachinesPlugin plugin;
    private final Map<Block, Window> guis;

    private final int slotR = 11;
    private final int slotI = 12;
    private final int slotS = 13;
    private final int slotO = 14;
    private final int slotL = 15;

    private final ItemStack gui0 = new ItemStack(Material.AIR, 0);
    private final ItemStack gui1 = Utils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
    private final ItemStack gui2 = Utils.createGuiItem(Material.WHITE_STAINED_GLASS_PANE, " ", "");

    private final ItemStack guiR = Utils.createGuiItem(
            Material.DARK_OAK_SIGN, "§2Receive Policy",
            "§ePlace one of the following trapdoors",
            "§e to change the order to accept items.",
            "§fACACIA  §8->§b  Receive.REVERSE",
            "§fWARPED  §8->§b  Receive.RANDOM"
    );
    private final ItemStack guiI = Utils.createGuiItem(
            Material.OAK_SIGN, "§2Input Policy",
            "§ePlace one of the following trapdoors",
            "§e to change which slots can accept items.",
            "§fSPRUCE  §8->§b  Input.TO_EMPTY",
            "§fBIRCH   §8->§b  Input.TO_NONEMPTY"
    );
    private final ItemStack guiS = Utils.createGuiItem(
            Material.BIRCH_SIGN, "§2Serve Policy",
            "§ePlace one of the following trapdoors",
            "§e to change the order to give out items.",
            "§fJUNGLE   §8->§b  Serve.REVERSE",
            "§fCRIMSON  §8->§b  Serve.RANDOM"
    );
    private final ItemStack guiO = Utils.createGuiItem(
            Material.SPRUCE_SIGN, "§2Output Policy",
            "§ePlace one of the following trapdoors",
            "§e to change which slots can give items.",
            "§fOAK       §8->§b  Output.FROM_SOLO",
            "§fDARK OAK  §8->§b  Output.FROM_NONSOLO"
    );
    private final ItemStack guiL = Utils.createGuiItem(
            Material.JUNGLE_SIGN, "§2Liquids Policy",
            "§ePlace one of the following trapdoors",
            "§e to change how buckets are transferred.",
            "§fIRON  §8->§b  Liquids.FLOW"
    );

    public ItemTransfererGui(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
        this.guis = new HashMap<>();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getMaterial() == Material.REDSTONE_TORCH
                && !event.getPlayer().isSneaking()) {

            Block block = event.getClickedBlock();
            BlockState state = block.getState();
            if (state instanceof Container) {

                Player player = event.getPlayer();
                if (!guis.containsKey(block)) {

                    Window gui = new Window(player, (Container) state);
                    guis.put(block, gui);
                    player.openInventory(gui);

                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() instanceof Window) {

            Window gui = (Window) event.getView();
            if (event.getClickedInventory() == gui.getTopInventory()) {
                int slot = event.getSlot();
                ItemStack cursor = event.getCursor();
                Material token = cursor == null ? null : cursor.getType();

                gui.getTopInventory().setMaxStackSize(1);

                if (!isGuiSlotTokenValid(slot, token)) {
                    event.setCancelled(true);
                }
            }
            else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                // TODO if a slot will and can take the clicked item, split one off to that slot!
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView() instanceof Window) {
            Window gui = (Window) event.getView();

            for (Integer slot : event.getRawSlots()) {
                Inventory clickedInv = gui.getInventory(slot);
                if (gui.getTopInventory().equals(clickedInv)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView() instanceof Window) {
            Window gui = (Window) event.getView();
            guis.remove(gui.getContainer().getBlock());

            ReceivePolicy retrieve = Objects.requireNonNull(ReceivePolicy.fromToken(Utils.getMaterial(gui.view.getItem(slotR))));
            InputPolicy input = Objects.requireNonNull(InputPolicy.fromToken(Utils.getMaterial(gui.view.getItem(slotI))));
            ServePolicy serve = Objects.requireNonNull(ServePolicy.fromToken(Utils.getMaterial(gui.view.getItem(slotS))));
            OutputPolicy output = Objects.requireNonNull(OutputPolicy.fromToken(Utils.getMaterial(gui.view.getItem(slotO))));
            LiquidsPolicy liquids = Objects.requireNonNull(LiquidsPolicy.fromToken(Utils.getMaterial(gui.view.getItem(slotL))));

            TransferScheme scheme = new TransferScheme(retrieve, input, serve, output, liquids);
            scheme.applyTo(gui.getContainer());
        }
    }

    public void closeAllGuis() {
        // FIXME onInventoryClose is not called when plugin is being disabled!

        for (Window gui : guis.values()) {
            gui.getPlayer().closeInventory();
        }
        guis.clear();
    }

    private boolean isGuiSlotTokenValid(int slot, Material token) {
        switch (slot) {
            case slotR: return token == null || ReceivePolicy.fromToken(token) != null;
            case slotI: return token == null || InputPolicy.fromToken(token) != null;
            case slotS: return token == null || ServePolicy.fromToken(token) != null;
            case slotO: return token == null || OutputPolicy.fromToken(token) != null;
            case slotL: return token == null || LiquidsPolicy.fromToken(token) != null;
        }
        return false;
    }

    public class Window extends InventoryView {

        private final Player viewer;
        private final Container container;
        private final Inventory view;

        public Window(Player viewer, Container container) {
            if (!container.isPlaced()) {
                throw new IllegalArgumentException("the container must be placed in a world!");
            }
            this.viewer = viewer;
            this.container = container;
            this.view = Bukkit.createInventory(viewer, 2 * 9, getTitle());

            view.setContents(new ItemStack[] {
                    gui1, gui1, guiR, guiI, guiS, guiO, guiL, gui1, gui1,
                    gui2, gui2, gui0, gui0, gui0, gui0, gui0, gui2, gui2
            });

            TransferScheme scheme = TransferScheme.ofContainer(container);

            view.setItem(slotR, new ItemStack(scheme.RECEIVE.getToken()));
            view.setItem(slotI, new ItemStack(scheme.INPUT.getToken()));
            view.setItem(slotS, new ItemStack(scheme.SERVE.getToken()));
            view.setItem(slotO, new ItemStack(scheme.OUTPUT.getToken()));
            view.setItem(slotL, new ItemStack(scheme.LIQUIDS.getToken()));
        }

        public Container getContainer() {
            return container;
        }

        @Override
        public @NotNull Inventory getTopInventory() {
            return view;
        }

        @Override
        public @NotNull Inventory getBottomInventory() {
            return viewer.getInventory();
        }

        @Override
        public @NotNull HumanEntity getPlayer() {
            return viewer;
        }

        @Override
        public @NotNull InventoryType getType() {
            return InventoryType.CHEST;
        }

        @Override
        public @NotNull String getTitle() {
            return "Set Transfer Policy";
        }
    }
}

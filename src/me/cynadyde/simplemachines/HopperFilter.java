package me.cynadyde.simplemachines;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class HopperFilter implements Listener {

    /** The block faces that a hopper filter can be attached to. */
    public static final List<BlockFace> FACES = Collections.unmodifiableList(Arrays.asList(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)); //, BlockFace.UP, BlockFace.DOWN));

    /** The order to search for an item to take out when transferring. */
    public enum ServePolicy {NORMAL, REVERSED, RANDOM;}

    /** The order to search for a slot to put a transferred item into. */
    public enum RetrievePolicy {NORMAL, REVERSED, RANDOM;}

    /** The rules when choosing a slot to have an item transferred in. */
    public enum InputPolicy {NORMAL, MAX_ONE, LOCK_EMPTY;}

    /** The rules when choosing a slot to have an item transferred out. */
    public enum OutputPolicy {NORMAL, MIN_ONE, MIN_ONE_STACKABLE;}

    /**
     * An item stack and the raw inventory slot number it was found at,
     * enabling a recall on a transferred item to be done if needed.
     */
    public static class InvSlot {

        private final int slot;
        private final ItemStack item;

        public InvSlot(int slot, ItemStack item) {
            this.slot = slot;
            this.item = item;
        }

        public int getSlot() {
            return slot;
        }

        public ItemStack getItem() {
            return item;
        }
    }

    private final SimpleMachinesPlugin plugin;

    public HopperFilter(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {

//        System.out.println("picked up an item!");
        // cancel, run task to retrieve item, then kill entity if nothing rejected
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMove(InventoryMoveItemEvent event) {

        if (event.isCancelled()) return;
        boolean takeover = false;

        ServePolicy serve = ServePolicy.NORMAL;
        OutputPolicy output = OutputPolicy.NORMAL;
        RetrievePolicy retrieve = RetrievePolicy.NORMAL;
        InputPolicy input = InputPolicy.NORMAL;

        if (event.getSource().getHolder() instanceof Container) {
            Container source = (Container) event.getSource().getHolder();

            for (BlockFace face : FACES) {
                switch (source.getBlock().getRelative(face).getType()) {
                    case OAK_TRAPDOOR:
                        output = OutputPolicy.MIN_ONE;
                        takeover = true;
                        break;
                    case JUNGLE_TRAPDOOR:
                        serve = ServePolicy.REVERSED;
                        takeover = true;
                        break;
                    case DARK_OAK_TRAPDOOR:
                        output = OutputPolicy.MIN_ONE_STACKABLE;
                        takeover = true;
                        break;
                    case CRIMSON_TRAPDOOR:
                        serve = ServePolicy.RANDOM;
                        takeover = true;
                        break;
                }
            }
        }
        if (event.getDestination().getHolder() instanceof Container) {
            Container dest = (Container) event.getDestination().getHolder();

            for (BlockFace face : FACES) {
                switch (dest.getBlock().getRelative(face).getType()) {
                    case SPRUCE_TRAPDOOR:
                        input = InputPolicy.MAX_ONE;
                        takeover = true;
                        break;
                    case BIRCH_TRAPDOOR:
                        input = InputPolicy.LOCK_EMPTY;
                        takeover = true;
                        break;
                    case ACACIA_TRAPDOOR:
                        retrieve = RetrievePolicy.REVERSED;
                        takeover = true;
                        break;
                    case WARPED_TRAPDOOR:
                        retrieve = RetrievePolicy.RANDOM;
                        takeover = true;
                        break;
                }
            }
        }
        if (takeover) {
            final InventoryHolder s = event.getSource().getHolder();
            final InventoryHolder d = event.getDestination().getHolder();
            final ServePolicy e = serve;
            final OutputPolicy o = output;
            final RetrievePolicy r = retrieve;
            final InputPolicy i = input;

            /* cancel the event despite our monitor priority so that
               we can mimic the result of the event (with more control) */
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> performTransfer(s, d, e, o, r, i), 1L);
        }
    }

    public void performTransfer(InventoryHolder source, InventoryHolder dest,
            ServePolicy serve, OutputPolicy output, RetrievePolicy retrieve, InputPolicy input) {

        InvSlot served = performItemServe(source, serve, output);
        ItemStack leftover = performItemRetrieve(served.getItem(), dest, retrieve, input);
        if (leftover != null && leftover.getAmount() > 0) {

        }
    }

    public InvSlot performItemServe(InventoryHolder source, ServePolicy serve, OutputPolicy output) {
        int size = source.getInventory().getSize();
        Iterator<Integer> slots = serve == ServePolicy.RANDOM
                ? new RandomPermuteIterator(0, size)
                : IntStream.range(0, size).iterator();

        while (slots.hasNext()) {
            int slot = slots.next();
            if (serve == ServePolicy.REVERSED) {
                slot = size - 1 - slot;
            }

            ItemStack item = source.getInventory().getItem(slot);
            if (item != null && (output != OutputPolicy.MIN_ONE || item.getAmount() > 1)) {

                ItemStack take = item.clone();
                ItemStack leave = item.clone();

                source.getInventory().setItem(slot, leave);
                if (source instanceof Container) {
                    if (((Container) source).update(false, false)) {
                        return new InvSlot(slot, take);
                    }
                }
                else {
                    // TODO do entities with inventories have to be updated?
                    return new InvSlot(slot, take);
                }
            }
        }
        return null;
    }

    public ItemStack performItemRetrieve(ItemStack served, InventoryHolder dest, RetrievePolicy retrieve, InputPolicy input) {
        int size = dest.getInventory().getSize();
        Iterator<Integer> slots = retrieve == RetrievePolicy.RANDOM
                ? new RandomPermuteIterator(0, size)
                : IntStream.range(0, size).iterator();

        while (slots.hasNext()) {
            int slot = slots.next();
            if (retrieve == RetrievePolicy.REVERSED) {
                slot = size - 1 - slot;
            }
            if (retrieve == RetrievePolicy.NORMAL &&) {

            }

            switch (input) {
                case NORMAL:
                    dest.getInventory().addItem(served);
                    break;
                case LOCK_EMPTY:
                    break;
                case MAX_ONE:
                    break;
            }
        }
    }

    public void performItemReturn(InvSlot recall, InventoryHolder source) {
        source.getInventory().setItem(recall.getSlot(), recall.getItem());
        if (source instanceof Container) {
            ((Container) source).update(false, false);
        }
    }
}

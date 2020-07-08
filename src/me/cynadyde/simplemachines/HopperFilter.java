package me.cynadyde.simplemachines;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
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

    /* FIXME should prolly sub-class InventoryMoveItemEvent, cancel them
         at lowest priority level (first), and then call the sub-classed type */

    /** The block faces that a hopper filter can be attached to. */
    public static final List<BlockFace> FACES = Collections.unmodifiableList(Arrays.asList(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));

    /** The order to search for an item to take out when transferring. */
    public enum ServePolicy {NORMAL, REVERSED, RANDOM}

    /** The order to search for a slot to put a transferred item into. */
    public enum RetrievePolicy {NORMAL, REVERSED, RANDOM}

    /** The rules when choosing a slot to have an item transferred in. */
    public enum InputPolicy {NORMAL, MAX_ONE, LOCK_EMPTY}

    /** The rules when choosing a slot to have an item transferred out. */
    public enum OutputPolicy {NORMAL, MIN_ONE}

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

        World world = null;

        System.out.println("Caught onInventoryMove() with filters attached!");
        System.out.println("from " + event.getSource() + " to " + event.getDestination());

        if (event.getSource().getHolder() instanceof Container) {
            Block source = ((Container) event.getSource().getHolder()).getBlock();
            world = source.getWorld();

            System.out.println("source is a container!");

            for (BlockFace face : FACES) {
                switch (source.getRelative(face).getType()) {
                    case JUNGLE_TRAPDOOR:
                        serve = ServePolicy.REVERSED;
                        takeover = true;
                        break;
                    case DARK_OAK_TRAPDOOR:
                        output = OutputPolicy.MIN_ONE;
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
            Block dest = ((Container) event.getDestination().getHolder()).getBlock();
            world = dest.getWorld();

            System.out.println("destination is a container!");

            for (BlockFace face : FACES) {
                switch (dest.getRelative(face).getType()) {
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

            System.out.println("SERVE: " + serve);
            System.out.println("OUTPUT: " + output);
            System.out.println("RETRIEVE: " + retrieve);
            System.out.println("INPUT: " + input);

            // item amount (set to spigot world config hopper take amount)
            int a = Bukkit.spigot().getConfig().getInt("world-settings." + world.getName() + ".hopper-amount", 1);
            if (a < 1) return;

            final InventoryHolder s = event.getSource().getHolder();
            final InventoryHolder d = event.getDestination().getHolder();
            final ServePolicy e = serve;
            final OutputPolicy o = output;
            final RetrievePolicy r = retrieve;
            final InputPolicy i = input;

            /* cancel the event despite our monitor priority so that
               we can mimic the result of the event (with more control) */
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTaskLater(
                    plugin, () -> performTransfer(s, d, a, e, o, r, i), 1L);
        }
        else {
            System.out.println("event allowed to carry on as normal");
        }
        System.out.println("---------------------------------------");
    }

    public void performTransfer(InventoryHolder source, InventoryHolder dest, int amount,
            ServePolicy serve, OutputPolicy output, RetrievePolicy retrieve, InputPolicy input) {

        InvSlot served = performItemOutput(source, amount, serve, output);
        if (served != null) {

            ItemStack leftover = performItemInput(dest, served.getItem(), retrieve, input);
            if (leftover != null) {

                performItemRecall(new InvSlot(served.getSlot(), leftover), source);
            }
        }
    }

    public InvSlot performItemOutput(InventoryHolder source, int amount, ServePolicy serve, OutputPolicy output) {

        int size = source.getInventory().getSize();
        Iterator<Integer> slots = serve == ServePolicy.RANDOM
                ? new RandomPermuteIterator(0, size)
                : IntStream.range(0, size).iterator();

        while (slots.hasNext()) {
            int slot = slots.next();
            if (serve == ServePolicy.REVERSED) {
                slot = (size - 1) - slot;
            }

            ItemStack item = source.getInventory().getItem(slot);
            if (item != null && (output != OutputPolicy.MIN_ONE || item.getAmount() > 1 || item.getMaxStackSize() <= 1)) {

                int taken = Math.min(amount, item.getAmount());
                ItemStack take = item.clone();
                take.setAmount(taken);

                if (item.getAmount() > taken) {
                    item.setAmount(item.getAmount() - taken);
                }
                else {
                    item = null;
                }
                source.getInventory().setItem(slot, item);
                return new InvSlot(slot, take);
            }
        }
        return null;
    }

    public ItemStack performItemInput(InventoryHolder dest, ItemStack served, RetrievePolicy retrieve, InputPolicy input) {

        if (retrieve == RetrievePolicy.NORMAL && input == InputPolicy.NORMAL) {
            // returns the count that didn't fit after adding to inventory
            return dest.getInventory().addItem(served).values().stream().findFirst().orElse(null);
        }
        int leftovers = served.getAmount();

        int size = dest.getInventory().getSize();
        Iterator<Integer> slots;

        // try to complete existing stacks in the inventory...
        if (leftovers > 0 && input != InputPolicy.MAX_ONE) {

            slots = retrieve == RetrievePolicy.RANDOM
                    ? new RandomPermuteIterator(0, size)
                    : IntStream.range(0, size).iterator();

            while (slots.hasNext() && leftovers > 0) {
                int slot = slots.next();
                if (retrieve == RetrievePolicy.REVERSED) {
                    slot = size - 1 - slot;
                }
                ItemStack current = dest.getInventory().getItem(slot);
                if (current != null && served.isSimilar(current)) {

                    int total = served.getAmount() + current.getAmount();
                    int overflow = Math.max(0, total - served.getMaxStackSize());

                    total -= overflow;
                    leftovers -= (total - current.getAmount());

                    current.setAmount(total);
                    dest.getInventory().setItem(slot, current);
                }
            }
        }
        // try to begin a new stack in the inventory...
        if (leftovers > 0 && input != InputPolicy.LOCK_EMPTY) {

            slots = retrieve == RetrievePolicy.RANDOM
                    ? new RandomPermuteIterator(0, size)
                    : IntStream.range(0, size).iterator();

            while (slots.hasNext() && leftovers > 0) {
                int slot = slots.next();
                if (retrieve == RetrievePolicy.REVERSED) {
                    slot = size - 1 - slot;
                }
                ItemStack current = dest.getInventory().getItem(slot);
                if (current == null || current.getAmount() < 1) {

                    dest.getInventory().setItem(slot, served);
                    leftovers = 0;
                }
            }
        }
        if (leftovers == 0) {
            return null;
        }
        else {
            ItemStack leftover = served.clone();
            leftover.setAmount(leftovers);
            return leftover;
        }
    }

    public void performItemRecall(InvSlot recall, InventoryHolder source) {

        System.out.println("RECALLING " + recall.getItem() + " TO SLOT " + recall.getSlot());

        source.getInventory().setItem(recall.getSlot(), recall.getItem());

        System.out.println("++++++++++++++");
    }
}

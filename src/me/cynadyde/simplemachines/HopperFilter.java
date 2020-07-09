package me.cynadyde.simplemachines;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.IntStream;

public class HopperFilter implements Listener {

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

    public static class InventoryMovePair {

        private final InventoryHolder source;
        private final InventoryHolder dest;

        public InventoryMovePair(InventoryHolder source, InventoryHolder dest) {
            this.source = source;
            this.dest = dest;
        }

        public InventoryHolder getSource() {
            return source;
        }

        public InventoryHolder getDest() {
            return dest;
        }

        private boolean equalsSource(InventoryMovePair that) {
            if (this.source.equals(that.source)) {
                return true;
            }
            else if (this.source instanceof BlockState && that.source instanceof BlockState) {
                if (((BlockState) this.source).isPlaced() && ((BlockState) that.source).isPlaced()) {
                    return ((BlockState) this.source).getBlock().equals(((BlockState) that.source).getBlock());
                }
            }
            else if (this.source instanceof Entity && that.source instanceof Entity) {
                return ((Entity) this.source).getEntityId() == ((Entity) that.source).getEntityId();
            }
            return false;
        }

        private boolean equalsDest(InventoryMovePair that) {
            if (this.dest.equals(that.dest)) {
                return true;
            }
            else if (this.dest instanceof BlockState && that.dest instanceof BlockState) {
                if (((BlockState) this.dest).isPlaced() && ((BlockState) that.dest).isPlaced()) {
                    return ((BlockState) this.dest).getBlock().equals(((BlockState) that.dest).getBlock());
                }
            }
            else if (this.dest instanceof Entity && that.dest instanceof Entity) {
                return ((Entity) this.dest).getEntityId() == ((Entity) that.dest).getEntityId();
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InventoryMovePair that = (InventoryMovePair) o;
            return this.equalsSource(that) && this.equalsDest(that);
        }

        @Override
        public int hashCode() {
            Object s, d;
            if (source instanceof BlockState && ((BlockState) source).isPlaced()) {
                s = ((BlockState) source).getBlock();
            }
            else if (source instanceof Entity) {
                s = ((Entity) source).getEntityId();
            }
            else {
                s = source;
            }
            if (dest instanceof BlockState && ((BlockState) dest).isPlaced()) {
                d = ((BlockState) dest).getBlock();
            }
            else if (dest instanceof Entity) {
                d = ((Entity) dest).getEntityId();
            }
            else {
                d = dest;
            }
            return Objects.hash(s, d);
        }
    }

    private final SimpleMachinesPlugin plugin;
    private final Map<InventoryMovePair, Integer> invMoveAttempts;

    public HopperFilter(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
        this.invMoveAttempts = new HashMap<>();
    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
//
//        if (event.isCancelled()) return;
//        boolean takeover = false;
//
//        RetrievePolicy retrieve = RetrievePolicy.NORMAL;
//        InputPolicy input = InputPolicy.NORMAL;
//
//        InventoryHolder dest = event.getInventory().getHolder();
//        if (dest instanceof Container) {
//            Block destBlock = ((Container) dest).getBlock();
//
//            for (BlockFace face : FACES) {
//                switch (destBlock.getRelative(face).getType()) {
//                    case SPRUCE_TRAPDOOR:
//                        input = InputPolicy.MAX_ONE;
//                        takeover = true;
//                        break;
//                    case BIRCH_TRAPDOOR:
//                        input = InputPolicy.LOCK_EMPTY;
//                        takeover = true;
//                        break;
//                    case ACACIA_TRAPDOOR:
//                        retrieve = RetrievePolicy.REVERSED;
//                        takeover = true;
//                        break;
//                    case WARPED_TRAPDOOR:
//                        retrieve = RetrievePolicy.RANDOM;
//                        takeover = true;
//                        break;
//                }
//            }
//        }
//        if (takeover) {
//
//            final Item s = event.getItem();
//            final InventoryHolder d = event.getInventory().getHolder();
//            final RetrievePolicy r = retrieve;
//            final InputPolicy i = input;
//
//            /* cancel the event despite our monitor priority so that
//               we can mimic the result of the event (with more control) */
//            event.setCancelled(true);
//            plugin.getServer().getScheduler().runTaskLater(
//                    plugin, () -> performTransfer(s, d, r, i), 0L);
//        }
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMoveItemFirst(InventoryMoveItemEvent event) {

        InventoryMovePair movePair = new InventoryMovePair(
                event.getSource().getHolder(),
                event.getDestination().getHolder());

        Integer remaining = invMoveAttempts.get(movePair);
        if (remaining != null) {
            if (remaining > 0) {
                invMoveAttempts.put(movePair, remaining - 1);
                event.setCancelled(true);
            }
            else {
                invMoveAttempts.remove(movePair);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMoveItemLast(InventoryMoveItemEvent event) {

        if (event.isCancelled()) return;
        boolean takeover = false;

        InventoryHolder source = event.getSource().getHolder();
        InventoryHolder dest = event.getSource().getHolder();

        if (source == null || dest == null) return;

        ServePolicy serve = ServePolicy.NORMAL;
        OutputPolicy output = OutputPolicy.NORMAL;
        RetrievePolicy retrieve = RetrievePolicy.NORMAL;
        InputPolicy input = InputPolicy.NORMAL;

        World world = null;

        if (event.getSource().getHolder() instanceof Container) {
            Block from = ((Container) event.getSource().getHolder()).getBlock();
            world = from.getWorld();

            for (BlockFace face : FACES) {
                switch (from.getRelative(face).getType()) {
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
            Block to = ((Container) event.getDestination().getHolder()).getBlock();
            world = to.getWorld();

            for (BlockFace face : FACES) {
                switch (to.getRelative(face).getType()) {
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

            InventoryMovePair movePair = new InventoryMovePair(
                    event.getSource().getHolder(),
                    event.getDestination().getHolder());

            invMoveAttempts.put(movePair,
                    Bukkit.spigot().getConfig().getInt("world-settings." + world.getName() + ".ticks-per.hopper-transfer",
                            Bukkit.spigot().getConfig().getInt("world-settings.default.ticks-per.hopper-transfer", 8)) / 8 - 1);

            // item amount (set to spigot world config hopper take amount)
            int a = Bukkit.spigot().getConfig().getInt("world-settings." + world.getName() + ".hopper-amount",
                    Bukkit.spigot().getConfig().getInt("world-settings.default.hopper-amount", 1));
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
    }

    public void performTransfer(InventoryHolder source, InventoryHolder dest, int amount,
            ServePolicy serve, OutputPolicy output, RetrievePolicy retrieve, InputPolicy input) {

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
                // System.out.println("SERVE: " + serve);
                // System.out.println("OUTPUT: " + output);
                // System.out.println("RETRIEVE: " + retrieve);
                // System.out.println("INPUT: " + input);

                if (isTargeted(source, dest)) {
                    System.out.println("TAKING " + take + " FROM SLOT " + slot + " LEAVING " + item);
                }

                source.getInventory().setItem(slot, item);
                InvSlot served = new InvSlot(slot, take);

                ItemStack leftover = performItemInput(dest, served.getItem(), retrieve, input);
                if (leftover == null) {
                    break;
                }
                else {
                    performItemRecall(new InvSlot(served.getSlot(), leftover), source);
                }
                if (isTargeted(source, dest)) {
                    System.out.println("++++++++++++++");
                }
            }
        }
    }

    public void performTransfer(Item item, InventoryHolder dest, RetrievePolicy retrieve, InputPolicy input) {

        ItemStack leftover = performItemInput(dest, item.getItemStack(), retrieve, input);
        if (leftover == null) {
            item.remove();
        }
        else {
            item.setItemStack(leftover);
        }
    }

    public ItemStack performItemInput(InventoryHolder dest, ItemStack served, RetrievePolicy retrieve, InputPolicy input) {

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

                    if (isTargeted(dest)) {
                        System.out.println("FOUND FREE SPACE FOR " + served + " AT SLOT " + slot + " WITH " + current
                                + " (" + total + " total and " + leftovers + " leftover)");
                    }

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

                    if (isTargeted(dest)) {
                        System.out.println("FOUND EMPTY SPACE FOR " + served + " AT SLOT " + slot);
                    }

                    dest.getInventory().setItem(slot, served);
                    leftovers = 0;
                }
            }
        }
        if (leftovers == 0) {
            if (isTargeted(dest)) {
                System.out.println("NOTHING LEFT OVER!");
            }
            return null;
        }
        else {
            ItemStack leftover = served.clone();
            leftover.setAmount(leftovers);
            return leftover;
        }
    }

    public void performItemRecall(InvSlot recall, InventoryHolder source) {

        if (isTargeted(source)) {
            System.out.println("RECALLING " + recall.getItem() + " TO SLOT " + recall.getSlot());
        }

        ItemStack current = source.getInventory().getItem(recall.getSlot());
        ItemStack incoming = recall.getItem().clone();
        if (current != null) {
            // if something's already there, add recall amount to the current amount
            // anything over max stack size will just be destroyed
            incoming.setAmount(incoming.getAmount() + current.getAmount());
        }
        source.getInventory().setItem(recall.getSlot(), incoming);
    }

    private boolean isTargeted(InventoryHolder... holders) {
        for (InventoryHolder holder : holders) {
            if (holder instanceof BlockState) {
                if (((BlockState) holder).isPlaced()) {
                    if (((BlockState) holder).getBlock().equals(plugin.target)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

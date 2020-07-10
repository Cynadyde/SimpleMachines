package me.cynadyde.simplemachines;

import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class HopperFilter implements Listener {

    /**
     * The block faces that a hopper filter can be attached to.
     */
    public static final List<BlockFace> FACES = Collections.unmodifiableList(Arrays.asList(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));

    /**
     * The order to search for an item to take out when transferring.
     */
    public enum ServePolicy {NORMAL, REVERSED, RANDOM}

    /**
     * The order to search for a slot to put a transferred item into.
     */
    public enum RetrievePolicy {NORMAL, REVERSED, RANDOM}

    /**
     * The rules when choosing a slot to have an item transferred in.
     */
    public enum InputPolicy {NORMAL, MAX_ONE, LOCK_EMPTY}

    /**
     * The rules when choosing a slot to have an item transferred out.
     */
    public enum OutputPolicy {NORMAL, MIN_ONE}

    private final SimpleMachinesPlugin plugin;
    private final ItemStack DUMMY;

    public HopperFilter(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
        this.DUMMY = new ItemStack(Material.STONE, 1);

        ItemMeta meta = DUMMY.getItemMeta();
        assert meta != null;
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "dump"),
                PersistentDataType.BYTE,
                (byte) 0);
        DUMMY.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {

        if (event.isCancelled()) return;

        InventoryHolder dest = event.getInventory().getHolder();

        if (dest == null) return;
        boolean takeover = false;

        RetrievePolicy retrieve = RetrievePolicy.NORMAL;
        InputPolicy input = InputPolicy.NORMAL;

        //noinspection DuplicatedCode
        if (dest instanceof BlockState) {
            Block to = ((BlockState) dest).getBlock();

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
            final Item s = event.getItem();
            final InventoryHolder d = dest;
            final RetrievePolicy r = retrieve;
            final InputPolicy i = input;

            /* cancel and mimic the event with all the needed control. */
            event.setCancelled(true);
            Runnable task = () -> performItemTransfer(s, d, r, i);
            plugin.getServer().getScheduler().runTaskLater(plugin, task, 0L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {

        if (event.isCancelled()) return;

        final InventoryHolder source = event.getSource().getHolder();
        final InventoryHolder dest = event.getDestination().getHolder();

        if (source == null || dest == null) return;
        boolean takeover = false;

        ServePolicy serve = ServePolicy.NORMAL;
        OutputPolicy output = OutputPolicy.NORMAL;
        RetrievePolicy retrieve = RetrievePolicy.NORMAL;
        InputPolicy input = InputPolicy.NORMAL;

        if (source instanceof BlockState) {
            Block from = ((BlockState) source).getBlock();

            for (BlockFace face : FACES) {
                switch (from.getRelative(face).getType()) {
                    case OAK_TRAPDOOR:
                        // unused...
                        break;
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
        //noinspection DuplicatedCode
        if (dest instanceof BlockState) {
            Block to = ((BlockState) dest).getBlock();

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

            /* by transferring a dummy item instead of out-right canceling the event,
               we trick craft bukkit into administering the proper hopper cooldown. */
            // event.setItem(new ItemStack(Material.AIR, 0));
            event.setItem(DUMMY);

            /* get the transfer amount from the spigot config since the event's item
               could just be a partial amount due to too few slot contents to take. */
            World world = null;
            if (source instanceof BlockState) world = ((BlockState) source).getWorld();
            if (source instanceof Entity) world = ((Entity) source).getWorld();
            final int amount = getHopperTransferAmount(world);

            // finalize the item transfer policies
            final ServePolicy s = serve;
            final OutputPolicy o = output;
            final RetrievePolicy r = retrieve;
            final InputPolicy i = input;

            /* now, mimic the event with all the needed control. */
            Runnable task = () -> performItemTransfer(source, dest, amount, s, o, r, i);
            plugin.getServer().getScheduler().runTaskLater(plugin, task, 0L);
        }
    }

    public void performItemTransfer(InventoryHolder source, InventoryHolder dest, int amount,
            ServePolicy serve, OutputPolicy output, RetrievePolicy retrieve, InputPolicy input) {

        //source.getInventory().remove(DUMMY);
        //dest.getInventory().remove(DUMMY);

        if (isTargeted(source, dest)) {
            System.out.println("++++++++++++++");
            System.out.println("SOURCE INV HOLDER: " + source);
            System.out.println("SERVE: " + serve);
            System.out.println("OUTPUT: " + output);
            System.out.println("DEST INV HOLDER: " + dest);
            System.out.println("RETRIEVE: " + retrieve);
            System.out.println("INPUT: " + input);
        }

        int size = source.getInventory().getSize();
        Iterator<Integer> slots = serve == ServePolicy.RANDOM
                ? new RandomPermuteIterator(0, size)
                : IntStream.range(0, size).iterator();

        while (slots.hasNext()) {

            int slot = slots.next();
            if (serve == ServePolicy.REVERSED) {
                slot = (size - 1) - slot;
            }
            ItemStack current = source.getInventory().getItem(slot);
            if (current != null && (output != OutputPolicy.MIN_ONE || current.getAmount() > 1 || current.getMaxStackSize() <= 1)) {

                int taken = Math.min(amount, current.getAmount());
                ItemStack transfer = current.clone();
                transfer.setAmount(taken);

                if (current.getAmount() > taken) {
                    current.setAmount(current.getAmount() - taken);
                }
                else {
                    current = null;
                }

                if (isTargeted(source, dest)) {
                    System.out.println("TAKING " + transfer + " FROM SLOT " + slot + " LEAVING " + current);
                }

                int leftover = performItemInput(dest, transfer, retrieve, input);

                if (leftover == 0) {
                    if (isTargeted(source)) {
                        System.out.println("NOTHING LEFT OVER!");
                    }
                    source.getInventory().setItem(slot, current);
                    break;
                }
                else {
                    if (isTargeted(source)) {
                        System.out.println("RECALLING " + leftover + " TO SLOT " + slot);
                    }
                    if (current != null) {
                        // anything over max stack size will just be destroyed
                        current.setAmount(current.getAmount() + leftover);
                    }
                    source.getInventory().setItem(slot, current);
                }
            }
        }
    }

    public void performItemTransfer(Item item, InventoryHolder dest, RetrievePolicy retrieve, InputPolicy input) {

        int leftover = performItemInput(dest, item.getItemStack(), retrieve, input);

        if (isTargeted(dest)) {
            System.out.println("ITEM ENTITY HAS " + leftover + " LEFTOVER");
        }
        if (leftover == 0) {
            item.remove();
        }
        else {
            ItemStack content = item.getItemStack();
            content.setAmount(leftover);
            item.setItemStack(content);
        }
    }

    public int performItemInput(InventoryHolder dest, ItemStack transfer, RetrievePolicy retrieve, InputPolicy input) {

        int leftovers = transfer.getAmount();

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
                if (current != null && transfer.isSimilar(current)) {

                    int total = transfer.getAmount() + current.getAmount();
                    int overflow = Math.max(0, total - transfer.getMaxStackSize());

                    total -= overflow;
                    leftovers -= (total - current.getAmount());

                    if (isTargeted(dest)) {
                        System.out.println("FOUND FREE SPACE FOR " + transfer + " AT SLOT " + slot + " WITH " + current
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
                if (current == null) {

                    if (isTargeted(dest)) {
                        System.out.println("FOUND EMPTY SPACE FOR " + transfer + " AT SLOT " + slot);
                    }

                    dest.getInventory().setItem(slot, transfer);
                    leftovers = 0;
                }
            }
        }
        return leftovers;
    }

    public int getHopperTransferAmount(World world) {
        int amount = 1;
        if (world != null) {
            Configuration spigotConfig = plugin.getServer().spigot().getConfig();
            ConfigurationSection worldsSection = spigotConfig.getConfigurationSection("world-settings");
            if (worldsSection != null) {
                ConfigurationSection thisWorldConfig = worldsSection.getConfigurationSection(world.getName());
                if (thisWorldConfig != null) {
                    amount = Math.max(amount, thisWorldConfig.getInt("hopper-amount", 1));
                }
                else {
                    ConfigurationSection defaultWorldConfig = worldsSection.getConfigurationSection("default");
                    if (defaultWorldConfig != null) {
                        amount = Math.max(amount, defaultWorldConfig.getInt("hopper-amount", 1));
                    }
                }
            }
        }
        return amount;
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

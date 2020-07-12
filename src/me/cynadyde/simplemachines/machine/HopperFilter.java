package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import me.cynadyde.simplemachines.transfer.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.SpigotWorldConfig;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.stream.IntStream;

public class HopperFilter implements Listener {

    private final SimpleMachinesPlugin plugin;
    private InventoryMoveItemEvent lastSpammedInvMoveItemEvent;

    private Field entityField; // the OBC hook into NMS
    private Field entityWorldField; // the NMS world
    private Field tileEntityField; // the OBC hook into NMS
    private Field tileEntityWorldField; // the NMS world
    private Field worldSpigotConfigField; // the NMS world's OBC spigot config
    private Method tileEntityHopperSetCooldownMethod;

    public HopperFilter(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
        this.lastSpammedInvMoveItemEvent = null;

        try {
            // get necessary classes depending on current MC version in use

            String version = plugin.getServer().getClass().getPackage().getName().split("\\.")[3];

            Class<?> craftEntityClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftEntity");
            Class<?> craftBlockEntityStateClass = Class.forName("org.bukkit.craftbukkit." + version + ".block.CraftBlockEntityState");
            Class<?> nmsWorldClass = Class.forName("net.minecraft.server." + version + ".World");
            Class<?> nmsEntityClass = Class.forName("net.minecraft.server." + version + ".Entity");
            Class<?> nmsTileEntityClass = Class.forName("net.minecraft.server." + version + ".TileEntity");
            Class<?> nmsTileEntityHopperClass = Class.forName("net.minecraft.server." + version + ".TileEntityHopper");

            // get the needed fields and methods

            entityField = craftEntityClass.getDeclaredField("entity");
            entityWorldField = nmsEntityClass.getDeclaredField("world");
            tileEntityField = craftBlockEntityStateClass.getDeclaredField("tileEntity");
            tileEntityWorldField = nmsTileEntityClass.getDeclaredField("world");
            worldSpigotConfigField = nmsWorldClass.getField("spigotConfig");
            tileEntityHopperSetCooldownMethod = nmsTileEntityHopperClass.getDeclaredMethod("setCooldown", int.class);

            // make sure the fields and methods are accessible

            entityField.setAccessible(true);
            entityWorldField.setAccessible(true);
            tileEntityField.setAccessible(true);
            tileEntityWorldField.setAccessible(true);
            worldSpigotConfigField.setAccessible(true);
            tileEntityHopperSetCooldownMethod.setAccessible(true);
        }
        catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException ex) {
            plugin.getLogger().severe("could not perform reflection: " + ex.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {

        if (event.isCancelled()) return;

        final Inventory dest = event.getInventory();
        TransferDestPolicy destPolicy = TransferDestPolicy.ofInventory(dest);

        boolean takeover = destPolicy.isNonNormal();

        if (takeover) {
            final Item s = event.getItem();
            final RetrievePolicy r = destPolicy.RETRIEVE;
            final InputPolicy i = destPolicy.INPUT;

            /* cancel and mimic the event with all the needed control. */
            event.setCancelled(true);
            Runnable task = () -> performItemTransfer(s, dest, r, i);
            plugin.getServer().getScheduler().runTaskLater(plugin, task, 0L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpammedInventoryMoveItem(InventoryMoveItemEvent event) {

        /* prevent hoppers from pulling items from another container
               more than once in a row due to event cancellation. */
        if (event.getDestination() == event.getInitiator()) {

            InventoryMoveItemEvent prevEvent = lastSpammedInvMoveItemEvent;
            lastSpammedInvMoveItemEvent = event;

            if (prevEvent != null
                    && event.getSource().equals(prevEvent.getSource())
                    && event.getDestination().equals(prevEvent.getDestination())) {

                event.setCancelled(true);
            }
            else {
                final InventoryHolder dest = event.getDestination().getHolder();

                Runnable task = () -> {
                    lastSpammedInvMoveItemEvent = null;
                    if (dest instanceof Hopper) {
                        int interval = getHopperTransferInterval(dest);
                        setHopperCooldown((Hopper) dest, interval);
                    }
                };
                /* all the spammed events are fired within a single loop.
                   this task will run after that. */
                plugin.getServer().getScheduler().runTaskLater(plugin, task, 0L);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {

        if (event.isCancelled()) return;

        final Inventory source = event.getSource();
        final Inventory dest = event.getDestination();

        TransferSourcePolicy sourcePolicy = TransferSourcePolicy.ofInventory(source);
        TransferDestPolicy destPolicy = TransferDestPolicy.ofInventory(dest);

        boolean takeover = sourcePolicy.isNonNormal() || destPolicy.isNonNormal();

        if (takeover) {
            event.setCancelled(true);

            /* get the transfer amount from the spigot config since the event's item
               could just be a partial amount due to too few slot contents to take. */
            final int amount = getHopperTransferAmount(source.getHolder());

            // finalize the item transfer policies
            final ServePolicy s = sourcePolicy.SERVE;
            final OutputPolicy o = sourcePolicy.OUTPUT;
            final RetrievePolicy r = destPolicy.RETRIEVE;
            final InputPolicy i = destPolicy.INPUT;

            /* now, mimic the event with all the needed control. */
            Runnable task = () -> performItemTransfer(source, dest, amount, s, o, r, i);
            plugin.getServer().getScheduler().runTaskLater(plugin, task, 0L);
        }
    }

    public void performItemTransfer(Inventory source, Inventory dest, int amount,
            ServePolicy serve, OutputPolicy output, RetrievePolicy retrieve, InputPolicy input) {

        if (isTargeted(source, dest)) {
            System.out.println("++++++++++++++");
            System.out.println("SOURCE " + source.getType() + " has Serve." + serve + " and Output." + output);
            System.out.println("DEST " + dest.getType() + " has Retrieve." + retrieve + " and Input." + input);
        }

        int size = source.getSize();
        Iterator<Integer> slots = serve == ServePolicy.RANDOM
                ? new RandomPermuteIterator(0, size)
                : IntStream.range(0, size).iterator();

        while (slots.hasNext()) {

            int slot = slots.next();
            if (serve == ServePolicy.REVERSED) {
                slot = (size - 1) - slot;
            }
            ItemStack current = source.getItem(slot);
            if (current != null && (output != OutputPolicy.MIN_ONE || current.getAmount() > 1 || current.getMaxStackSize() <= 1)) {

                ItemStack transfer = current.clone();

                int taken = Math.min(amount, current.getAmount());
                transfer.setAmount(taken);

                if (isTargeted(source, dest)) {
                    System.out.println("TAKING " + transfer + " FROM SOURCE SLOT " + slot + " LEAVING " + current);
                }

                int leftover = performItemInput(source, dest, transfer, retrieve, input);

                // anything over max stack size (due to leftover) will just be destroyed
                current.setAmount(current.getAmount() - taken + leftover);
                source.setItem(slot, current);

                if (source.getHolder() instanceof Hopper) {
                    setHopperCooldown((Hopper) source.getHolder(), getHopperCheckInterval(source.getHolder()));
                }
                if (leftover == 0) {
                    break;
                }
                else {
                    if (isTargeted(source, dest)) {
                        System.out.println("RECALLING " + leftover + " TO SOURCE SLOT " + slot + " TO MAKE " + current);
                    }
                }
            }
        }
        if (isTargeted(source, dest)) {
            System.out.println("--------------");
        }
    }

    public void performItemTransfer(Item item, Inventory dest, RetrievePolicy retrieve, InputPolicy input) {

        if (isTargeted(dest)) {
            System.out.println("++++++++++++++");
            System.out.println("SOURCE ITEM ENTITY");
            System.out.println("DEST " + dest.getType() + " has Retrieve." + retrieve + " and Input." + input);
        }

        int leftover = performItemInput(dest, dest, item.getItemStack(), retrieve, input);

        if (isTargeted(dest)) {
            System.out.println("ITEM ENTITY HAS " + leftover + " REMAINING");
        }
        if (leftover == 0) {
            item.remove();
        }
        else {
            ItemStack content = item.getItemStack();
            content.setAmount(leftover);
            item.setItemStack(content);
        }
        if (isTargeted(dest)) {
            System.out.println("--------------");
        }
    }

    public int performItemInput(Inventory source, Inventory dest, ItemStack transfer, RetrievePolicy retrieve, InputPolicy input) {

        int leftovers = transfer.getAmount();

        int size = dest.getSize();
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
                ItemStack current = dest.getItem(slot);
                if (current != null && transfer.isSimilar(current)) {

                    int total = transfer.getAmount() + current.getAmount();
                    int overflow = Math.max(0, total - transfer.getMaxStackSize());

                    total -= overflow;
                    leftovers -= (total - current.getAmount());

                    if (isTargeted(source, dest)) {
                        System.out.println("FOUND SPACE FOR " + transfer + " AT DEST SLOT " + slot + " WITH " + current);
                    }

                    current.setAmount(total);
                    dest.setItem(slot, current);
                }
            }
        }
        // otherwise try to begin a new stack in the inventory...
        if (leftovers > 0 && input != InputPolicy.LOCK_EMPTY) {

            slots = retrieve == RetrievePolicy.RANDOM
                    ? new RandomPermuteIterator(0, size)
                    : IntStream.range(0, size).iterator();

            while (slots.hasNext() && leftovers > 0) {

                int slot = slots.next();
                if (retrieve == RetrievePolicy.REVERSED) {
                    slot = size - 1 - slot;
                }
                ItemStack current = dest.getItem(slot);
                if (current == null) {

                    if (isTargeted(source, dest)) {
                        System.out.println("FOUND SPACE FOR " + transfer + " AT DEST SLOT " + slot);
                    }

                    dest.setItem(slot, transfer);
                    leftovers = 0;
                }
            }
        }
        return leftovers;
    }

    private SpigotWorldConfig getWorldConfigFor(InventoryHolder holder) {
        try {
            if (holder instanceof BlockState) {
                return (SpigotWorldConfig) worldSpigotConfigField.get(tileEntityWorldField.get(tileEntityField.get(holder)));
            }
            else if (holder instanceof Entity) {
                return (SpigotWorldConfig) worldSpigotConfigField.get(entityWorldField.get(entityField.get(holder)));
            }
        }
        catch (IllegalAccessException | NullPointerException ex) {
            plugin.getLogger().severe("could not perform InventoryHolder reflection: " + ex.getMessage());
        }
        return null;
    }

    private int getHopperTransferAmount(InventoryHolder holder) {
        SpigotWorldConfig config = getWorldConfigFor(holder);
        if (config != null) {
            return config.hopperAmount;
        }
        return 1;
    }

    private int getHopperTransferInterval(InventoryHolder holder) {
        SpigotWorldConfig config = getWorldConfigFor(holder);
        if (config != null) {
            return config.hopperTransfer;
        }
        return 8;
    }

    private int getHopperCheckInterval(InventoryHolder holder) {
        SpigotWorldConfig config = getWorldConfigFor(holder);
        if (config != null) {
            return config.hopperCheck;
        }
        return 1;
    }

    private void setHopperCooldown(Hopper hopper, int cooldown) {
        try {
            tileEntityHopperSetCooldownMethod.invoke(tileEntityField.get(hopper), cooldown);
        }
        catch (IllegalAccessException | InvocationTargetException | NullPointerException ex) {
            plugin.getLogger().severe("could not perform Hopper reflection: " + ex.getMessage());
        }
    }

    private boolean isTargeted(Inventory... inventories) {
        for (Inventory inv : inventories) {
            if (inv != null && inv.getHolder() != null) {
                if (inv.getHolder() instanceof BlockState) {
                    if (((BlockState) inv.getHolder()).isPlaced()) {
                        if (((BlockState) inv.getHolder()).getBlock().equals(plugin.target)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}

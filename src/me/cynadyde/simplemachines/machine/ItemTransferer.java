package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.transfer.*;
import me.cynadyde.simplemachines.util.PluginKey;
import me.cynadyde.simplemachines.util.ReflectiveUtils;
import me.cynadyde.simplemachines.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.spigotmc.SpigotWorldConfig;

import java.util.Iterator;

public class ItemTransferer implements Listener {

    // TODO hoppers suck in liquids above it (policy permitting)

    private final SimpleMachinesPlugin plugin;
    private InventoryMoveItemEvent lastSpammedInvMoveItemEvent;

    public ItemTransferer(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
        this.lastSpammedInvMoveItemEvent = null;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // anti grief plugins should have been able to cancel the event by now
        BlockState state = event.getBlock().getState();
        if (state instanceof Container) {
            TransferScheme scheme = TransferScheme.ofHolder((Container) state);

            if (scheme.isNonNormal()) {
                Location dest = state.getLocation().add(0.5, 0.5, 0.5);
                for (TransferPolicy policy : scheme.getPolicies()) {
                    if (!policy.getToken().isAir()) {
                        state.getWorld().dropItemNaturally(dest, new ItemStack(policy.getToken()));
                    }
                }
                PersistentDataContainer pdc = ((Container) state).getPersistentDataContainer();
                pdc.remove(PluginKey.RECEIVE_POLICY.get());
                pdc.remove(PluginKey.INPUT_POLICY.get());
                pdc.remove(PluginKey.SERVE_POLICY.get());
                pdc.remove(PluginKey.OUTPUT_POLICY.get());
                pdc.remove(PluginKey.LIQUIDS_POLICY.get());

                state.update();
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // click filled bucket to container with empty bucket to deposit liquids
        // anti grief plugins should have been able to cancel the event by now
        if (event.getItem() != null) {
            ItemStack item = event.getItem();
            if (item.getAmount() == 1 && Utils.FILLED_BUCKETS.contains(item.getType())) {
                if (event.getClickedBlock() != null) {
                    BlockState state = event.getClickedBlock().getState();
                    if (state instanceof Container) {
                        TransferScheme scheme = TransferScheme.ofHolder((Container) state);
                        if (scheme.LIQUIDS == LiquidsPolicy.POUR_INTO) {

                            Inventory dest = ((Container) state).getInventory();
                            int leftovers = performItemInput(dest, item, scheme);
                            if (leftovers == 0) {
                                EntityEquipment equipment = event.getPlayer().getEquipment();
                                if (equipment != null) {
                                    equipment.setItemInMainHand(new ItemStack(Material.BUCKET, 1));
                                }
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        final Inventory dest = event.getInventory();
        final TransferScheme scheme = TransferScheme.ofHolder(dest.getHolder());

        boolean takeover = scheme.isNonNormal();

        if (takeover) {
            final Item s = event.getItem();

            /* cancel and mimic the event with all the needed control. */
            event.setCancelled(true); // lets hope other plugins don't un-cancel!
            Runnable task = () -> performItemTransfer(s, dest, scheme);
            plugin.getServer().getScheduler().runTaskLater(plugin, task, 0L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpammedInventoryMoveItem(InventoryMoveItemEvent event) {
        /* prevent hoppers from immediately trying to pull items from
               another container again when the event is cancelled. */
        if (event.getDestination() == event.getInitiator()) {

            InventoryMoveItemEvent prevEvent = lastSpammedInvMoveItemEvent;
            lastSpammedInvMoveItemEvent = event;

            // this is run after the first...
            if (prevEvent != null
                    && event.getSource().equals(prevEvent.getSource())
                    && event.getDestination().equals(prevEvent.getDestination())) {

                event.setCancelled(true); // lets hope other plugins don't un-cancel!
            }
            // this is run on the first...
            else {
                final InventoryHolder dest = event.getDestination().getHolder();

                Runnable task = () -> {
                    lastSpammedInvMoveItemEvent = null;
                    if (dest instanceof Hopper) {
                        int interval = getHopperTransferInterval(dest);
                        ReflectiveUtils.setHopperCooldown((Hopper) dest, interval);
                    }
                };
                /* this is run once all the spammed events have been fired for the tick. */
                plugin.getServer().getScheduler().runTaskLater(plugin, task, 0L);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        final Inventory source = event.getSource();
        final Inventory dest = event.getDestination();
        final TransferScheme scheme = TransferScheme.ofTransaction(source, dest);

        boolean takeover = scheme.isNonNormal();

        if (takeover) {
            event.setCancelled(true); // lets hope other plugins don't un-cancel!

            /* get the transfer amount from the spigot config since the event's item
               could just be a partial amount due to too few slot contents to take. */
            final int amount = getHopperTransferAmount(source.getHolder());
            final boolean pushed = event.getInitiator() == event.getSource();

            /* now, mimic the event with all the needed control. */
            Runnable task = () -> performItemTransfer(source, dest, amount, pushed, scheme);
            plugin.getServer().getScheduler().runTaskLater(plugin, task, 0L);
        }
    }

    public void performItemTransfer(Inventory source, Inventory dest, int amount, boolean pushed, TransferScheme scheme) {

        if (isTargeted(source, dest)) {
            System.out.println("++++++++++++++");
            System.out.println("SOURCE " + source.getType() + " has Serve." + scheme.SERVE + " and Output." + scheme.OUTPUT);
            System.out.println("DEST " + dest.getType() + " has Receive." + scheme.RECEIVE + " and Input." + scheme.INPUT);
        }

        Iterator<Integer> slots = scheme.SERVE.getIterator(source.getHolder());
        while (slots.hasNext()) {
            int slot = slots.next();

            ItemStack current = source.getItem(slot);
            if (scheme.OUTPUT.testSlot(current) && !scheme.LIQUIDS.isFillable(current)) {

                ItemStack transfer = current.clone();

                int taken = Math.min(amount, current.getAmount());
                transfer.setAmount(taken);

                if (isTargeted(source, dest)) {
                    System.out.println("TAKING " + transfer + " FROM SOURCE SLOT " + slot + " LEAVING " + current);
                }

                int leftover = performItemInput(dest, transfer, scheme);

                if (scheme.LIQUIDS.isDrainable(current) && leftover == 0) {
                    // the contents of the bucket are transferred, but not the bucket
                    current.setType(Material.BUCKET);
                }
                else {
                    // anything over max stack size (due to leftover) will just be destroyed
                    current.setAmount(current.getAmount() - taken + leftover);
                }
                source.setItem(slot, current);

                if (source.getHolder() instanceof Hopper) {
                    int cooldown = pushed
                            ? getHopperTransferInterval(source.getHolder())
                            : getHopperCheckInterval(source.getHolder());

                    ReflectiveUtils.setHopperCooldown((Hopper) source.getHolder(), cooldown);
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

    public void performItemTransfer(Item item, Inventory dest, TransferScheme scheme) {

        if (isTargeted(dest)) {
            System.out.println("++++++++++++++");
            System.out.println("SOURCE ITEM ENTITY");
            System.out.println("DEST " + dest.getType() + " has Receive." + scheme.RECEIVE + " and Input." + scheme.INPUT);
        }

        int leftover = performItemInput(dest, item.getItemStack(), scheme);

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

    public int performItemInput(Inventory dest, ItemStack transfer, TransferScheme scheme) {

        int leftovers = transfer.getAmount();
        Iterator<Integer> slots;

        // try to transfer liquids if applicable...
        if (scheme.LIQUIDS.isDrainable(transfer)) {

            slots = scheme.RECEIVE.getIterator(dest.getHolder());
            while (slots.hasNext() && leftovers > 0) {
                int slot = slots.next();

                ItemStack current = dest.getItem(slot);
                if (scheme.LIQUIDS.isFillable(current)) {
                    dest.setItem(slot, transfer);
                    leftovers--;
                }
            }
        }
        else {
            // try to complete existing stacks in the inventory...
            if (leftovers > 0 && scheme.INPUT != InputPolicy.TO_EMPTY) {

                slots = scheme.RECEIVE.getIterator(dest.getHolder());
                while (slots.hasNext() && leftovers > 0) {
                    int slot = slots.next();

                    ItemStack current = dest.getItem(slot);
                    if (current != null && transfer.isSimilar(current)) {

                        int total = transfer.getAmount() + current.getAmount();
                        int overflow = Math.max(0, total - transfer.getMaxStackSize());

                        total -= overflow;
                        leftovers -= (total - current.getAmount());

                        if (isTargeted(dest)) {
                            System.out.println("FOUND SPACE FOR " + transfer + " AT DEST SLOT " + slot + " WITH " + current);
                        }

                        current.setAmount(total);
                        dest.setItem(slot, current);
                    }
                }
            }
            // otherwise try to begin a new stack in the inventory...
            if (leftovers > 0 && scheme.INPUT != InputPolicy.TO_NONEMPTY) {

                slots = scheme.RECEIVE.getIterator(dest.getHolder());
                while (slots.hasNext() && leftovers > 0) {
                    int slot = slots.next();

                    ItemStack current = dest.getItem(slot);
                    if (current == null) {

                        if (isTargeted(dest)) {
                            System.out.println("FOUND SPACE FOR " + transfer + " AT DEST SLOT " + slot);
                        }

                        dest.setItem(slot, transfer);
                        leftovers = 0;
                    }
                }
            }
        }
        return leftovers;
    }

    private int getHopperTransferAmount(InventoryHolder holder) {
        SpigotWorldConfig config = ReflectiveUtils.getWorldConfigFor(holder);
        if (config != null) {
            return config.hopperAmount;
        }
        return 1;
    }

    private int getHopperTransferInterval(InventoryHolder holder) {
        SpigotWorldConfig config = ReflectiveUtils.getWorldConfigFor(holder);
        if (config != null) {
            return config.hopperTransfer;
        }
        return 8;
    }

    private int getHopperCheckInterval(InventoryHolder holder) {
        SpigotWorldConfig config = ReflectiveUtils.getWorldConfigFor(holder);
        if (config != null) {
            return config.hopperCheck;
        }
        return 1;
    }

    // debug purposes...
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
package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.transfer.InputPolicy;
import me.cynadyde.simplemachines.transfer.LiquidsPolicy;
import me.cynadyde.simplemachines.transfer.TransferPolicy;
import me.cynadyde.simplemachines.transfer.TransferScheme;
import me.cynadyde.simplemachines.util.ItemUtils;
import me.cynadyde.simplemachines.util.ReflectiveUtils;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.spigotmc.SpigotWorldConfig;

import java.util.Iterator;
import java.util.Objects;

public class ItemTransferer implements Listener {

    private final SimpleMachinesPlugin plugin;
    private InventoryMoveItemEvent lastSpammedInvMoveItemEvent;
    private final BukkitTask liquidAbsorbTask;

    public ItemTransferer(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
        this.lastSpammedInvMoveItemEvent = null;

        this.liquidAbsorbTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::onHopperTick, 0L, 4 * 20L);
    }

    public void cancelTasks() {
        this.liquidAbsorbTask.cancel();
    }

    public void onHopperTick() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState tile : chunk.getTileEntities()) {
                    if (tile instanceof Hopper) {

                        Block base = tile.getBlock().getRelative(BlockFace.UP);
                        if (base.isLiquid() || base.getBlockData() instanceof Waterlogged) {

                            TransferScheme scheme = TransferScheme.ofHolder((PersistentDataHolder) tile);
                            if (scheme.LIQUIDS == LiquidsPolicy.POUR_INTO) {

                                Inventory dest = ((Hopper) tile).getInventory();
                                performLiquidTransfer(base, dest, scheme);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        // anti grief plugins should have been able to cancel the event by now
        if (event.getBlockState() instanceof Container) {
            Container state = (Container) event.getBlockState();
            TransferScheme scheme = TransferScheme.ofHolder(state);
            if (scheme.isNonNormal()) {

                Item drop = null;
                for (Item entity : event.getItems()) {
                    ItemStack item = entity.getItemStack();
                    if (item.getType() == state.getType()) {
                        if (!state.getSnapshotInventory().contains(item)) {
                            drop = entity;
                            break;
                        }
                    }
                }
                if (drop != null) {
                    // all these assumptions can be made since item & state types are equal
                    ItemStack item = drop.getItemStack();
                    BlockStateMeta meta = Objects.requireNonNull((BlockStateMeta) item.getItemMeta());
                    Container dropState = (Container) meta.getBlockState();
                    scheme.applyTo(dropState);

                    meta.setBlockState(dropState);
                    item.setItemMeta(meta);
                }
                else {
                    Location dest = state.getLocation().add(0.5, 0.5, 0.5);
                    for (TransferPolicy policy : scheme.getPolicies()) {
                        if (!policy.getToken().isAir()) {
                            state.getWorld().dropItemNaturally(dest, new ItemStack(policy.getToken()));
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        BlockState placed = event.getBlockPlaced().getState();
        if (placed instanceof PersistentDataHolder) {
            PersistentDataContainer placedPdc = ((PersistentDataHolder) placed).getPersistentDataContainer();
            ItemMeta meta = event.getItemInHand().getItemMeta();
            if (meta instanceof BlockStateMeta) {
                BlockState held = ((BlockStateMeta) meta).getBlockState();
                if (held instanceof PersistentDataHolder) {
                    PersistentDataContainer heldPdc = ((PersistentDataHolder) held).getPersistentDataContainer();
                    for (NamespacedKey key : heldPdc.getKeys()) {
                        // TODO HOW????
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // click filled bucket to container with empty bucket to deposit liquids
        // anti grief plugins should have been able to cancel the event by now
        if (event.getItem() != null) {
            ItemStack item = event.getItem();
            if (item.getAmount() == 1 && ItemUtils.FILLED_BUCKETS.contains(item.getType())) {
                if (event.getClickedBlock() != null) {
                    BlockState state = event.getClickedBlock().getState();
                    if (state instanceof Container) {
                        TransferScheme scheme = TransferScheme.ofHolder((Container) state);
                        if (scheme.LIQUIDS == LiquidsPolicy.POUR_INTO) {

                            Inventory dest = ((Container) state).getInventory();
                            int leftovers = performInventoryInput(dest, item, scheme);
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
        final TransferScheme scheme = TransferScheme.ofInvHolder(dest.getHolder());

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
            Runnable task = () -> performInventoryTransfer(source, dest, amount, pushed, scheme);
            plugin.getServer().getScheduler().runTaskLater(plugin, task, 0L);
        }
    }

    public void performInventoryTransfer(Inventory source, Inventory dest, int amount, boolean pushed, TransferScheme scheme) {
        InventoryHolder holder = source.getHolder();
        Iterator<Integer> slots = scheme.SERVE.getIterator(holder);
        while (slots.hasNext()) {
            int slot = slots.next();

            ItemStack current = source.getItem(slot);
            if (scheme.OUTPUT.testSlot(current) && !scheme.LIQUIDS.isFillable(current)) {

                ItemStack transfer = current.clone();
                int taken = Math.min(amount, current.getAmount());
                transfer.setAmount(taken);

                int leftover = performInventoryInput(dest, transfer, scheme);

                if (scheme.LIQUIDS.isDrainable(current) && leftover == 0) {
                    // the contents of the bucket are transferred, but not the bucket
                    current.setType(Material.BUCKET);
                }
                else {
                    // anything over max stack size (due to leftover) will just be destroyed
                    current.setAmount(current.getAmount() - taken + leftover);
                }
                source.setItem(slot, current);

                if (holder instanceof Hopper) {
                    int cooldown = pushed
                            ? getHopperTransferInterval(holder)
                            : getHopperCheckInterval(holder);

                    ReflectiveUtils.setHopperCooldown((Hopper) holder, cooldown);
                }
                if (leftover == 0) {
                    break;
                }
            }
        }
    }

    public void performItemTransfer(Item item, Inventory dest, TransferScheme scheme) {
        int leftover = performInventoryInput(dest, item.getItemStack(), scheme);

        if (leftover == 0) {
            item.remove();
        }
        else {
            ItemStack content = item.getItemStack();
            content.setAmount(leftover);
            item.setItemStack(content);
        }
    }

    public void performLiquidTransfer(Block block, Inventory dest, TransferScheme scheme) {
        if (scheme.LIQUIDS != LiquidsPolicy.POUR_INTO) {
            return;
        }
        boolean isWater;
        if (isInWater(block)) {
            isWater = true;
        }
        else if (isInLava(block)) {
            isWater = false;
        }
        else {
            return;
        }
        Block source = null;
        Block seeker = block;
        int limit = block.getWorld().getMaxHeight() - 1;
        do {
            if (canDrain(seeker)) {
                source = seeker;
            }
            if (seeker.getY() >= limit) {
                break;
            }
            seeker = seeker.getRelative(BlockFace.UP, 1);
        }
        while (isWater ? isInWater(seeker) : isInLava(seeker));

        if (source != null) {
            ItemStack item = new ItemStack(isWater ? Material.WATER_BUCKET : Material.LAVA_BUCKET, 1);
            int leftover = performInventoryInput(dest, item, scheme);

            if (leftover == 0) {
                drainFluid(source);
                Sound sound = isWater ? Sound.ITEM_BUCKET_FILL : Sound.ITEM_BUCKET_FILL_LAVA;
                block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), sound, SoundCategory.BLOCKS, 1F, 1F);
            }
        }
    }

    public int performInventoryInput(Inventory dest, ItemStack transfer, TransferScheme scheme) {
        int leftovers = transfer.getAmount();

        InventoryHolder holder = dest.getHolder();
        Iterator<Integer> slots;

        // try to transfer liquids if applicable...
        if (scheme.LIQUIDS.isDrainable(transfer)) {

            slots = scheme.RECEIVE.getIterator(holder);
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

                slots = scheme.RECEIVE.getIterator(holder);
                while (slots.hasNext() && leftovers > 0) {
                    int slot = slots.next();

                    ItemStack current = dest.getItem(slot);
                    if (current != null && transfer.isSimilar(current)) {

                        int total = transfer.getAmount() + current.getAmount();
                        int overflow = Math.max(0, total - transfer.getMaxStackSize());

                        total -= overflow;
                        leftovers -= (total - current.getAmount());

                        current.setAmount(total);
                        dest.setItem(slot, current);
                    }
                }
            }
            // otherwise try to begin a new stack in the inventory...
            if (leftovers > 0 && scheme.INPUT != InputPolicy.TO_NONEMPTY) {

                slots = scheme.RECEIVE.getIterator(holder);
                while (slots.hasNext() && leftovers > 0) {
                    int slot = slots.next();

                    ItemStack current = dest.getItem(slot);
                    if (current == null) {

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

    private boolean isInWater(Block block) {
        if (block.getType() == Material.WATER) {
            return true;
        }
        BlockData data = block.getBlockData();
        if (data instanceof Waterlogged) {
            return ((Waterlogged) data).isWaterlogged();
        }
        return false;
    }

    private boolean isInLava(Block block) {
        return block.getType() == Material.LAVA;
    }

    private boolean canDrain(Block block) {
        BlockData data = block.getBlockData();
        if (block.isLiquid() && data instanceof Levelled) {
            return ((Levelled) data).getLevel() == 0;
        }
        else if (data instanceof Waterlogged) {
            return ((Waterlogged) data).isWaterlogged();
        }
        return false;
    }

    private void drainFluid(Block block) {
        if (block.isLiquid()) {
            block.setType(Material.AIR);
        }
        else {
            BlockData data = block.getBlockData();
            if (data instanceof Waterlogged) {
                ((Waterlogged) data).setWaterlogged(false);
                block.setBlockData(data);
            }
        }
    }
}

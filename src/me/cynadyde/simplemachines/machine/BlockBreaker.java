package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.util.ItemUtils;
import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import me.cynadyde.simplemachines.util.ReflectiveUtils;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dropper;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class BlockBreaker implements Listener {

    private final SimpleMachinesPlugin plugin;
    private final Map<Block, BukkitTask> jobs;

    private boolean animateMiningJob = true; // could be config driven
    private int durabilityCost = 4;

    public BlockBreaker(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
        this.jobs = new HashMap<>();
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        if (isBlockBreakerMachine(event.getBlock())) {
            event.setCancelled(true);

            final Block machine = event.getBlock();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> doBlockBreak(machine), 0L);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        InventoryHolder holder = event.getSource().getHolder();
        if (holder instanceof BlockState) {

            final Block machine = ((BlockState) holder).getBlock();
            if (isBlockBreakerMachine(machine)) {
                event.setCancelled(true);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> doBlockBreak(machine), 0L);
            }
        }
    }

    public boolean isBlockBreakerMachine(Block block) {
        return block.getType() == Material.DROPPER
                && block.getRelative(BlockFace.UP).getType() == Material.STONECUTTER;
    }

    public void doBlockBreak(Block machine) {
        if (jobs.containsKey(machine) || !isBlockBreakerMachine(machine)) {
            return;
        }
        // the target block must be something that can be mined
        final Block target = machine.getRelative(BlockFace.UP, 2);
        if (!isBreakable(target)) {
            return;
        }

        // find the next tool stored in the machine, if any...
        // a tool is required to break blocks
        final Dropper initMachine = (Dropper) machine.getState();
        final Integer slot = getRandomToolSlot(initMachine.getInventory(), target);
        if (slot == null) {
            return;
        }

        final BlockState initTarget = target.getState();
        final ItemStack tool = Objects.requireNonNull(initMachine.getInventory().getItem(slot));
        final int cost = durabilityCost; // durability cost for tool

        final boolean animate = animateMiningJob; // send block cracks and sounds to nearby players
        final int id = ItemUtils.RNG.nextInt(); // block break animation id for players' clients
        final Set<Player> receivers = new HashSet<>(); // players who've been sent the animation

        final int duration = getDestroyTime(target, tool); // total ticks needed to destroy block

        /* create a task that will continue mining the block each tick until
            either it is broken or the tool, machine, or block is disrupted. */
        jobs.put(target, new BukkitRunnable() {

            private int stage = 0; // current block break animation stage
            private int ticks = 0; // ticks elapsed

            @Override
            public void run() {
                if (!isMiningJobValid(machine, initTarget, tool, slot)) {
                    cancel();
                }
                else {
                    ticks += 1;
                    if (ticks >= duration) {
                        finishMiningJob(machine, initTarget, tool, slot, cost);
                        cancel();
                    }
                    else if (animate) {
                        int prevStage = stage;
                        stage = (int) Math.floor(10 * (ticks / (double) duration));
                        if (stage != prevStage) {
                            Vector origin = target.getLocation().add(0.5, 0.5, 0.5).toVector();
                            for (Player player : target.getWorld().getPlayers()) {
                                if (player.getLocation().toVector().isInSphere(origin, 32)) {
                                    ReflectiveUtils.updateBlockBreakAnimation(id, target, stage, player);
                                    receivers.add(player);
                                }
                            }
                        }
                        if ((ticks & 3) == 0) { // every 4 ticks (5x per second)
                            ReflectiveUtils.playBlockHitSound(target);
                        }
                    }
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                if (animate) {
                    for (Player player : receivers) {
                        if (player.isOnline()) {
                            ReflectiveUtils.updateBlockBreakAnimation(id, target, -1, player);
                        }
                    }
                    receivers.clear();
                }
                jobs.remove(target);
                super.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L));
    }

    private boolean isMiningJobValid(Block machine, BlockState target, ItemStack tool, int slot) {
        // if the machine was destroyed, this job is invalidated!
        if (isBlockBreakerMachine(machine) && target.isPlaced()) {
            Dropper dropper = (Dropper) machine.getState();

            // if the block being mined was changed, this job is invalidated!
            if (target.getBlock().getType() == target.getType()) {

                // if the tool was removed, this job is invalidated!
                ItemStack item = dropper.getInventory().getItem(slot);
                return tool.equals(item);
            }
        }
        return false;
    }

    private void finishMiningJob(Block machine, BlockState target, ItemStack tool, int slot, int cost) {
        // complete() is only called after check() - machine assumed to be a dropper
        Dropper dropper = (Dropper) machine.getState();
        ItemMeta meta = Objects.requireNonNull(tool.getItemMeta()); // tool item meta is Damageable

        Block targetBlock = target.getBlock();
        BlockState newState = targetBlock.getState();
        newState.setType(Material.AIR);

        BlockFormEvent event = new BlockFormEvent(targetBlock, newState);
        plugin.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {

            if (cost > 0) {
                int maxDamage = tool.getType().getMaxDurability();
                int curDamage = ((Damageable) meta).getDamage();
                int newDamage = cost;

                double chance = 1.0 / (meta.getEnchantLevel(Enchantment.DURABILITY) + 1);
                if (chance < 1.0) {
                    for (int i = 0; i < newDamage; i++) {
                        if (ItemUtils.RNG.nextDouble() >= chance) {
                            newDamage -= 1;
                        }
                    }
                }
                if (newDamage > 0) {
                    curDamage += newDamage;

                    if (curDamage <= maxDamage) {
                        ((Damageable) meta).setDamage(curDamage);
                        tool.setItemMeta(meta);
                        dropper.getInventory().setItem(slot, tool);
                    }
                    else {
                        dropper.getInventory().setItem(slot, null);
                        machine.getWorld().playSound(machine.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.12F);
                    }
                }
            }
            for (ItemStack drop : targetBlock.getDrops(tool)) {
                ItemUtils.dropFromDropper(dropper, drop);
            }
            target.getWorld().playEffect(target.getLocation().add(0.5, 0.5, 0.5), Effect.STEP_SOUND, target.getType());

            event.getNewState().update(true, true);
        }
    }

    public boolean isBreakable(Block block) {
        return !block.getType().isAir() && !block.isLiquid() && block.getType().getHardness() >= 0;
    }

    public Integer getRandomToolSlot(Inventory inv, Block toolTarget) {
        ItemStack[] contents = inv.getContents();
        Integer fallback = null;
        Iterator<Integer> iterator = new RandomPermuteIterator(0, contents.length);
        while (iterator.hasNext()) {
            int i = iterator.next();
            ItemStack item = contents[i];
            if (isTool(item)) {
                fallback = i;
                if (isHarvestedBy(toolTarget, item)) {
                    return i;
                }
            }
        }
        return fallback;
    }

    public boolean isTool(ItemStack tool) {
        return tool != null && ItemUtils.TOOLS.contains(tool.getType());
    }

    public boolean isHarvestedBy(Block block, ItemStack tool) {
        if (ReflectiveUtils.isSpecialToolRequired(block)) {
            return ReflectiveUtils.canBreakSpecialBlock(block, tool);
        }
        return true;
    }

    public int getDestroyTime(Block block, ItemStack tool) {
        if (!isBreakable(block)) {
            throw new IllegalArgumentException("block is not breakable");
        }
        double seconds = block.getType().getHardness() * (isHarvestedBy(block, tool) ? 1.5 : 5.0);
        double speed = ReflectiveUtils.getDestroySpeed(block, tool);
        if (speed > 1) {
            ItemMeta meta = tool.getItemMeta();
            if (meta != null) {
                int efficiency = meta.getEnchantLevel(Enchantment.DIG_SPEED);
                if (efficiency > 0) {
                    speed += Math.pow(efficiency, 2) + 1;
                }
            }
        }
        return (int) Math.ceil((seconds / speed) * 20);
    }

    public void cancelAllJobs() {
        for (BukkitTask task : jobs.values()) {
            task.cancel();
        }
        jobs.clear();
    }
}

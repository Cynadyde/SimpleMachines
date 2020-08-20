package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import me.cynadyde.simplemachines.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dropper;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class BlockBreaker implements Listener {

    private final SimpleMachinesPlugin plugin;
    private final Set<MiningJob> jobs;

    public BlockBreaker(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
        this.jobs = new HashSet<>();
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
    public void onItemSpawn(ItemSpawnEvent event) {
        Item drop = event.getEntity();
        Location pos = event.getLocation();

        for (MiningJob job : jobs) {
            if (job.getMachine().getWorld().equals(pos.getWorld())
                    && job.getRegion().contains(pos.toVector())) {

                Dropper machine = job.getMachine();
                if (isBlockBreakerMachine(machine.getBlock())) {

                    Utils.dropFromDropper(machine, drop.getItemStack());
                    event.setCancelled(true);
                }
                break;
            }
        }
    }

    public boolean isBlockBreakerMachine(Block block) {
        return block.getType() == Material.DROPPER
                && block.getRelative(BlockFace.UP).getType() == Material.STONECUTTER;
    }

    public void doBlockBreak(Block machine) {
        if (isBlockBreakerMachine(machine)) {

            Dropper dropper = (Dropper) machine.getState();
            Block target = machine.getRelative(BlockFace.UP, 2);
            if (isBreakable(target)) {

                // find the next tool stored in the dropper, if any...
                int slot = -1;
                ItemStack[] contents = dropper.getInventory().getContents();
                RandomPermuteIterator iterator = new RandomPermuteIterator(0, contents.length);
                while (iterator.hasNext()) {
                    int i = iterator.next();
                    ItemStack item = contents[i];
                    if (item != null && Utils.TOOLS.contains(item.getType())) {
                        slot = i;
                        break;
                    }
                }
                // a tool is required to break blocks...
                if (slot != -1) {
                    ItemStack tool = contents[slot];
                    ItemMeta meta = Objects.requireNonNull(tool.getItemMeta()); // tool item meta is Damageable

                    if (target.breakNaturally(tool)) {

                        // the mining job will scoop up any dropped items and have them machine-dispensed instead
                        final MiningJob job = new MiningJob(dropper, machine.getBoundingBox().expand(0.25));
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> jobs.remove(job), 0L);
                        jobs.add(job);

                        // the block's hardness is factored into lost durability
                        int maxDamage = tool.getType().getMaxDurability();
                        int damage = ((Damageable) meta).getDamage();
                        damage += Math.max(1, Math.ceil(target.getType().getHardness()));
                        double chance = 1.0 / (meta.getEnchantLevel(Enchantment.DURABILITY) + 1);
                        if (chance < 1.0) {
                            for (int i = 0; i < damage; i++) {
                                if (Utils.RNG.nextDouble() >= chance) {
                                    damage -= 1;
                                }
                            }
                        }
                        if (damage <= maxDamage) {
                            ((Damageable) meta).setDamage(damage);
                            tool.setItemMeta(meta);
                            dropper.getInventory().setItem(slot, tool);
                        }
                        else {
                            dropper.getInventory().setItem(slot, null);
                            dropper.getWorld().playSound(dropper.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.12F);
                        }
                    }
                }
            }
        }
    }

    public boolean isBreakable(Block block) {
        return !block.getType().isAir() && !block.isLiquid() && block.getType().getHardness() >= 0;
    }

    public static class MiningJob {

        private final Dropper machine;
        private final BoundingBox region;

        public MiningJob(Dropper machine, BoundingBox region) {
            this.machine = machine;
            this.region = region;
        }

        public Dropper getMachine() {
            return machine;
        }

        public BoundingBox getRegion() {
            return region;
        }
    }
}

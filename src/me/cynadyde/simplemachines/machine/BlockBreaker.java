package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class BlockBreaker implements Listener {

    private final SimpleMachinesPlugin plugin;
    private final Set<MiningJob> jobs;

    Class<?> nmsItemToolMaterial;
    Class<?> nmsItemShears;

    Method nmsItemGetDestroySpeed;
    Method nmsItemCanDestroySpecialBlock;

    Field obcItemHandle;

    public BlockBreaker(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
        this.jobs = new HashSet<>();

        try {
            Class<?> obcItem = Class.forName("org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack");
            Class<?> nmsItem = Class.forName("net.minecraft.server.v1_16_R1.Item");
            Class<?> nmsItemStack = Class.forName("net.minecraft.server.v1_16_R1.ItemStack");
            Class<?> nmsIBlockData = Class.forName("net.minecraft.server.v1_16_R1.IBlockData");

            nmsItemToolMaterial = Class.forName("net.minecraft.server.v1_16_R1.ItemToolMaterial");
            nmsItemShears = Class.forName("net.minecraft.server.v1_16_R1.ItemShears");

            nmsItemGetDestroySpeed = nmsItem.getDeclaredMethod("getDestroySpeed", nmsItemStack, nmsIBlockData);
            nmsItemCanDestroySpecialBlock = nmsItem.getDeclaredMethod("canDestroySpecialBlock", nmsIBlockData);

            obcItemHandle = obcItem.getDeclaredField("handle");
        }
        catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException ex) {
            plugin.getLogger().severe("could not perform reflection: " + ex.getMessage());
        }
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
                for (int i = 0; i < contents.length; i++) {
                    ItemStack item = contents[i];
                    if (item != null && Utils.TOOLS.contains(item.getType())) {
                        slot = i;
                        break;
                    }
                }
                // a tool is required to break blocks...
                if (slot != -1) {
                    ItemStack tool = contents[slot];
                    ItemMeta meta = tool.getItemMeta();
                    assert (meta instanceof Damageable);

                    if (target.breakNaturally(tool)) {

                        // the mining job will scoop up any dropped items and have them machine-dispensed instead
                        final MiningJob job = new MiningJob(dropper, machine.getBoundingBox().expand(0.25));
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> jobs.remove(job), 0L);
                        jobs.add(job);

                        // the mining duration in quarter seconds is taken from the tool's durability
                        int maxDamage = tool.getType().getMaxDurability();
                        int damage = ((Damageable) meta).getDamage() + 1 + (getTicksToMine(target, tool) / 5);
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
        return !block.isLiquid() && !block.getType().isAir() && !(block.getType().getHardness() < 0);
    }

    public int getTicksToMine(Block block, ItemStack tool) {

        boolean appropriateTool = canToolHarvest(block, tool);
        boolean preferredTool = isToolEfficient(block, tool);

        double seconds = block.getType().getHardness() * (appropriateTool ? 1.5 : 5.0);

        if (preferredTool) {
            double multiplier = getToolDigMultiplier(tool);
            ItemMeta meta = tool.getItemMeta();
            if (meta != null) {
                int efficiency = meta.getEnchantLevel(Enchantment.DIG_SPEED);
                if (efficiency > 0) {
                    multiplier += Math.pow(efficiency, 2) + 1;
                }
            }
            seconds /= multiplier;
        }
        return (int) Math.round(seconds * 20);
    }

    public boolean canToolHarvest(Block block, ItemStack tool) {
        // TODO
        return false;
    }

    public boolean isToolEfficient(Block block, ItemStack tool) {
        // TODO
        return false;
    }

    public double getToolDigMultiplier(ItemStack tool) {
        // TODO
        return 1.0D;
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

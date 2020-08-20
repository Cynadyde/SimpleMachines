package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import me.cynadyde.simplemachines.util.Utils;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dropper;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Objects;

public class BlockBreaker implements Listener {

    private final SimpleMachinesPlugin plugin;

    private Class<?> obcCraftItemStack;
    private Class<?> obcCraftBlock;

    private Field obcCraftItemStackHandle;
    private Method nmsItemStackGetItem;
    private Method nmsItemGetDestroySpeed;
    private Method obcCraftBlockGetNMS;

    public BlockBreaker(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;

        try {
            // get necessary classes depending on current MC version in use

            String version = plugin.getServer().getClass().getPackage().getName().split("\\.")[3];

            obcCraftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
            obcCraftBlock = Class.forName("org.bukkit.craftbukkit." + version + ".block.CraftBlock");
            Class<?> nmsItemStack = Class.forName("net.minecraft.server." + version + ".ItemStack");
            Class<?> nmsItem = Class.forName("net.minecraft.server." + version + ".Item");
            Class<?> nmsIBlockData = Class.forName("net.minecraft.server." + version + ".IBlockData");

            obcCraftItemStackHandle = obcCraftItemStack.getDeclaredField("handle");
            nmsItemStackGetItem = nmsItemStack.getDeclaredMethod("getItem");
            nmsItemGetDestroySpeed = nmsItem.getDeclaredMethod("getDestroySpeed", nmsItemStack, nmsIBlockData);
            obcCraftBlockGetNMS = obcCraftBlock.getDeclaredMethod("getNMS");

            obcCraftItemStackHandle.setAccessible(true);
        }
        catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException ex) {
            plugin.getLogger().severe("could not perform reflection due to " + ex.getClass().getName() + ": " + ex.getMessage());
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
        if (isBlockBreakerMachine(machine)) {

            Dropper dropper = (Dropper) machine.getState();
            Block target = machine.getRelative(BlockFace.UP, 2);
            if (isBreakable(target)) {

                // find the next tool stored in the dropper, if any...
                int slot = -1;
                ItemStack[] contents = dropper.getInventory().getContents();
                Iterator<Integer> iterator = new RandomPermuteIterator(0, contents.length);
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

                    /* the block's hardness is factored into lost durability,
                        and a penalty is added if the incorrect tool is used */
                    double factor = isPreferredTool(target, tool) ? 2.0 : 5.0;
                    float hardness = target.getType().getHardness();

                    int maxDamage = tool.getType().getMaxDurability();
                    int damage = ((Damageable) meta).getDamage();

                    damage += Math.max(1, Math.ceil(hardness * factor));
                    double chance = 1.0 / (meta.getEnchantLevel(Enchantment.DURABILITY) + 1);
                    if (chance < 1.0) {
                        for (int i = 0; i < damage; i++) {
                            if (Utils.RNG.nextDouble() >= chance) {
                                damage -= 1;
                            }
                        }
                    }
                    if (damage > maxDamage) {
                        dropper.getInventory().setItem(slot, null);
                        dropper.getWorld().playSound(dropper.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.12F);
                    }
                    else {
                        ((Damageable) meta).setDamage(damage);
                        tool.setItemMeta(meta);
                        dropper.getInventory().setItem(slot, tool);
                    }
                    for (ItemStack drop : target.getDrops(tool)) {
                        Utils.dropFromDropper(dropper, drop);
                    }
                    target.getWorld().playEffect(target.getLocation().add(0.5, 0.5, 0.5), Effect.STEP_SOUND, target.getType());

                    target.setType(Material.AIR);
                }
            }
        }
    }

    public boolean isBreakable(Block block) {
        /* TODO it would be a good idea to allow this machine to be hooked into anti grief, somehow */

        return !block.getType().isAir() && !block.isLiquid() && block.getType().getHardness() >= 0;
    }

    public boolean isPreferredTool(Block block, ItemStack tool) {
        if (obcCraftItemStack.isInstance(tool) && obcCraftBlock.isInstance(block)) {
            try {
                Object itemStack = obcCraftItemStackHandle.get(tool);
                Object item = nmsItemStackGetItem.invoke(itemStack);
                Object blockData = obcCraftBlockGetNMS.invoke(block);
                return (float) nmsItemGetDestroySpeed.invoke(item, itemStack, blockData) > 1.0F;
            }
            catch (ClassCastException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
                plugin.getLogger().severe("could not perform Item getDestroySpeed reflection due to " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
        return false;
    }
}

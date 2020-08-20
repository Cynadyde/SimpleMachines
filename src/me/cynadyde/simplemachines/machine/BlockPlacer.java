package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import me.cynadyde.simplemachines.util.Utils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.material.MaterialData;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BlockPlacer implements Listener {

    private final SimpleMachinesPlugin plugin;

    private Method obcCraftBlockGetNMS;
    private Method nmsIBlockDataGetStepSound;
    private Method nmsSoundEffectTypeGetPlaceSound;
    private Field nmsSoundEffectKey;

    public BlockPlacer(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;

        try {
            // get necessary classes depending on current MC version in use

            String version = plugin.getServer().getClass().getPackage().getName().split("\\.")[3];

            Class<?> obcCraftBlock = Class.forName("org.bukkit.craftbukkit." + version + ".block.CraftBlock");
            Class<?> nmsIBlockData = Class.forName("net.minecraft.server." + version + ".BlockBase.BlockData");
            Class<?> nmsSoundEffectType = Class.forName("net.minecraft.server." + version + ".SoundEffectType");
            Class<?> nmsSoundEffect = Class.forName("net.minecraft.server." + version + ".SoundEffect");

            obcCraftBlockGetNMS = obcCraftBlock.getDeclaredMethod("getNMS");
            nmsIBlockDataGetStepSound = nmsIBlockData.getDeclaredMethod("getStepSound");
            nmsSoundEffectTypeGetPlaceSound = nmsSoundEffectType.getDeclaredMethod("e");
            nmsSoundEffectKey = nmsSoundEffect.getDeclaredField("b");

            nmsSoundEffectKey.setAccessible(true);
        }
        catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException ex) {
            plugin.getLogger().severe("could not perform reflection: " + ex.getMessage());
        }
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        if (isBlockPlacingMachine(event.getBlock())) {
            event.setCancelled(true);

            final Block machine = event.getBlock();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> doBlockPlace(machine), 0L);
        }
    }

    public boolean isBlockPlacingMachine(Block block) {
        if (block.getType() == Material.DROPPER) {
            Block part = block.getRelative(BlockFace.UP);
            if (part.getType() == Material.PISTON
                    && ((Directional) part.getBlockData()).getFacing() == BlockFace.DOWN) {
                return true;
            }
        }
        return false;
    }

    public void doBlockPlace(Block machine) {
        if (isBlockPlacingMachine(machine)) {

            Dropper dropper = (Dropper) machine.getState();
            Block dest = machine.getRelative(BlockFace.UP, 2);
            if (dest.getType().isAir() || dest.isLiquid()) {

                boolean waterlogged = dest.getType() == Material.WATER;

                // find the next block to place, if any...
                int slot = -1;
                ItemStack[] contents = dropper.getInventory().getContents();
                RandomPermuteIterator iterator = new RandomPermuteIterator(0, contents.length);
                while (iterator.hasNext()) {
                    int i = iterator.next();
                    ItemStack item = contents[i];
                    if (item != null && item.getType().isBlock()) {
                        slot = i;
                        break;
                    }
                }
                if (slot != -1) {
                    ItemStack item = contents[slot];
                    if (item.getItemMeta() instanceof BlockStateMeta) {
                        copyStateToBlock(dest, ((BlockStateMeta) item.getItemMeta()).getBlockState());
                    }
                    else {
                        dest.setType(item.getType());
                    }

                    BlockFace placeDir = ((Directional) dropper.getBlockData()).getFacing();
                    BlockData destData = dest.getBlockData();

                    if (destData instanceof Directional) {
                        ((Directional) destData).setFacing(placeDir.getOppositeFace());
                    }
                    if (destData instanceof Rotatable) {
                        ((Rotatable) destData).setRotation(placeDir);
                    }
                    if (destData instanceof Waterlogged) {
                        ((Waterlogged) destData).setWaterlogged(waterlogged);
                    }
                    dest.setBlockData(destData);
                    playBlockPlaceSound(dest);
                }
            }
        }
    }

    public void copyStateToBlock(Block dest, BlockState data) {
//        dest.setType(item.getType());
//
//        if (item.getItemMeta() instanceof TileState) {
//            CraftBlockEntityState<?> state = (CraftBlockEntityState<?>) dest.getState();
//            BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
//            BlockState tile = meta.getBlockState();
//
//
//
//        }
    }

    public void playBlockPlaceSound(Block block) {
        try {
            Object o = block;
            o = obcCraftBlockGetNMS.invoke(o);
            o = nmsIBlockDataGetStepSound.invoke(o);
            o = nmsSoundEffectTypeGetPlaceSound.invoke(o);
            o = nmsSoundEffectKey.get(o);

            String key = o.toString(); /*((CraftBlock) block).getNMS().getStepSound().e().b.toString();*/
            block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), key, 1.0F, 1.0F);
        }
        catch (IllegalAccessException | InvocationTargetException ex) {
            plugin.getLogger().severe("could not perform block SoundEffectType reflection: " + ex.getMessage());
        }
    }
}

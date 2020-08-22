package me.cynadyde.simplemachines.util;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.SpigotWorldConfig;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class ReflectiveUtils {

    private static Logger logger = Logger.getLogger("minecraft");

    private static Class<?> obcCraftBlock;
    private static Class<?> obcCraftBlockEntityState;
    private static Class<?> obcCraftItemStack;

    private static Method obcCraftBlockGetNMS;
    private static Method obcCraftBlockEntityStateApplyTo;
    private static Method obcCraftItemStackAsNewCraftStack;
    private static Method nmsItemGetDestroySpeed;
    private static Method nmsItemGetCraftingRemainingItem;
    private static Method nmsItemStackGetItem;
    private static Method nmsTileEntityHopperSetCooldown;

    private static Field obcCraftBlockEntityStateTileEntity;
    private static Field obcCraftEntityEntity;
    private static Field obcCraftItemStackHandle;
    private static Field nmsEntityWorld;
    private static Field nmsTileEntityWorld;
    private static Field nmsWorldSpigotConfig;

    public static void setLogger(Logger pluginLogger) {
        logger = pluginLogger;
    }

    private static void logReflectionError(String name, Exception ex) {
        logger.severe("could not perform " + name + " reflection due to "
                + ex.getClass().getName() + ": " + ex.getMessage());
    }

    static {
        try {
            // get necessary classes depending on current MC version in use
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

            // get the needed obc/nms classes
            obcCraftBlock = Class.forName("org.bukkit.craftbukkit." + version + ".block.CraftBlock");
            obcCraftBlockEntityState = Class.forName("org.bukkit.craftbukkit." + version + ".block.CraftBlockEntityState");
            Class<?> obcCraftEntity = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftEntity");
            obcCraftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
            Class<?> nmsEntity = Class.forName("net.minecraft.server." + version + ".Entity");
            Class<?> nmsIBlockData = Class.forName("net.minecraft.server." + version + ".IBlockData");
            Class<?> nmsItem = Class.forName("net.minecraft.server." + version + ".Item");
            Class<?> nmsItemStack = Class.forName("net.minecraft.server." + version + ".ItemStack");
            Class<?> nmsTileEntity = Class.forName("net.minecraft.server." + version + ".TileEntity");
            Class<?> nmsTileEntityHopper = Class.forName("net.minecraft.server." + version + ".TileEntityHopper");
            Class<?> nmsWorld = Class.forName("net.minecraft.server." + version + ".World");

            // get the needed methods
            obcCraftBlockGetNMS = obcCraftBlock.getDeclaredMethod("getNMS");
            obcCraftBlockEntityStateApplyTo = obcCraftBlockEntityState.getDeclaredMethod("applyTo", nmsTileEntity);
            obcCraftItemStackAsNewCraftStack = obcCraftItemStack.getDeclaredMethod("asNewCraftStack", nmsItem);
            nmsItemGetDestroySpeed = nmsItem.getDeclaredMethod("getDestroySpeed", nmsItemStack, nmsIBlockData);
            nmsItemStackGetItem = nmsItemStack.getDeclaredMethod("getItem");
            nmsTileEntityHopperSetCooldown = nmsTileEntityHopper.getDeclaredMethod("setCooldown", int.class);
            nmsItemGetCraftingRemainingItem = nmsItem.getDeclaredMethod("getCraftingRemainingItem");

            // get the needed fields
            obcCraftBlockEntityStateTileEntity = obcCraftBlockEntityState.getDeclaredField("tileEntity");
            obcCraftEntityEntity = obcCraftEntity.getDeclaredField("entity");
            obcCraftItemStackHandle = obcCraftItemStack.getDeclaredField("handle");
            nmsEntityWorld = nmsEntity.getDeclaredField("world");
            nmsTileEntityWorld = nmsTileEntity.getDeclaredField("world");
            nmsWorldSpigotConfig = nmsWorld.getField("spigotConfig");

            // make sure the fields and methods are accessible
            obcCraftBlockEntityStateApplyTo.setAccessible(true);
            obcCraftBlockEntityStateTileEntity.setAccessible(true);
            obcCraftEntityEntity.setAccessible(true);
            obcCraftItemStackHandle.setAccessible(true);
            nmsEntityWorld.setAccessible(true);
            nmsTileEntityWorld.setAccessible(true);
            nmsTileEntityHopperSetCooldown.setAccessible(true);
            nmsWorldSpigotConfig.setAccessible(true);
        }
        catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException ex) {
            logReflectionError("initial", ex);
        }
    }

    public static ItemStack getCraftingRemainder(ItemStack item) {
        try {
            if (obcCraftItemStack.isInstance(item)) {
                // net.minecraft.server.v1_16_R1.Item nmsItem = ((CraftItemStack) c).handle.getItem().getCraftingRemainingItem();
                // ItemStack item = CraftItemStack.asNewCraftStack(nmsItem);
                Object nmsItemObj = nmsItemStackGetItem.invoke(obcCraftItemStackHandle.get(item));
                Object nmsItemObjResult = nmsItemGetCraftingRemainingItem.invoke(nmsItemObj);
                ItemStack result = (ItemStack) obcCraftItemStackAsNewCraftStack.invoke(nmsItemObjResult);
                return Utils.isEmpty(result) ? null : result;
            }
        }
        catch (ClassCastException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
            logReflectionError("Item#getCraftingRemainingItem()", ex);
        }
        return null;
    }

    public static boolean isPreferredTool(Block block, ItemStack tool) {
        try {
            if (obcCraftItemStack.isInstance(tool) && obcCraftBlock.isInstance(block)) {
                Object itemStack = obcCraftItemStackHandle.get(tool);
                Object item = nmsItemStackGetItem.invoke(itemStack);
                Object blockData = obcCraftBlockGetNMS.invoke(block);
                return (float) nmsItemGetDestroySpeed.invoke(item, itemStack, blockData) > 1.0F;
            }
        }
        catch (ClassCastException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
            logReflectionError("Item#getDestroySpeed()", ex);
        }
        return false;
    }

    public static void copyBlockState(BlockState dest, BlockState source) {

        // FIXME placed chest did not retain its contents
        try {
            if (obcCraftBlockEntityState.isInstance(dest) && obcCraftBlockEntityState.isInstance(source)) {
                obcCraftBlockEntityStateApplyTo.invoke(source, obcCraftBlockEntityStateTileEntity.get(dest));
                dest.update(true, true);
            }
        }
        catch (ClassCastException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
            logReflectionError("TileEntity#applyTo()", ex);
        }
    }

    public static SpigotWorldConfig getWorldConfigFor(InventoryHolder holder) {
        try {
            if (holder instanceof BlockState) {
                return (SpigotWorldConfig) nmsWorldSpigotConfig.get(nmsTileEntityWorld.get(obcCraftBlockEntityStateTileEntity.get(holder)));
            }
            else if (holder instanceof Entity) {
                return (SpigotWorldConfig) nmsWorldSpigotConfig.get(nmsEntityWorld.get(obcCraftEntityEntity.get(holder)));
            }
        }
        catch (ClassCastException | IllegalAccessException | NullPointerException ex) {
            logReflectionError("InventoryHolder...World#spigotConfig", ex);
        }
        return null;
    }

    public static void setHopperCooldown(Hopper hopper, int cooldown) {
        try {
            nmsTileEntityHopperSetCooldown.invoke(obcCraftBlockEntityStateTileEntity.get(hopper), cooldown);
        }
        catch (IllegalAccessException | InvocationTargetException | NullPointerException ex) {
            logReflectionError("TileEntityHopper#cooldown", ex);
        }
    }
}

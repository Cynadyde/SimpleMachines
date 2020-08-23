package me.cynadyde.simplemachines.util;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.SpigotWorldConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class ReflectiveUtils {

    private static Logger logger = Logger.getLogger("minecraft");

    private static Class<?> obcCraftBlock;
    private static Class<?> obcCraftBlockEntityState;
    private static Class<?> obcCraftItemStack;

    private static Constructor<?> nmsNbtCompoundConstructor;

    private static Method obcCraftBlockGetNMS;
    private static Method obcCraftBlockEntityStateGetTileEntity;
    private static Method obcCraftBlockEntityStateGetSnapshot;
    private static Method obcCraftItemStackAsNewCraftStack;
    private static Method nmsItemGetDestroySpeed;
    private static Method nmsItemGetCraftingRemainingItem;
    private static Method nmsItemStackGetItem;
    private static Method nmsTileEntityGetWorld;
    private static Method nmsTileEntityGetPosition;
    private static Method nmsTileEntityHopperSetCooldown;
    private static Method nmsTileEntityLoad;
    private static Method nmsTileEntitySetLocation;
    private static Method nmsTileEntitySave;

    private static Field obcCraftEntityEntity;
    private static Field obcCraftItemStackHandle;
    private static Field nmsEntityWorld;
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
            Class<?> nmsNbtCompound = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
            Class<?> nmsTileEntity = Class.forName("net.minecraft.server." + version + ".TileEntity");
            Class<?> nmsTileEntityHopper = Class.forName("net.minecraft.server." + version + ".TileEntityHopper");
            Class<?> nmsWorld = Class.forName("net.minecraft.server." + version + ".World");
            Class<?> nmsBlockPosition = Class.forName("net.minecraft.server.v1_16_R2.BlockPosition");

            // get the needed constructors
            nmsNbtCompoundConstructor = nmsNbtCompound.getConstructor();

            // get the needed methods
            obcCraftBlockGetNMS = obcCraftBlock.getDeclaredMethod("getNMS");
            obcCraftBlockEntityStateGetTileEntity = obcCraftBlockEntityState.getDeclaredMethod("getTileEntity");
            obcCraftBlockEntityStateGetSnapshot = obcCraftBlockEntityState.getDeclaredMethod("getSnapshot");
            obcCraftItemStackAsNewCraftStack = obcCraftItemStack.getDeclaredMethod("asNewCraftStack", nmsItem);
            nmsItemGetDestroySpeed = nmsItem.getDeclaredMethod("getDestroySpeed", nmsItemStack, nmsIBlockData);
            nmsItemGetCraftingRemainingItem = nmsItem.getDeclaredMethod("getCraftingRemainingItem");
            nmsItemStackGetItem = nmsItemStack.getDeclaredMethod("getItem");
            nmsTileEntityGetWorld = nmsTileEntity.getDeclaredMethod("getWorld");
            nmsTileEntityGetPosition = nmsTileEntity.getDeclaredMethod("getPosition");
            nmsTileEntityHopperSetCooldown = nmsTileEntityHopper.getDeclaredMethod("setCooldown", int.class);
            nmsTileEntityLoad = nmsTileEntity.getDeclaredMethod("load", nmsIBlockData, nmsNbtCompound);
            nmsTileEntitySetLocation = nmsTileEntity.getDeclaredMethod("setLocation", nmsWorld, nmsBlockPosition);
            nmsTileEntitySave = nmsTileEntity.getDeclaredMethod("save", nmsNbtCompound);

            // get the needed fields
            obcCraftEntityEntity = obcCraftEntity.getDeclaredField("entity");
            obcCraftItemStackHandle = obcCraftItemStack.getDeclaredField("handle");
            nmsEntityWorld = nmsEntity.getDeclaredField("world");
            nmsWorldSpigotConfig = nmsWorld.getField("spigotConfig");

            // make sure everything is accessible
            obcCraftBlockGetNMS.setAccessible(true);
            obcCraftBlockEntityStateGetTileEntity.setAccessible(true);
            obcCraftBlockEntityStateGetSnapshot.setAccessible(true);
            obcCraftEntityEntity.setAccessible(true);
            obcCraftItemStackAsNewCraftStack.setAccessible(true);
            obcCraftItemStackHandle.setAccessible(true);
            nmsItemGetDestroySpeed.setAccessible(true);
            nmsItemGetCraftingRemainingItem.setAccessible(true);
            nmsItemStackGetItem.setAccessible(true);
            nmsEntityWorld.setAccessible(true);
            nmsTileEntityGetWorld.setAccessible(true);
            nmsTileEntityGetPosition.setAccessible(true);
            nmsTileEntityLoad.setAccessible(true);
            nmsTileEntitySetLocation.setAccessible(true);
            nmsTileEntitySave.setAccessible(true);
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

    public static void copyBlockState(BlockState source, BlockState dest) {
        if (source == null || dest == null || !source.getClass().equals(dest.getClass())) {
            throw new IllegalArgumentException("source and dest must be non-null and have the same class.");
        }
        try {
            if (obcCraftBlockEntityState.isInstance(dest) && obcCraftBlockEntityState.isInstance(source)) {
                // modifies dest's tile entity snapshot so that its next update applies changes to the world

                Object sourceTileEnt = obcCraftBlockEntityStateGetSnapshot.invoke(source);
                Object destTileEnt = obcCraftBlockEntityStateGetSnapshot.invoke(dest);

                Object sourceNbt = nmsTileEntitySave.invoke(sourceTileEnt, nmsNbtCompoundConstructor.newInstance());

                Object destWorld = nmsTileEntityGetWorld.invoke(destTileEnt);
                Object destPos = nmsTileEntityGetPosition.invoke(destTileEnt);
                Object destBlockData = obcCraftBlockGetNMS.invoke(dest.getBlock());

                nmsTileEntitySetLocation.invoke(sourceTileEnt, destWorld, destPos);
                nmsTileEntityLoad.invoke(destTileEnt, destBlockData, sourceNbt);

                dest.update(true, true);
            }
        }
        catch (ClassCastException | IllegalAccessException | InvocationTargetException | InstantiationException | NullPointerException ex) {
            logReflectionError("TileEntity#applyTo()", ex);
        }
    }

    public static SpigotWorldConfig getWorldConfigFor(InventoryHolder holder) {
        try {
            if (holder instanceof BlockState) {
                return (SpigotWorldConfig) nmsWorldSpigotConfig.get(nmsTileEntityGetWorld.invoke(obcCraftBlockEntityStateGetTileEntity.invoke(holder)));
            }
            else if (holder instanceof Entity) {
                return (SpigotWorldConfig) nmsWorldSpigotConfig.get(nmsEntityWorld.get(obcCraftEntityEntity.get(holder)));
            }
        }
        catch (ClassCastException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
            logReflectionError("InventoryHolder...World#spigotConfig", ex);
        }
        return null;
    }

    public static void setHopperCooldown(Hopper hopper, int cooldown) {
        try {
            nmsTileEntityHopperSetCooldown.invoke(obcCraftBlockEntityStateGetTileEntity.invoke(hopper), cooldown);
        }
        catch (ClassCastException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
            logReflectionError("TileEntityHopper#cooldown", ex);
        }
    }
}

package me.cynadyde.simplemachines.util;

import net.minecraft.server.v1_16_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.craftbukkit.v1_16_R2.block.CraftBlockEntityState;
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
    private static Method obcCraftBlockEntityStateCopyData;
    private static Method obcCraftBlockEntityStateGetTileEntity;
    private static Method obcCraftBlockEntityStateGetSnapshot;
    private static Method obcCraftItemStackAsNewCraftStack;
    private static Method nmsItemGetDestroySpeed;
    private static Method nmsItemGetCraftingRemainingItem;
    private static Method nmsItemStackGetItem;
    private static Method nmsTileEntityHopperSetCooldown;

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
            obcCraftBlockEntityStateCopyData = obcCraftBlockEntityState.getDeclaredMethod("copyData", nmsTileEntity, nmsTileEntity);
            obcCraftBlockEntityStateGetTileEntity = obcCraftBlockEntityState.getDeclaredMethod("getTileEntity");
            obcCraftBlockEntityStateGetSnapshot = obcCraftBlockEntityState.getDeclaredMethod("getSnapshot");
            obcCraftItemStackAsNewCraftStack = obcCraftItemStack.getDeclaredMethod("asNewCraftStack", nmsItem);
            nmsItemGetDestroySpeed = nmsItem.getDeclaredMethod("getDestroySpeed", nmsItemStack, nmsIBlockData);
            nmsItemGetCraftingRemainingItem = nmsItem.getDeclaredMethod("getCraftingRemainingItem");
            nmsItemStackGetItem = nmsItemStack.getDeclaredMethod("getItem");
            nmsTileEntityHopperSetCooldown = nmsTileEntityHopper.getDeclaredMethod("setCooldown", int.class);

            // get the needed fields
            obcCraftEntityEntity = obcCraftEntity.getDeclaredField("entity");
            obcCraftItemStackHandle = obcCraftItemStack.getDeclaredField("handle");
            nmsEntityWorld = nmsEntity.getDeclaredField("world");
            nmsTileEntityWorld = nmsTileEntity.getDeclaredField("world");
            nmsWorldSpigotConfig = nmsWorld.getField("spigotConfig");

            // make sure the fields and methods are accessible
            obcCraftBlockGetNMS.setAccessible(true);
            obcCraftBlockEntityStateCopyData.setAccessible(true);
            obcCraftBlockEntityStateGetTileEntity.setAccessible(true);
            obcCraftBlockEntityStateGetSnapshot.setAccessible(true);
            obcCraftEntityEntity.setAccessible(true);
            obcCraftItemStackAsNewCraftStack.setAccessible(true);
            obcCraftItemStackHandle.setAccessible(true);
            nmsItemGetDestroySpeed.setAccessible(true);
            nmsItemGetCraftingRemainingItem.setAccessible(true);
            nmsItemStackGetItem.setAccessible(true);
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

    public static void copyBlockState(BlockState source, BlockState dest) {

        // TODO tidy up

        try {
            System.out.println("Attempting to copy block state from " + source + " to " + dest);
            if (obcCraftBlockEntityState.isInstance(dest) && obcCraftBlockEntityState.isInstance(source)) {

//                TileEntity sourceTileEnt = (TileEntity) obcCraftBlockEntityStateGetTileEntity.invoke(source);
                TileEntity sourceTileEnt = (TileEntity) obcCraftBlockEntityStateGetSnapshot.invoke(source);
                TileEntity destTileEnt = (TileEntity) obcCraftBlockEntityStateGetSnapshot.invoke(dest);

                NBTTagCompound sourceNbt = sourceTileEnt.save(new NBTTagCompound());
                System.out.println("the nbt being copied is: " + sourceNbt);

                World world = destTileEnt.getWorld();
                BlockPosition pos = destTileEnt.getPosition();
                sourceTileEnt.setLocation(world, pos);

                sourceNbt = sourceTileEnt.save(new NBTTagCompound());
                System.out.println("the nbt being copied is now: " + sourceNbt);

                destTileEnt.load((IBlockData) obcCraftBlockGetNMS.invoke(dest.getBlock()), sourceNbt);
                NBTTagCompound destNbt = destTileEnt.save(new NBTTagCompound());
                System.out.println("the dest tile entity snapshot is: " + destNbt);

//                System.out.println("source tile entity: " + sourceTileEnt);
//                System.out.println("dest tile entity: " + destTileEnt);

                /*sourceTileEnt.setLocation(destTileEnt.getWorld(), destTileEnt.getPosition());
                destTileEnt.getWorld().setTileEntity(destTileEnt.getPosition(), sourceTileEnt);*/

//                CraftBlockEntityState<?> state = new CraftBlockEntityState<>(source.getType(), sourceTileEnt);

//                CraftBlockEntityState<?> state = (CraftBlockEntityState<?>) dest.getBlock().getState();

                boolean result = dest.update(true, true);
                if (!result) {
                    System.out.println("could NOT update blocks state!");
                }
                else {
                    System.out.println("update successful!");
                }

                System.out.println("nbt: " + ((CraftBlockEntityState<?>) dest).getSnapshotNBT());

//                NBTTagCompound sourceNbt = ((TileEntity) sourceTileEnt).save(new NBTTagCompound());
//                NBTTagCompound destNbt = ((TileEntity) destTileEnt).save(new NBTTagCompound());

//                Class<?> nmsTileEntity = Class.forName("net.minecraft.server.v1_16_R2.TileEntity");
//                Class<?> nmsNBTTagCompound = Class.forName("net.minecraft.server.v1_16_R2.NBTTagCompound");
//                Method nmsTileEntitySave = nmsTileEntity.getDeclaredMethod("save", nmsNBTTagCompound);
//                Constructor<?> nmsNBTTagCompoundNew = nmsNBTTagCompound.getConstructor();
//
//                Object sourceNbt = nmsTileEntitySave.invoke(sourceTileEnt, nmsNBTTagCompoundNew.newInstance());
//                Object destNbt = nmsTileEntitySave.invoke(destTileEnt, nmsNBTTagCompoundNew.newInstance());

//                System.out.println("sourceTileEnt: " + sourceNbt);
//                System.out.println("destTileEnt: " + destNbt);

//                obcCraftBlockEntityStateCopyData.invoke(dest, sourceTileEnt, destTileEnt);
//                obcCraftBlockEntityStateApplyTo.invoke(source, obcCraftBlockEntityStateTileEntity.get(dest));
//                System.out.println("forcing an update of the dest");
//                boolean result = dest.update(true, true);
//                if (!result) System.out.println("could NOT update dest");

//                Object resultTileEnt = obcCraftBlockEntityStateGetTileEntity.invoke(dest);
//                NBTTagCompound resultNbt = ((TileEntity) resultTileEnt).save(new NBTTagCompound());
//
//                System.out.println("NEW destTileEnt: " + resultNbt);
            }
        }
        catch (ClassCastException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
            logReflectionError("TileEntity#applyTo()", ex);
        }
    }

    public static SpigotWorldConfig getWorldConfigFor(InventoryHolder holder) {
        try {
            if (holder instanceof BlockState) {
                return (SpigotWorldConfig) nmsWorldSpigotConfig.get(nmsTileEntityWorld.get(obcCraftBlockEntityStateGetTileEntity.invoke(holder)));
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

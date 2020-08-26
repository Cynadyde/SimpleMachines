package me.cynadyde.simplemachines.util;

import net.minecraft.server.v1_16_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.spigotmc.SpigotWorldConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.logging.Logger;

public class ReflectiveUtils {

    private static Logger logger = Logger.getLogger("minecraft");

    private static Class<?> obcCraftBlock;
    private static Class<?> obcCraftBlockEntityState;
    private static Class<?> obcCraftContainer;
    private static Class<?> obcCraftEntity;
    private static Class<?> obcCraftItemStack;
    private static Class<?> obcCraftMetaSkull;
    private static Class<?> obcCraftSkull;
    private static Class<?> obcCraftWorld;
    private static Class<?> nmsBlock;
    private static Class<?> nmsBlockData;
    private static Class<?> nmsBlockPosition;
    private static Class<?> nmsEntity;
    private static Class<?> nmsIBlockData;
    private static Class<?> nmsIWorldReader;
    private static Class<?> nmsItem;
    private static Class<?> nmsItemBlock;
    private static Class<?> nmsItemStack;
    private static Class<?> nmsNbtCompound;
    private static Class<?> nmsSoundEffect;
    private static Class<?> nmsSoundEffectType;
    private static Class<?> nmsTileEntity;
    private static Class<?> nmsTileEntityHopper;
    private static Class<?> nmsWorld;

    private static Constructor<?> nmsNbtCompoundConstructor;

    private static Method obcCraftBlockGetNMS;
    private static Method obcCraftBlockGetPosition;
    private static Method obcCraftBlockEntityStateGetTileEntity;
    private static Method obcCraftBlockEntityStateGetSnapshot;
    private static Method obcCraftBlockEntityStateLoad;
    private static Method obcCraftContainerSetCustomName;
    private static Method obcCraftItemStackAsNewCraftStack;
    private static Method obcCraftWorldGetHandle;
    private static Method nmsBlockGetBlockData;
    private static Method nmsBlockDataCanPlace;
    private static Method nmsBlockDataGetStepSound;
    private static Method nmsBlockDataIsRequiresSpecialTool;
    private static Method nmsItemCanDestroySpecialBlock;
    private static Method nmsItemGetDestroySpeed;
    private static Method nmsItemGetCraftingRemainingItem;
    private static Method nmsItemBlockGetBlock;
    private static Method nmsItemStackGetItem;
    private static Method nmsTileEntityGetWorld;
    private static Method nmsTileEntityGetPosition;
    private static Method nmsTileEntityHopperSetCooldown;
    private static Method nmsTileEntityLoad;
    private static Method nmsTileEntitySetLocation;
    private static Method nmsTileEntitySetPosition;
    private static Method nmsTileEntitySave;
    private static Method nmsSoundEffectTypeGetStepSound;

    private static Field obcCraftEntityEntity;
    private static Field obcCraftItemStackHandle;
    private static Field obcCraftMetaSkullProfile;
    private static Field obcCraftSkullProfile;
    private static Field nmsEntityWorld;
    private static Field nmsWorldSpigotConfig;

    private ReflectiveUtils() {}

    public static void setLogger(Logger pluginLogger) {
        logger = pluginLogger;
    }

    private static void logReflectionError(String name, Exception ex) {
        logger.severe("could not perform " + name + " reflection due to "
                + ex.getClass().getName() + ": " + ex.getMessage());

        if (!(ex instanceof ReflectiveOperationException)) {
            ex.printStackTrace();
        }
    }

    public static void reflect() {
        try {
            // get necessary classes depending on current MC version in use
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

            // get the needed obc/nms classes
            obcCraftBlock = Class.forName("org.bukkit.craftbukkit." + version + ".block.CraftBlock");
            obcCraftBlockEntityState = Class.forName("org.bukkit.craftbukkit." + version + ".block.CraftBlockEntityState");
            obcCraftContainer = Class.forName("org.bukkit.craftbukkit.v1_16_R2.block.CraftContainer");
            obcCraftEntity = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftEntity");
            obcCraftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
            obcCraftMetaSkull = Class.forName("org.bukkit.craftbukkit.v1_16_R2.inventory.CraftMetaSkull");
            obcCraftSkull = Class.forName("org.bukkit.craftbukkit.v1_16_R2.block.CraftSkull");
            obcCraftWorld = Class.forName("org.bukkit.craftbukkit." + version + ".CraftWorld");
            nmsBlock = Class.forName("net.minecraft.server." + version + ".Block");
            nmsBlockData = Class.forName("net.minecraft.server." + version + ".BlockBase$BlockData");
            nmsEntity = Class.forName("net.minecraft.server." + version + ".Entity");
            nmsIBlockData = Class.forName("net.minecraft.server." + version + ".IBlockData");
            nmsIWorldReader = Class.forName("net.minecraft.server.v1_16_R2.IWorldReader");
            nmsItem = Class.forName("net.minecraft.server." + version + ".Item");
            nmsItemBlock = Class.forName("net.minecraft.server.v1_16_R2.ItemBlock");
            nmsItemStack = Class.forName("net.minecraft.server." + version + ".ItemStack");
            nmsNbtCompound = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
            nmsSoundEffect = Class.forName("net.minecraft.server.v1_16_R2.SoundEffect");
            nmsSoundEffectType = Class.forName("net.minecraft.server.v1_16_R2.SoundEffectType");
            nmsTileEntity = Class.forName("net.minecraft.server." + version + ".TileEntity");
            nmsTileEntityHopper = Class.forName("net.minecraft.server." + version + ".TileEntityHopper");
            nmsWorld = Class.forName("net.minecraft.server." + version + ".World");
            nmsBlockPosition = Class.forName("net.minecraft.server." + version + ".BlockPosition");

            // get the needed constructors
            nmsNbtCompoundConstructor = nmsNbtCompound.getConstructor();

            // get the needed methods
            obcCraftBlockGetNMS = obcCraftBlock.getDeclaredMethod("getNMS");
            obcCraftBlockGetPosition = obcCraftBlock.getDeclaredMethod("getPosition");
            obcCraftBlockEntityStateGetTileEntity = obcCraftBlockEntityState.getDeclaredMethod("getTileEntity");
            obcCraftBlockEntityStateGetSnapshot = obcCraftBlockEntityState.getDeclaredMethod("getSnapshot");
            obcCraftBlockEntityStateLoad = obcCraftBlockEntityState.getDeclaredMethod("load", nmsTileEntity);
            obcCraftContainerSetCustomName = obcCraftContainer.getDeclaredMethod("setCustomName", String.class);
            obcCraftItemStackAsNewCraftStack = obcCraftItemStack.getDeclaredMethod("asNewCraftStack", nmsItem);
            obcCraftWorldGetHandle = obcCraftWorld.getDeclaredMethod("getHandle");
            nmsBlockGetBlockData = nmsBlock.getDeclaredMethod("getBlockData");
            nmsBlockDataCanPlace = nmsBlockData.getDeclaredMethod("canPlace", nmsIWorldReader, nmsBlockPosition);
            nmsBlockDataGetStepSound = nmsBlockData.getDeclaredMethod("getStepSound");
            nmsBlockDataIsRequiresSpecialTool = nmsBlockData.getDeclaredMethod("isRequiresSpecialTool");
            nmsItemCanDestroySpecialBlock = nmsItem.getDeclaredMethod("canDestroySpecialBlock", nmsIBlockData);
            nmsItemGetDestroySpeed = nmsItem.getDeclaredMethod("getDestroySpeed", nmsItemStack, nmsIBlockData);
            nmsItemGetCraftingRemainingItem = nmsItem.getDeclaredMethod("getCraftingRemainingItem");
            nmsItemBlockGetBlock = nmsItemBlock.getDeclaredMethod("getBlock");
            nmsItemStackGetItem = nmsItemStack.getDeclaredMethod("getItem");
            nmsTileEntityGetWorld = nmsTileEntity.getDeclaredMethod("getWorld");
            nmsTileEntityGetPosition = nmsTileEntity.getDeclaredMethod("getPosition");
            nmsTileEntityHopperSetCooldown = nmsTileEntityHopper.getDeclaredMethod("setCooldown", int.class);
            nmsTileEntityLoad = nmsTileEntity.getDeclaredMethod("load", nmsIBlockData, nmsNbtCompound);
            nmsTileEntitySetLocation = nmsTileEntity.getDeclaredMethod("setLocation", nmsWorld, nmsBlockPosition);
            nmsTileEntitySetPosition = nmsTileEntity.getDeclaredMethod("setPosition", nmsBlockPosition);
            nmsTileEntitySave = nmsTileEntity.getDeclaredMethod("save", nmsNbtCompound);
            nmsSoundEffectTypeGetStepSound = nmsSoundEffectType.getDeclaredMethod("d");

            // get the needed fields
            obcCraftEntityEntity = obcCraftEntity.getDeclaredField("entity");
            obcCraftItemStackHandle = obcCraftItemStack.getDeclaredField("handle");
            obcCraftMetaSkullProfile = obcCraftMetaSkull.getDeclaredField("profile");
            obcCraftSkullProfile = obcCraftSkull.getDeclaredField("profile");
            nmsEntityWorld = nmsEntity.getDeclaredField("world");
            nmsWorldSpigotConfig = nmsWorld.getField("spigotConfig");

            // make sure everything is accessible
            obcCraftBlockGetNMS.setAccessible(true);
            obcCraftBlockGetPosition.setAccessible(true);
            obcCraftBlockEntityStateGetTileEntity.setAccessible(true);
            obcCraftBlockEntityStateGetSnapshot.setAccessible(true);
            obcCraftBlockEntityStateLoad.setAccessible(true);
            obcCraftContainerSetCustomName.setAccessible(true);
            obcCraftEntityEntity.setAccessible(true);
            obcCraftItemStackAsNewCraftStack.setAccessible(true);
            obcCraftItemStackHandle.setAccessible(true);
            obcCraftMetaSkullProfile.setAccessible(true);
            obcCraftSkullProfile.setAccessible(true);
            obcCraftWorldGetHandle.setAccessible(true);
            nmsBlockGetBlockData.setAccessible(true);
            nmsBlockDataCanPlace.setAccessible(true);
            nmsBlockDataGetStepSound.setAccessible(true);
            nmsBlockDataIsRequiresSpecialTool.setAccessible(true);
            nmsItemCanDestroySpecialBlock.setAccessible(true);
            nmsItemGetDestroySpeed.setAccessible(true);
            nmsItemGetCraftingRemainingItem.setAccessible(true);
            nmsItemBlockGetBlock.setAccessible(true);
            nmsItemStackGetItem.setAccessible(true);
            nmsEntityWorld.setAccessible(true);
            nmsTileEntityGetWorld.setAccessible(true);
            nmsTileEntityGetPosition.setAccessible(true);
            nmsTileEntityLoad.setAccessible(true);
            nmsTileEntitySetLocation.setAccessible(true);
            nmsTileEntitySetPosition.setAccessible(true);
            nmsTileEntitySave.setAccessible(true);
            nmsTileEntityHopperSetCooldown.setAccessible(true);
            nmsSoundEffectTypeGetStepSound.setAccessible(true);
            nmsWorldSpigotConfig.setAccessible(true);
        }
        catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException ex) {
            logReflectionError("initial", ex);
        }
    }

    public static ItemStack getCraftingRemainder(ItemStack item) {
        try {
            if (obcCraftItemStack.isInstance(item)) {
                Object nmsItemObj = nmsItemStackGetItem.invoke(obcCraftItemStackHandle.get(item));
                Object nmsItemObjResult = nmsItemGetCraftingRemainingItem.invoke(nmsItemObj);
                ItemStack result = (ItemStack) obcCraftItemStackAsNewCraftStack.invoke(nmsItemObjResult);
                return Utils.isEmpty(result) ? null : result;
            }
        }
        catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException ex) {
            logReflectionError("Item#getCraftingRemainingItem()", ex);
        }
        return null;
    }

    public static boolean isSpecialToolRequired(Block block) {
        try {
            return (boolean) nmsBlockDataIsRequiresSpecialTool.invoke(obcCraftBlockGetNMS.invoke(block));
        }
        catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException ex) {
            logReflectionError("BlockData#isRequiresSpecialTool()", ex);
        }
        return true;
    }

    public static boolean canBreakSpecialBlock(Block block, ItemStack item) {
        try {
            Object nmsItemStackObj = obcCraftItemStackHandle.get(item);
            Object nmsItemObj = nmsItemStackGetItem.invoke(nmsItemStackObj);
            Object nmsIBlockDataObj = obcCraftBlockGetNMS.invoke(block);

            return (boolean) nmsItemCanDestroySpecialBlock.invoke(nmsItemObj, nmsIBlockDataObj);
        }
        catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException ex) {
            logReflectionError("Item#canBreakSpecialBlock()", ex);
        }
        return false;
    }

    public static float getDestroySpeed(Block block, ItemStack tool) {
        try {
            if (obcCraftItemStack.isInstance(tool) && obcCraftBlock.isInstance(block)) {
                Object itemStack = obcCraftItemStackHandle.get(tool);
                Object item = nmsItemStackGetItem.invoke(itemStack);
                Object blockData = obcCraftBlockGetNMS.invoke(block);
                return (float) nmsItemGetDestroySpeed.invoke(item, itemStack, blockData);
            }
        }
        catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException ex) {
            logReflectionError("Item#getDestroySpeed()", ex);
        }
        return 1.0F;
    }

    public static void updateBlockBreakAnimation(int id, Block block, int stage, Player player) {
        BlockPosition pos = ((CraftBlock) block).getPosition();
        PacketPlayOutBlockBreakAnimation packet = new PacketPlayOutBlockBreakAnimation(id, pos, stage);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    public static void makeBlockHitSound(Block block) {
        SoundEffect s = ((CraftBlock) block).getNMS().getStepSound().d();
        ((CraftWorld) block.getWorld()).getHandle().playSound((EntityHuman) null, ((CraftBlock) block).getPosition(), s, SoundCategory.BLOCKS, 0.6F, 0.65F);

    }

    public static boolean canPlaceOn(ItemStack item, Block block) {
        try {
            Object nmsWorldObj = obcCraftWorldGetHandle.invoke(block.getWorld());
            Object nmsBlockPositionObj = obcCraftBlockGetPosition.invoke(block);
            Object nmsItemStackObj = obcCraftItemStackHandle.get(item);
            Object nmsItemObj = nmsItemStackGetItem.invoke(nmsItemStackObj);

            if (nmsItemBlock.isInstance(nmsItemObj)) {

                Object nmsBlockObj = nmsItemBlockGetBlock.invoke(nmsItemObj);
                Object nmsBlockDataObj = nmsBlockGetBlockData.invoke(nmsBlockObj);

                return (boolean) nmsBlockDataCanPlace.invoke(nmsBlockDataObj, nmsWorldObj, nmsBlockPositionObj);
            }
        }
        catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException ex) {
            logReflectionError("IBlockData#canPlace()", ex);
        }
        return false;
    }

    public static void copyBlockState(BlockState source, BlockState dest) {
        if (source == null || dest == null || !source.getClass().equals(dest.getClass())) {
            throw new IllegalArgumentException("source and dest must be non-null and have the same class.");
        }
        try {
            System.out.println("dest: " + dest);
            System.out.println("source: " + source);

            // modifies dest's tile entity snapshot so that its next update applies changes to the world
            if (obcCraftBlockEntityState.isInstance(dest) && obcCraftBlockEntityState.isInstance(source)) {

                System.out.println("old dest nbt: " + nmsTileEntitySave.invoke(obcCraftBlockEntityStateGetSnapshot.invoke(dest), nmsNbtCompoundConstructor.newInstance()));
                System.out.println("source nbt: " + nmsTileEntitySave.invoke(obcCraftBlockEntityStateGetSnapshot.invoke(source), nmsNbtCompoundConstructor.newInstance()));

                obcCraftBlockEntityStateLoad.invoke(dest, obcCraftBlockEntityStateGetSnapshot.invoke(source));

                System.out.println("new dest nbt: " + nmsTileEntitySave.invoke(obcCraftBlockEntityStateGetSnapshot.invoke(dest), nmsNbtCompoundConstructor.newInstance()));
            }
        }
        catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException | InstantiationException ex) {
            logReflectionError("CraftBlockEntityState#load()", ex);
        }
    }

    public static void setCustomContainerName(Container container, String name) {
        // necessary because the API for some reason doesn't mark all container types as Nameable
        try {
            obcCraftContainerSetCustomName.invoke(container, name);
        }
        catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException ex) {
            logReflectionError("CraftContainer#setCustomName()", ex);
        }
    }

    public static void copySkullProfile(SkullMeta from, Skull to) {
        try {
            obcCraftSkullProfile.set(to, obcCraftMetaSkullProfile.get(from));
        }
        catch (IllegalAccessException ex) {
            logReflectionError("CraftSkull#profile", ex);
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
        catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException ex) {
            logReflectionError("InventoryHolder...World#spigotConfig", ex);
        }
        return null;
    }

    public static void setHopperCooldown(Hopper hopper, int cooldown) {
        try {
            nmsTileEntityHopperSetCooldown.invoke(obcCraftBlockEntityStateGetTileEntity.invoke(hopper), cooldown);
        }
        catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException ex) {
            logReflectionError("TileEntityHopper#cooldown", ex);
        }
    }
}

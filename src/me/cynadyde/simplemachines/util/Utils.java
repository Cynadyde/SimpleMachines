package me.cynadyde.simplemachines.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Dropper;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class Utils {

    private Utils() {}

    /**
     * The plugin's random number generator.
     */
    public static final Random RNG = new Random();

    /**
     * The six faces of a cube.
     */
    public static final List<BlockFace> FACES = Collections.unmodifiableList(Arrays.asList(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));

    public static final List<Material> TOOLS = Collections.unmodifiableList(Arrays.asList(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
            Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.SHEARS
    ));

    /**
     * Tests if the given trapdoor is covering the given face.
     */
    public static boolean isCoveringFace(TrapDoor trapdoor, BlockFace face) {
        switch (face) {
            case UP:
                return !trapdoor.isOpen() && trapdoor.getHalf() == Bisected.Half.BOTTOM;
            case DOWN:
                return !trapdoor.isOpen() && trapdoor.getHalf() == Bisected.Half.TOP;
            default:
                return trapdoor.isOpen() && trapdoor.getFacing() == face;
        }
    }

    /**
     * Drops an item stack into the world as if from the given dropper.
     */
    public static void dropFromDropper(Dropper dropper, ItemStack item) {
        /* ugh, kinda backwards to manually code the item being dispensed, but the
            alternative creates another dispense event. (which then has to be isolated) */

        BlockFace facing = ((Directional) dropper.getBlockData()).getFacing();
        Block adjacent = dropper.getBlock().getRelative(facing);

        List<ItemStack> spill = new ArrayList<>();

        if (adjacent.getState() instanceof Container) {
            // attempts to add the items to the container, returning anything that didn't fit
            spill.addAll(((Container) adjacent.getState()).getInventory().addItem(item).values());
        }
        else {
            spill.add(item);
        }
        if (!spill.isEmpty()) {
            ItemStack spilled = spill.get(0); // should never have more than 1

            double x = dropper.getX() + 0.5 + (0.7 * facing.getModX());
            double y = dropper.getY() + 0.5 + (0.7 * facing.getModY()) - (facing == BlockFace.DOWN || facing == BlockFace.UP ? 0.125 : 0.15625);
            double z = dropper.getZ() + 0.5 + (0.7 * facing.getModZ());

            Item drop = dropper.getWorld().dropItem(new Location(null, x, y, z), spilled);

            double offset = (RNG.nextDouble() * 0.1) + 0.2;
            drop.setVelocity(new Vector(
                    (RNG.nextGaussian() * 0.007499999832361937 * 6D) + ((double) facing.getModX() * offset),
                    (RNG.nextGaussian() * 0.007499999832361937 * 6D) + 0.20000000298023224,
                    (RNG.nextGaussian() * 0.007499999832361937 * 6D) + ((double) facing.getModZ() * offset)
            ));
        }
    }
}

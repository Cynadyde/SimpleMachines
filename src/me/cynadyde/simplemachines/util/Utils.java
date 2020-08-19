package me.cynadyde.simplemachines.util;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Utils {

    private Utils() {}

    /**
     * The six faces of a cube.
     */
    public static final List<BlockFace> FACES = Collections.unmodifiableList(Arrays.asList(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));

    /**
     * The eight types of trapdoors.
     */
    public static final List<Material> TRAPDOORS = Collections.unmodifiableList(Arrays.asList(
            Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.JUNGLE_TRAPDOOR,
            Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR, Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR
    ));

    /**
     * The four pairs of trapdoors corresponding to the four transfer policy types.
     */
    public static final List<List<Material>> TRAPDOOR_GROUPS = Collections.unmodifiableList(Arrays.asList(
            Collections.unmodifiableList(Arrays.asList(Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR)),
            Collections.unmodifiableList(Arrays.asList(Material.DARK_OAK_TRAPDOOR, Material.OAK_TRAPDOOR)),
            Collections.unmodifiableList(Arrays.asList(Material.JUNGLE_TRAPDOOR, Material.CRIMSON_TRAPDOOR)),
            Collections.unmodifiableList(Arrays.asList(Material.ACACIA_TRAPDOOR, Material.WARPED_TRAPDOOR))
    ));

    /**
     * Tests if the given trapdoor is covering the given face.
     */
    public static boolean isTrapdoorCoveringFace(TrapDoor trapdoor, BlockFace face) {
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
     * Creates an item stack (amount 1) with the given display name and lore.
     * Use '§' for color codes.
     */
    public static ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}

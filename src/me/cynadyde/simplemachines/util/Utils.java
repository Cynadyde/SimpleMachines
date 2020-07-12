package me.cynadyde.simplemachines.util;

import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.TrapDoor;

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

}

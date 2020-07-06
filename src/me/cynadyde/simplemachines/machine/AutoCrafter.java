package me.cynadyde.simplemachines.machine;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public class AutoCrafter extends Machine {

    private Block bottom;
    private Block middle;
    private Block top;

    public AutoCrafter(Block block) throws IllegalArgumentException {
        boolean correct = false;

        inspect:
        if (block != null) {
            switch (block.getType()) {
                default:
                    break inspect;
                case CRAFTING_TABLE:
                    this.bottom = block;
                    this.middle = block.getRelative(BlockFace.UP, 1);
                    this.top = block.getRelative(BlockFace.UP, 2);
                    break;
                case DISPENSER:
                    this.bottom = block.getRelative(BlockFace.DOWN, 1);
                    this.middle = block;
                    this.top = block.getRelative(BlockFace.UP, 1);
                    break;
                case BLAST_FURNACE:
                    this.bottom = block.getRelative(BlockFace.DOWN, 2);
                    this.middle = block.getRelative(BlockFace.DOWN, 1);
                    this.top = block;
                    break;
            }
            if (bottom.getType() == Material.CRAFTING_TABLE) {
                if (middle.getType() == Material.DISPENSER
                        && ((Directional) middle.getBlockData()).getFacing() == BlockFace.DOWN) {
                    if (top.getType() == Material.BLAST_FURNACE) {
                        correct = true;
                    }
                }
            }
        }
        if (!correct) {
            throw new IllegalArgumentException("That block was not part of an AutoCrafter");
        }
    }

    @Override
    public Block getPos() {
        return bottom;
    }

    @Override
    public void interactedBy(Player player) {

    }
}

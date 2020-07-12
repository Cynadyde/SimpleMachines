package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.Utils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.inventory.Inventory;

public class TransferDestPolicy {

    public final RetrievePolicy RETRIEVE;
    public final InputPolicy INPUT;

    public static TransferDestPolicy ofInventory(Inventory inv) {
        if (inv == null
                || inv.getHolder() == null
                || !(inv.getHolder() instanceof Container)) {

            return new TransferDestPolicy(RetrievePolicy.NORMAL, InputPolicy.NORMAL);
        }
        return ofContainer((Container) inv.getHolder());
    }

    public static TransferDestPolicy ofContainer(Container container) {
        if (!container.isPlaced()) {
            return new TransferDestPolicy(RetrievePolicy.NORMAL, InputPolicy.NORMAL);
        }

        Block block = container.getBlock();
        RetrievePolicy retrieve = RetrievePolicy.NORMAL;
        InputPolicy input = InputPolicy.NORMAL;

        for (BlockFace face : Utils.FACES) {
            Block adjacent = block.getRelative(face);
            switch (adjacent.getType()) {
                case ACACIA_TRAPDOOR:
                    if (Utils.isTrapdoorCoveringFace((TrapDoor) adjacent.getBlockData(), face)) {
                        retrieve = RetrievePolicy.REVERSED;
                    }
                    break;
                case SPRUCE_TRAPDOOR:
                    if (Utils.isTrapdoorCoveringFace((TrapDoor) adjacent.getBlockData(), face)) {
                        input = InputPolicy.MAX_ONE;
                    }
                    break;
                case BIRCH_TRAPDOOR:
                    if (Utils.isTrapdoorCoveringFace((TrapDoor) adjacent.getBlockData(), face)) {
                        input = InputPolicy.LOCK_EMPTY;
                    }
                    break;
                case WARPED_TRAPDOOR:
                    if (Utils.isTrapdoorCoveringFace((TrapDoor) adjacent.getBlockData(), face)) {
                        retrieve = RetrievePolicy.RANDOM;
                    }
                    break;
            }
        }
        return new TransferDestPolicy(retrieve, input);
    }

    public TransferDestPolicy(RetrievePolicy retrieve, InputPolicy input) {
        this.RETRIEVE = retrieve;
        this.INPUT = input;
    }

    public boolean isNonNormal() {
        return this.INPUT != InputPolicy.NORMAL
                || this.RETRIEVE != RetrievePolicy.NORMAL;
    }
}

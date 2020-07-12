package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.Utils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.inventory.Inventory;

public class TransferSourcePolicy {

    public final ServePolicy SERVE;
    public final OutputPolicy OUTPUT;

    public static TransferSourcePolicy ofInventory(Inventory inv) {
        if (inv == null
                || inv.getHolder() == null
                || !(inv.getHolder() instanceof Container)) {

            return new TransferSourcePolicy(ServePolicy.NORMAL, OutputPolicy.NORMAL);
        }
        return ofContainer((Container) inv.getHolder());
    }

    public static TransferSourcePolicy ofContainer(Container container) {
        if (!container.isPlaced()) {
            return new TransferSourcePolicy(ServePolicy.NORMAL, OutputPolicy.NORMAL);
        }
        Block block = container.getBlock();
        ServePolicy serve = ServePolicy.NORMAL;
        OutputPolicy output = OutputPolicy.NORMAL;

        for (BlockFace face : Utils.FACES) {
            Block adjacent = block.getRelative(face);
            switch (adjacent.getType()) {
                case OAK_TRAPDOOR:
                    // unused...
                    break;
                case DARK_OAK_TRAPDOOR:
                    if (Utils.isTrapdoorCoveringFace((TrapDoor) adjacent.getBlockData(), face)) {
                        output = OutputPolicy.MIN_ONE;
                    }
                case JUNGLE_TRAPDOOR:
                    if (Utils.isTrapdoorCoveringFace((TrapDoor) adjacent.getBlockData(), face)) {
                        serve = ServePolicy.REVERSED;
                    }
                    break;
                case CRIMSON_TRAPDOOR:
                    if (Utils.isTrapdoorCoveringFace((TrapDoor) adjacent.getBlockData(), face)) {
                        serve = ServePolicy.RANDOM;
                    }
                    break;
            }
        }
        return new TransferSourcePolicy(serve, output);
    }

    public TransferSourcePolicy(ServePolicy serve, OutputPolicy output) {
        this.SERVE = serve;
        this.OUTPUT = output;
    }

    public boolean isNonNormal() {
        return this.OUTPUT != OutputPolicy.NORMAL
                || this.SERVE != ServePolicy.NORMAL;
    }
}

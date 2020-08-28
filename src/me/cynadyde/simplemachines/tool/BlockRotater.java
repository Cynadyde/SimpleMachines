package me.cynadyde.simplemachines.tool;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.util.ItemUtils;
import org.bukkit.Axis;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BlockRotater implements Listener {

    private final SimpleMachinesPlugin plugin;

    public BlockRotater(SimpleMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // anti grief plugins should have been able to cancel the event by now
        if (event.getClickedBlock() != null && event.getItem() != null) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
                if (ItemUtils.HOES.contains(event.getMaterial())) {

                    Block block = event.getClickedBlock();
                    BlockData original = block.getBlockData();
                    BlockData changed = safelyRotated(block);
                    if (!original.matches(changed)) {

                        // make sure player can build here
                        Block against = block.getRelative(event.getBlockFace().getOppositeFace(), 1);
                        BlockPlaceEvent e = new BlockPlaceEvent(block, block.getState(), against, event.getItem(), event.getPlayer(), true, EquipmentSlot.HAND);
                        plugin.getServer().getPluginManager().callEvent(e);
                        if (!e.isCancelled()) {
                            block.setBlockData(changed);
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    private BlockData safelyRotated(Block block) {
        BlockData data = block.getBlockData();

        if (data instanceof Slab) {
            Slab slab = ((Slab) data);
            Slab.Type type = slab.getType();

            if (type == Slab.Type.TOP) {
                slab.setType(Slab.Type.BOTTOM);
            }
            else if (type == Slab.Type.BOTTOM) {
                slab.setType(Slab.Type.TOP);
            }
        }
        else if (data instanceof Stairs || data instanceof TrapDoor) {
            Bisected bisected = ((Bisected) data);
            Directional directional = ((Directional) data);
            Bisected.Half half = bisected.getHalf();
            BlockFace facing = directional.getFacing();

            if (facing == BlockFace.NORTH) {
                directional.setFacing(BlockFace.EAST);
            }
            else if (facing == BlockFace.EAST) {
                directional.setFacing(BlockFace.SOUTH);
            }
            else if (facing == BlockFace.SOUTH) {
                directional.setFacing(BlockFace.WEST);
            }
            else if (facing == BlockFace.WEST) {
                directional.setFacing(BlockFace.NORTH);

                if (half == Bisected.Half.BOTTOM) {
                    bisected.setHalf(Bisected.Half.TOP);
                }
                else if (half == Bisected.Half.TOP) {
                    bisected.setHalf(Bisected.Half.BOTTOM);
                }
            }
        }
        else if (data instanceof Orientable) {
            Orientable orientable = ((Orientable) data);
            Axis axis = orientable.getAxis();
            orientable.setAxis(rotateValue(orientable.getAxes(), axis));
        }
        else if (data instanceof Directional) {
            Directional directional = ((Directional) data);
            BlockFace facing = directional.getFacing();

            if (data instanceof WallSign
                    || (data instanceof Switch && ((Switch) data).getAttachedFace() == FaceAttachable.AttachedFace.WALL)
                    || ItemUtils.TORCHES.contains(data.getMaterial())) {

                List<BlockFace> faces = directional.getFaces().stream()
                        .sorted(Comparator.comparing(Enum::ordinal))
                        .collect(Collectors.toList());

                int init = faces.indexOf(facing);
                int i;
                do {
                    i = (faces.indexOf(facing) + 1) % faces.size();
                    facing = faces.get(i);
                }
                while (i != init && !isAttachable(block.getRelative(facing, -1), facing));
                directional.setFacing(facing);
            }
            else {
                directional.setFacing(rotateValue(directional.getFaces(), facing));
            }
        }
        else if (data instanceof Rotatable) {
            Rotatable rotatable = ((Rotatable) data);
            BlockFace rotation = rotatable.getRotation();
            rotatable.setRotation(rotateValue(ItemUtils.ROTATIONS, rotation));
        }
        return data;
    }

    private <T extends Enum<?>> T rotateValue(Collection<T> values, T value) {
        return rotateValue(values.stream().sorted(Comparator.comparing(Enum::ordinal)).collect(Collectors.toList()), value);
    }

    private <T extends Enum<?>> T rotateValue(List<T> values, T value) {
        int i = (values.indexOf(value) + 1) % values.size();
        return values.get(i);
    }

    private boolean isAttachable(Block block, BlockFace face) {
        return block.getType().isOccluding();
    }
}

package me.cynadyde.simplemachines;

import me.cynadyde.simplemachines.machine.AutoCrafter;
import me.cynadyde.simplemachines.machine.Machine;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleMachinesPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        // player is (non-sneak) left-clicking a block
        if (event.useInteractedBlock() != Event.Result.DENY
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && !event.getPlayer().isSneaking()) {

            // that block is part of a multi-block machine structure
            Machine machine = getMachine(event.getClickedBlock());
            if (machine != null) {
                machine.interactedBy(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {

    }

    public void onBlockDispense(BlockDispenseEvent event) {

    }


    public Machine getMachine(Block block) {
        Machine result = null;
        try {
            result = new AutoCrafter(block);
        }
        catch (IllegalArgumentException ignored) {}
        return result;
    }
}

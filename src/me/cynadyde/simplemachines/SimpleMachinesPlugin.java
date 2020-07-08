package me.cynadyde.simplemachines;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.stream.IntStream;

public class SimpleMachinesPlugin extends JavaPlugin implements Listener {

    // TODO change up hopper mechanics a little bit:
    //   should be able to serve items in round-robin order

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new AutoCrafter(this), this);
        getServer().getPluginManager().registerEvents(new HopperFilter(this), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command == getCommand("insertitem")) {
            if (sender instanceof Player) {
                ItemStack item = ((Player) sender).getInventory().getItemInMainHand();
                if (!item.getType().isAir()) {

                    System.out.println("player holding " + item);

                    Block target = ((Player) sender).getTargetBlockExact(16);
                    if (target != null && target.getState() instanceof Container) {

                        System.out.println("player looking at " + target);

                        performItemInput(
                                item,
                                (InventoryHolder) target.getState(),
                                HopperFilter.RetrievePolicy.REVERSED,
                                HopperFilter.InputPolicy.NORMAL);

//                        ((Container) target.getState()).getInventory().setItem(26, item);
//                        target.getState().update(false, false);

                        System.out.println("inserted item!");
                        System.out.println("=====================");
                    }
                }
            }
        }
        return true;
    }

    public ItemStack performItemInput(ItemStack served, InventoryHolder dest, HopperFilter.RetrievePolicy retrieve, HopperFilter.InputPolicy input) {

        int size = dest.getInventory().getSize();
        Iterator<Integer> slots = retrieve == HopperFilter.RetrievePolicy.RANDOM
                ? new RandomPermuteIterator(0, size)
                : IntStream.range(0, size).iterator();

        int leftoverAmount = served.getAmount();

        while (slots.hasNext() && leftoverAmount > 0) {
            int slot = slots.next();
            if (retrieve == HopperFilter.RetrievePolicy.REVERSED) {
                slot = size - 1 - slot;
            }
            ItemStack current = dest.getInventory().getItem(slot);
            if (current == null || current.getAmount() < 1) {
                if (input != HopperFilter.InputPolicy.LOCK_EMPTY) {

                    System.out.println("FOUND EMPTY SPACE FOR " + served + " AT SLOT " + slot + " WOOT");

                    dest.getInventory().setItem(slot, served);
                    leftoverAmount = 0;
                }
            }
            else if (served.isSimilar(current)) {
                if (input != HopperFilter.InputPolicy.MAX_ONE) {

                    int total = served.getAmount() + current.getAmount();
                    int overflow = Math.max(0, total - served.getMaxStackSize());

                    total -= overflow;
                    leftoverAmount -= (total - current.getAmount());

                    System.out.println("FOUND FREE SPACE FOR " + served + " AT SLOT " + slot + " WITH " + current + " (total " + total + ") WOOT (" + leftoverAmount + " left)");
                    current.setAmount(total);
                    dest.getInventory().setItem(slot, current);
                }
            }
        }

        // if nothing was able to be changed, just return the input
//        if (dest instanceof Container) {
//            if (!((BlockState) dest).update(false, false)) {
//                System.out.println("uh oh couldn't update container...");
//                System.out.println("++++++++++++++");
//                return served;
//            }
//            System.out.println("able to update container!");
//        }
        if (leftoverAmount == 0) {
            System.out.println("NOTHING LEFT OVER!");
            System.out.println("++++++++++++++");
            return null;
        }
        else {
            ItemStack leftover = served.clone();
            leftover.setAmount(leftoverAmount);
            System.out.println("in total, " + leftoverAmount + " was left over...");
            System.out.println("++++++++++++++");
            return leftover;
        }
    }
}

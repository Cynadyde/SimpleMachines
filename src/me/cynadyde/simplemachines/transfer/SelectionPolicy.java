package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.PluginKey;
import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import me.cynadyde.simplemachines.util.RoundRobinIterator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * The order to search for a slot to input/output from.
 */
public enum SelectionPolicy implements TransferPolicy {
    NORMAL {
        @Override
        public Iterator<Integer> getIterator(InventoryHolder holder) {
            int length = holder.getInventory().getSize();
            return IntStream.range(0, length).iterator();
        }

        @Override
        public Material getToken() {
            return Material.AIR;
        }
    },
    REVERSED {
        @Override
        public Iterator<Integer> getIterator(InventoryHolder holder) {
            int length = holder.getInventory().getSize();
            AtomicInteger counter = new AtomicInteger(length);
            return IntStream.generate(counter::decrementAndGet).limit(length).iterator();
        }

        @Override
        public Material getToken() {
            return Material.ACACIA_TRAPDOOR;
        }
    },
    RANDOM {
        @Override
        public Iterator<Integer> getIterator(InventoryHolder holder) {
            int length = holder.getInventory().getSize();
            return new RandomPermuteIterator(0, length);
        }

        @Override
        public Material getToken() {
            return Material.WARPED_TRAPDOOR;
        }
    },
    ROUND_ROBIN {
        @Override
        public Iterator<Integer> getIterator(InventoryHolder holder) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("SimpleMachines");
            return new RoundRobinIterator(plugin, holder);
        }

        @Override
        public Material getToken() {
            return Material.CRIMSON_TRAPDOOR;
        }
    };

    public abstract Iterator<Integer> getIterator(InventoryHolder holder);

    public static SelectionPolicy fromToken(Material token) {
        for (SelectionPolicy policy : values()) {
            if (policy.getToken() == token) {
                return policy;
            }
        }
        return null;
    }
}

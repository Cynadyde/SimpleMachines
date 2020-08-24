package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.PluginKey;
import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import org.bukkit.Material;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
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
            final int lastIndex = length - 1;
            return IntStream.range(0, length).map(n -> lastIndex - n).iterator();
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
            int length = holder.getInventory().getSize();
            int start = 0;
            if (holder instanceof PersistentDataHolder) {
                PersistentDataContainer pdc = ((PersistentDataHolder) holder).getPersistentDataContainer();
                Byte data = pdc.get(PluginKey.LATEST_SLOT.get(), PersistentDataType.BYTE);
                if (data != null) {
                    start = (int) data;
                }
            }
            return IntStream.concat(IntStream.range(start, length), IntStream.range(0, start)).iterator();
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

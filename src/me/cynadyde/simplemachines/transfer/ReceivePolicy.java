package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import org.bukkit.Material;

import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * The order to search for a slot to put a transferred item into.
 */
public enum ReceivePolicy implements TransferPolicy {
    NORMAL {
        @Override
        public Iterator<Integer> getIterator(int length) {
            return IntStream.range(0, length).iterator();
        }

        @Override
        public Material getToken() {
            return Material.AIR;
        }
    },
    REVERSED {
        @Override
        public Iterator<Integer> getIterator(int length) {
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
        public Iterator<Integer> getIterator(int length) {
            return new RandomPermuteIterator(0, length);
        }

        @Override
        public Material getToken() {
            return Material.WARPED_TRAPDOOR;
        }
    };

    public abstract Iterator<Integer> getIterator(int length);

    public static ReceivePolicy fromToken(Material token) {
        for (ReceivePolicy policy : values()) {
            if (policy.getToken() == token) {
                return policy;
            }
        }
        return null;
    }
}

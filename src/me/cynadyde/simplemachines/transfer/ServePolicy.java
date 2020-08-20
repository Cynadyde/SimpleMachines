package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.RandomPermuteIterator;
import org.bukkit.Material;

import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * The order to search for an item to take out when transferring.
 */
public enum ServePolicy implements TransferPolicy {
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
            return Material.JUNGLE_TRAPDOOR;
        }
    },
    RANDOM {
        @Override
        public Iterator<Integer> getIterator(int length) {
            return new RandomPermuteIterator(0, length);
        }

        @Override
        public Material getToken() {
            return Material.CRIMSON_TRAPDOOR;
        }
    };

    public abstract Iterator<Integer> getIterator(int length);

    public static ServePolicy fromToken(Material token) {
        for (ServePolicy policy : values()) {
            if (policy.getToken() == token) {
                return policy;
            }
        }
        return null;
    }
}

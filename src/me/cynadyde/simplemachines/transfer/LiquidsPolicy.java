package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.ItemUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum LiquidsPolicy implements TransferPolicy {

    NORMAL {
        @Override
        public boolean isDrainable(ItemStack item) {
            return false;
        }

        @Override
        public boolean isFillable(ItemStack item) {
            return false;
        }

        @Override
        public Material getToken() {
            return Material.AIR;
        }
    },

    POUR_INTO {
        @Override
        public boolean isDrainable(ItemStack item) {
            return item != null && item.getAmount() == 1 && ItemUtils.FILLED_BUCKETS.contains(item.getType());
        }

        @Override
        public boolean isFillable(ItemStack item) {
            return item != null && item.getAmount() == 1 && item.getType() == Material.BUCKET;
        }

        @Override
        public Material getToken() {
            return Material.JUNGLE_TRAPDOOR;
        }
    };

    public abstract boolean isDrainable(ItemStack item);

    public abstract boolean isFillable(ItemStack item);

    public static LiquidsPolicy fromToken(Material token) {
        for (LiquidsPolicy policy : values()) {
            if (policy.getToken() == token) {
                return policy;
            }
        }
        return null;
    }
}

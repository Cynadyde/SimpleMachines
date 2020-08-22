package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum LiquidsPolicy implements TransferPolicy {

    NORMAL {
        @Override
        public boolean isFlowing(ItemStack item) {
            return false;
        }

        @Override
        public boolean isDrained(ItemStack item) {
            return false;
        }

        @Override
        public Material getToken() {
            return Material.AIR;
        }
    },

    FLOW {
        @Override
        public boolean isFlowing(ItemStack item) {
            return item != null && Utils.FILLED_BUCKETS.contains(item.getType()) && item.getAmount() == 1;
        }

        @Override
        public boolean isDrained(ItemStack item) {
            return item != null && item.getType() == Material.BUCKET && item.getAmount() == 1;
        }

        @Override
        public Material getToken() {
            return Material.IRON_TRAPDOOR;
        }
    };

    public static LiquidsPolicy fromToken(Material token) {
        for (LiquidsPolicy policy : values()) {
            if (policy.getToken() == token) {
                return policy;
            }
        }
        return null;
    }

    public abstract boolean isFlowing(ItemStack item);

    public abstract boolean isDrained(ItemStack item);
}

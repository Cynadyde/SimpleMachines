package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * The rules when choosing a slot to have an item transferred in.
 */
public enum InputPolicy implements TransferPolicy {

    /** Any slot may be ok to give items to. */
    NORMAL {
        @Override
        public boolean testSlot(ItemStack slot) {
            return !Utils.isFull(slot);
        }

        @Override
        public Material getToken() {
            return Material.AIR;
        }
    },

    /** Only empty slots are ok to give items to. */
    TO_EMPTY {
        @Override
        public boolean testSlot(ItemStack slot) {
            return Utils.isEmpty(slot);
        }

        @Override
        public Material getToken() {
            return Material.SPRUCE_TRAPDOOR;
        }
    },

    /** Only non-empty slots are ok to give items to. */
    TO_NONEMPTY {
        @Override
        public boolean testSlot(ItemStack slot) {
            return !Utils.isEmpty(slot) && !Utils.isFull(slot);
        }

        @Override
        public Material getToken() {
            return Material.BIRCH_TRAPDOOR;
        }
    };

    public abstract boolean testSlot(ItemStack slot);

    public static InputPolicy fromToken(Material token) {
        for (InputPolicy policy : values()) {
            if (policy.getToken() == token) {
                return policy;
            }
        }
        return null;
    }
}

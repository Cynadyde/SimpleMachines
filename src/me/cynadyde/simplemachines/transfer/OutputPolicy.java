package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.ItemUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;

/**
 * The rules when choosing a slot to have an item transferred out.
 */
public enum OutputPolicy implements TransferPolicy {

    /** Any slot may be ok to take from. */
    NORMAL {
        @Override
        public boolean testSlot(ItemStack slot) {
            return !ItemUtils.isEmpty(slot);
        }

        @Override
        public Material getToken() {
            return Material.AIR;
        }
    },

    /** Only unfilled slots with a single item are ok to take from. */
    FROM_SOLO {
        @Override
        public boolean testSlot(ItemStack slot) {
            return !ItemUtils.isEmpty(slot) && !ItemUtils.isFull(slot) && slot.getAmount() == 1;
        }

        @Override
        public Material getToken() {
            return Material.OAK_TRAPDOOR;
        }
    },

    /** Only filled slots or those with multiple items are ok to take from. */
    FROM_NONSOLO {
        @Override
        public boolean testSlot(ItemStack slot) {
            return !ItemUtils.isEmpty(slot) && (ItemUtils.isFull(slot) || slot.getAmount() > 1);
        }

        @Override
        public Material getToken() {
            return Material.DARK_OAK_TRAPDOOR;
        }
    };

    @Contract("null -> false")
    public abstract boolean testSlot(ItemStack slot);

    public static OutputPolicy fromToken(Material token) {
        for (OutputPolicy policy : values()) {
            if (policy.getToken() == token) {
                return policy;
            }
        }
        return null;
    }
}

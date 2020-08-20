package me.cynadyde.simplemachines.transfer;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;

import java.util.function.Predicate;

/**
 * The rules when choosing a slot to have an item transferred out.
 */
public enum OutputPolicy implements Predicate<ItemStack> {

    /** Any slot may be ok to take from. */
    NORMAL {
        @Override
        public boolean test(ItemStack slot) {
            return slot != null;
        }
    },

    /** Only unfilled slots with a single item are ok to take from. */
    FROM_SOLO {
        @Override
        public boolean test(ItemStack slot) {
            return slot != null && (slot.getAmount() == 1 && slot.getMaxStackSize() != 1);
        }
    },

    /** Only filled slots or those with multiple items are ok to take from. */
    FROM_NONSOLO {
        @Override
        public boolean test(ItemStack slot) {
            return slot != null && (slot.getAmount() > 1 || slot.getMaxStackSize() == 1);
        }
    };

    @Contract("null -> false")
    public abstract boolean test(ItemStack slot);
}

package me.cynadyde.simplemachines.transfer;

import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

/**
 * The rules when choosing a slot to have an item transferred in.
 */
public enum InputPolicy implements Predicate<ItemStack> {

    /** Any slot may be ok to give items to. */
    NORMAL {
        @Override
        public boolean test(ItemStack slot) {
            return true;
        }
    },

    /** Only empty slots are ok to give items to. */
    TO_EMPTY {
        @Override
        public boolean test(ItemStack slot) {
            return slot == null || slot.getType().isAir() || slot.getAmount() <= 0;
        }
    },

    /** Only non-empty slots are ok to give items to. */
    TO_NONEMPTY {
        @Override
        public boolean test(ItemStack slot) {
            return slot != null && !slot.getType().isAir() && slot.getAmount() > 0;
        }
    }
}

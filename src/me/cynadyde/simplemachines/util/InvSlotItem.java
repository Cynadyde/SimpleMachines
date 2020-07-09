package me.cynadyde.simplemachines.util;

import org.bukkit.inventory.ItemStack;

/**
 * An item stack and the raw inventory slot number it was found at,
 * enabling a recall on a transferred item to be done if needed.
 */
public class InvSlotItem {

    private final int slot;
    private final ItemStack item;

    public InvSlotItem(int slot, ItemStack item) {
        this.slot = slot;
        this.item = item;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItem() {
        return item;
    }
}

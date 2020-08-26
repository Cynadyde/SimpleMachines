package me.cynadyde.simplemachines.util;

import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
import java.util.stream.IntStream;

public class RoundRobinIterator implements Iterator<Integer> {

    private final InventoryHolder holder;
    private final Iterator<Integer> iter;

    public RoundRobinIterator(InventoryHolder holder) {
        int length = holder.getInventory().getSize();
        int start = 0;
        System.out.println("new round robin iterator with holder: " + holder);
        if (holder instanceof PersistentDataHolder) {
            PersistentDataContainer pdc = ((PersistentDataHolder) holder).getPersistentDataContainer();
            Byte data = pdc.get(PluginKey.LATEST_SLOT.get(), PersistentDataType.BYTE);
            if (data != null) {
                System.out.println("existing latest slot: " + data);
                start = ((int) data) + 1;
            }
        }
        this.holder = holder;
        this.iter = IntStream.concat(IntStream.range(start, length), IntStream.range(0, start)).iterator();
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public Integer next() {
        Integer result = iter.next();
        if (holder instanceof PersistentDataHolder && result != null) {
            PersistentDataContainer pdc = ((PersistentDataHolder) holder).getPersistentDataContainer();
            System.out.println("new latest slot: " + result.byteValue());
            pdc.set(PluginKey.LATEST_SLOT.get(), PersistentDataType.BYTE, result.byteValue());
            System.out.println("saved last slot: " + ((PersistentDataHolder) holder).getPersistentDataContainer().get(PluginKey.LATEST_SLOT.get(), PersistentDataType.BYTE));
            if (holder instanceof BlockState) {
                System.out.println("woo?");
                System.out.println("success? " + ((BlockState) holder).update(false, false));
            }
        }
        return result;
    }
}

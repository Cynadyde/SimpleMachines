package me.cynadyde.simplemachines.util;

import org.bukkit.inventory.InventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

import java.util.Iterator;
import java.util.stream.IntStream;

public class RoundRobinIterator implements Iterator<Integer> {

    private final Plugin plugin;
    private final InventoryHolder holder;
    private final Iterator<Integer> iter;

    public RoundRobinIterator(Plugin plugin, InventoryHolder holder) {
        int length = holder.getInventory().getSize();
        int start = 0;
        if (holder instanceof Metadatable) {
            MetadataValue val = ((Metadatable) holder).getMetadata(PluginKey.LATEST_SLOT.get().getKey()).stream()
                    .filter(v -> plugin.equals(v.getOwningPlugin())).findFirst().orElse(null);
            if (val != null) {
                start = val.asByte();
            }
        }
        this.plugin = plugin;
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
        if (holder instanceof Metadatable && result != null) {
            ((Metadatable) holder).setMetadata(PluginKey.LATEST_SLOT.get().getKey(),
                    new FixedMetadataValue(plugin, result.byteValue()));
        }
        return result;
    }
}

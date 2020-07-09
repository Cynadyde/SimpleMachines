package me.cynadyde.simplemachines.util;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

public class InvMovePair {

    private final InventoryHolder source;
    private final InventoryHolder dest;

    public InvMovePair(InventoryHolder source, InventoryHolder dest) {
        this.source = source;
        this.dest = dest;
    }

    public InventoryHolder getSource() {
        return source;
    }

    public InventoryHolder getDest() {
        return dest;
    }

    private boolean equalsSource(InvMovePair that) {
        if (this.source.equals(that.source)) {
            return true;
        }
        else if (this.source instanceof BlockState && that.source instanceof BlockState) {
            if (((BlockState) this.source).isPlaced() && ((BlockState) that.source).isPlaced()) {
                return ((BlockState) this.source).getBlock().equals(((BlockState) that.source).getBlock());
            }
        }
        else if (this.source instanceof Entity && that.source instanceof Entity) {
            return ((Entity) this.source).getEntityId() == ((Entity) that.source).getEntityId();
        }
        return false;
    }

    private boolean equalsDest(InvMovePair that) {
        if (this.dest.equals(that.dest)) {
            return true;
        }
        else if (this.dest instanceof BlockState && that.dest instanceof BlockState) {
            if (((BlockState) this.dest).isPlaced() && ((BlockState) that.dest).isPlaced()) {
                return ((BlockState) this.dest).getBlock().equals(((BlockState) that.dest).getBlock());
            }
        }
        else if (this.dest instanceof Entity && that.dest instanceof Entity) {
            return ((Entity) this.dest).getEntityId() == ((Entity) that.dest).getEntityId();
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvMovePair that = (InvMovePair) o;
        return this.equalsSource(that) && this.equalsDest(that);
    }

    @Override
    public int hashCode() {
        Object s, d;
        if (source instanceof BlockState && ((BlockState) source).isPlaced()) {
            s = ((BlockState) source).getBlock();
        }
        else if (source instanceof Entity) {
            s = ((Entity) source).getEntityId();
        }
        else {
            s = source;
        }
        if (dest instanceof BlockState && ((BlockState) dest).isPlaced()) {
            d = ((BlockState) dest).getBlock();
        }
        else if (dest instanceof Entity) {
            d = ((Entity) dest).getEntityId();
        }
        else {
            d = dest;
        }
        return Objects.hash(s, d);
    }
}

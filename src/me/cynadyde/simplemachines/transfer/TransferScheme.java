package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.PluginKey;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class TransferScheme {

    public final ReceivePolicy RECEIVE;
    public final InputPolicy INPUT;
    public final ServePolicy SERVE;
    public final OutputPolicy OUTPUT;
    public final LiquidsPolicy LIQUIDS;

    public static TransferScheme ofInventory(Inventory inv) {
        if (inv == null
                || inv.getHolder() == null
                || !(inv.getHolder() instanceof Container)) {

            return new TransferScheme();
        }
        return ofContainer((Container) inv.getHolder());
    }

    public static TransferScheme ofContainer(Container container) {
        if (container == null) {
            return new TransferScheme();
        }
        PersistentDataContainer pdc = container.getPersistentDataContainer();

        Byte i = pdc.get(PluginKey.INPUT_POLICY.get(), PersistentDataType.BYTE);
        InputPolicy input = (i != null && 0 <= i && i < InputPolicy.values().length)
                ? InputPolicy.values()[i]
                : InputPolicy.NORMAL;

        Byte r = pdc.get(PluginKey.RECEIVE_POLICY.get(), PersistentDataType.BYTE);
        ReceivePolicy retrieve = (r != null && 0 <= r && r < ReceivePolicy.values().length)
                ? ReceivePolicy.values()[r]
                : ReceivePolicy.NORMAL;

        Byte o = pdc.get(PluginKey.OUTPUT_POLICY.get(), PersistentDataType.BYTE);
        OutputPolicy output = (o != null && 0 <= o && o < OutputPolicy.values().length)
                ? OutputPolicy.values()[o]
                : OutputPolicy.NORMAL;

        Byte s = pdc.get(PluginKey.SERVE_POLICY.get(), PersistentDataType.BYTE);
        ServePolicy serve = (s != null && 0 <= s && s < ServePolicy.values().length)
                ? ServePolicy.values()[s]
                : ServePolicy.NORMAL;

        Byte l = pdc.get(PluginKey.LIQUIDS_POLICY.get(), PersistentDataType.BYTE);
        LiquidsPolicy liquids = (l != null && 0 <= l && l < LiquidsPolicy.values().length)
                ? LiquidsPolicy.values()[l]
                : LiquidsPolicy.NORMAL;

        return new TransferScheme(retrieve, input, serve, output, liquids);
    }

    private TransferScheme() {
        this.RECEIVE = ReceivePolicy.NORMAL;
        this.INPUT = InputPolicy.NORMAL;
        this.SERVE = ServePolicy.NORMAL;
        this.OUTPUT = OutputPolicy.NORMAL;
        this.LIQUIDS = LiquidsPolicy.NORMAL;
    }

    public TransferScheme(ReceivePolicy retrieve, InputPolicy input, ServePolicy serve, OutputPolicy output, LiquidsPolicy liquids) {
        this.RECEIVE = retrieve;
        this.INPUT = input;
        this.SERVE = serve;
        this.OUTPUT = output;
        this.LIQUIDS = liquids;
    }

    public boolean isNonNormal() {
        return this.RECEIVE != ReceivePolicy.NORMAL
                || this.INPUT != InputPolicy.NORMAL
                || this.SERVE != ServePolicy.NORMAL
                || this.OUTPUT != OutputPolicy.NORMAL
                || this.LIQUIDS != LiquidsPolicy.NORMAL;
    }

    public void applyTo(Container container) {
        if (container != null) {
            PersistentDataContainer pdc = container.getPersistentDataContainer();

            byte r = (byte) RECEIVE.ordinal();
            if (r > 0) pdc.set(PluginKey.RECEIVE_POLICY.get(), PersistentDataType.BYTE, r);
            else pdc.remove(PluginKey.RECEIVE_POLICY.get());

            byte i = (byte) INPUT.ordinal();
            if (i > 0) pdc.set(PluginKey.INPUT_POLICY.get(), PersistentDataType.BYTE, i);
            else pdc.remove(PluginKey.INPUT_POLICY.get());

            byte s = (byte) SERVE.ordinal();
            if (s > 0) pdc.set(PluginKey.SERVE_POLICY.get(), PersistentDataType.BYTE, s);
            else pdc.remove(PluginKey.SERVE_POLICY.get());

            byte o = (byte) OUTPUT.ordinal();
            if (o > 0) pdc.set(PluginKey.OUTPUT_POLICY.get(), PersistentDataType.BYTE, o);
            else pdc.remove(PluginKey.OUTPUT_POLICY.get());

            byte l = (byte) LIQUIDS.ordinal();
            if (l > 0) pdc.set(PluginKey.LIQUIDS_POLICY.get(), PersistentDataType.BYTE, l);
            else pdc.remove(PluginKey.LIQUIDS_POLICY.get());

            if (container.isPlaced()) {
                container.update(false);
            }
        }
    }
}

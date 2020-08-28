package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.PluginKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class TransferScheme {

    public static final TransferScheme NORMAL = new TransferScheme(SelectionPolicy.NORMAL, SelectionPolicy.NORMAL, InputPolicy.NORMAL, OutputPolicy.NORMAL, LiquidsPolicy.NORMAL);

    public final SelectionPolicy RECEIVE;
    public final SelectionPolicy SERVE;
    public final InputPolicy INPUT;
    public final OutputPolicy OUTPUT;
    public final LiquidsPolicy LIQUIDS;

    public static TransferScheme ofHolder(Object persistentDataHolder) {
        if (persistentDataHolder instanceof PersistentDataHolder) {
            PersistentDataHolder holder = (PersistentDataHolder) persistentDataHolder;
            return new TransferScheme(
                    loadReceivePolicyFrom(holder),
                    loadServePolicyFrom(holder),
                    loadInputPolicyFrom(holder),
                    loadOutputPolicyFrom(holder),
                    loadLiquidsPolicyFrom(holder));
        }
        else {
            return NORMAL;
        }
    }

    public static TransferScheme ofTransaction(Inventory source, Inventory dest) {
        PersistentDataHolder d = dest.getHolder() instanceof PersistentDataHolder ? (PersistentDataHolder) dest.getHolder() : null;
        PersistentDataHolder s = source.getHolder() instanceof PersistentDataHolder ? (PersistentDataHolder) source.getHolder() : null;
        return new TransferScheme(
                d != null ? loadReceivePolicyFrom(d) : SelectionPolicy.NORMAL,
                s != null ? loadServePolicyFrom(s) : SelectionPolicy.NORMAL,
                d != null ? loadInputPolicyFrom(d) : InputPolicy.NORMAL,
                s != null ? loadOutputPolicyFrom(s) : OutputPolicy.NORMAL,
                d != null ? loadLiquidsPolicyFrom(d) : LiquidsPolicy.NORMAL);
    }

    private static <T extends Enum<?> & TransferPolicy> T loadPolicy(PersistentDataHolder holder, PluginKey key, T[] values, T fallback) {
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
        Byte data = pdc.get(key.get(), PersistentDataType.BYTE);
        if (data != null && 0 <= data && data < values.length) {
            return values[data];
        }

        return fallback;
    }

    private static <T extends Enum<?> & TransferPolicy> void savePolicy(PersistentDataHolder holder, PluginKey key, T value, T fallback) {
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
        byte data = (byte) value.ordinal();
        byte zero = (byte) fallback.ordinal();
        if (data != zero) {
            pdc.set(key.get(), PersistentDataType.BYTE, data);
        }
        else {
            pdc.remove(key.get());
        }
    }

    public static SelectionPolicy loadReceivePolicyFrom(PersistentDataHolder holder) {
        return loadPolicy(holder, PluginKey.RECEIVE_POLICY, SelectionPolicy.values(), SelectionPolicy.NORMAL);
    }

    public static SelectionPolicy loadServePolicyFrom(PersistentDataHolder holder) {
        return loadPolicy(holder, PluginKey.SERVE_POLICY, SelectionPolicy.values(), SelectionPolicy.NORMAL);
    }

    public static InputPolicy loadInputPolicyFrom(PersistentDataHolder holder) {
        return loadPolicy(holder, PluginKey.INPUT_POLICY, InputPolicy.values(), InputPolicy.NORMAL);
    }

    public static OutputPolicy loadOutputPolicyFrom(PersistentDataHolder holder) {
        return loadPolicy(holder, PluginKey.OUTPUT_POLICY, OutputPolicy.values(), OutputPolicy.NORMAL);
    }

    public static LiquidsPolicy loadLiquidsPolicyFrom(PersistentDataHolder holder) {
        return loadPolicy(holder, PluginKey.LIQUIDS_POLICY, LiquidsPolicy.values(), LiquidsPolicy.NORMAL);
    }

    public static void saveReceivePolicyTo(PersistentDataHolder holder, SelectionPolicy policy) {
        savePolicy(holder, PluginKey.RECEIVE_POLICY, policy, SelectionPolicy.NORMAL);
    }

    public static void saveServePolicyTo(PersistentDataHolder holder, SelectionPolicy policy) {
        savePolicy(holder, PluginKey.SERVE_POLICY, policy, SelectionPolicy.NORMAL);
    }

    public static void saveInputPolicyTo(PersistentDataHolder holder, InputPolicy policy) {
        savePolicy(holder, PluginKey.INPUT_POLICY, policy, InputPolicy.NORMAL);
    }

    public static void saveOutputPolicyTo(PersistentDataHolder holder, OutputPolicy policy) {
        savePolicy(holder, PluginKey.OUTPUT_POLICY, policy, OutputPolicy.NORMAL);
    }

    public static void saveLiquidsPolicyTo(PersistentDataHolder holder, LiquidsPolicy policy) {
        savePolicy(holder, PluginKey.LIQUIDS_POLICY, policy, LiquidsPolicy.NORMAL);
    }

    public TransferScheme(SelectionPolicy receive, SelectionPolicy serve, InputPolicy input, OutputPolicy output, LiquidsPolicy liquids) {
        this.RECEIVE = receive;
        this.SERVE = serve;
        this.INPUT = input;
        this.OUTPUT = output;
        this.LIQUIDS = liquids;
    }

    public boolean isNonNormal() {
        return this.RECEIVE != SelectionPolicy.NORMAL
                || this.SERVE != SelectionPolicy.NORMAL
                || this.INPUT != InputPolicy.NORMAL
                || this.OUTPUT != OutputPolicy.NORMAL
                || this.LIQUIDS != LiquidsPolicy.NORMAL;
    }

    public TransferPolicy[] getPolicies() {
        return new TransferPolicy[] { RECEIVE, SERVE, INPUT, OUTPUT, LIQUIDS };
    }

    public void applyTo(Object persistentDataHolder) {
        if (persistentDataHolder instanceof PersistentDataHolder) {
            PersistentDataHolder holder = (PersistentDataHolder) persistentDataHolder;

            saveReceivePolicyTo(holder, RECEIVE);
            saveServePolicyTo(holder, SERVE);
            saveInputPolicyTo(holder, INPUT);
            saveOutputPolicyTo(holder, OUTPUT);
            saveLiquidsPolicyTo(holder, LIQUIDS);
        }
    }

    @Override
    public String toString() {
        return "TransferScheme{" +
                "Receive." + RECEIVE +
                ", Serve." + SERVE +
                ", Input." + INPUT +
                ", Output." + OUTPUT +
                ", Liquids." + LIQUIDS +
                '}';
    }
}

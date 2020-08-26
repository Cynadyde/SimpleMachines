package me.cynadyde.simplemachines.transfer;

import me.cynadyde.simplemachines.util.PluginKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class TransferScheme {

    public final SelectionPolicy RECEIVE;
    public final SelectionPolicy SERVE;
    public final InputPolicy INPUT;
    public final OutputPolicy OUTPUT;
    public final LiquidsPolicy LIQUIDS;

    public static TransferScheme ofHolder(InventoryHolder holder) {
        return new TransferScheme(
                loadReceivePolicyFrom(holder),
                loadServePolicyFrom(holder),
                loadInputPolicyFrom(holder),
                loadOutputPolicyFrom(holder),
                loadLiquidsPolicyFrom(holder));
    }

    public static TransferScheme ofTransaction(Inventory source, Inventory dest) {
        InventoryHolder i = dest != null ? dest.getHolder() : null;
        InventoryHolder o = source != null ? source.getHolder() : null;
        return new TransferScheme(
                loadReceivePolicyFrom(i),
                loadServePolicyFrom(o),
                loadInputPolicyFrom(i),
                loadOutputPolicyFrom(o),
                loadLiquidsPolicyFrom(i));
    }

    private static <T extends Enum<?> & TransferPolicy> T loadPolicy(InventoryHolder holder, PluginKey key, T[] values, T fallback) {
        if (holder instanceof PersistentDataHolder) {
            PersistentDataContainer pdc = ((PersistentDataHolder) holder).getPersistentDataContainer();
            Byte data = pdc.get(key.get(), PersistentDataType.BYTE);
            if (data != null && 0 <= data && data < values.length) {
                return values[data];
            }
        }
        return fallback;
    }

    private static <T extends Enum<?> & TransferPolicy> void savePolicy(InventoryHolder holder, PluginKey key, T value, T fallback) {
        if (holder instanceof PersistentDataHolder) {
            PersistentDataContainer pdc = ((PersistentDataHolder) holder).getPersistentDataContainer();
            byte data = (byte) value.ordinal();
            byte zero = (byte) fallback.ordinal();
            if (data != zero) {
                pdc.set(key.get(), PersistentDataType.BYTE, data);
            }
            else {
                pdc.remove(key.get());
            }
        }
    }

    public static SelectionPolicy loadReceivePolicyFrom(InventoryHolder holder) {
        return loadPolicy(holder, PluginKey.RECEIVE_POLICY, SelectionPolicy.values(), SelectionPolicy.NORMAL);
    }

    public static SelectionPolicy loadServePolicyFrom(InventoryHolder holder) {
        return loadPolicy(holder, PluginKey.SERVE_POLICY, SelectionPolicy.values(), SelectionPolicy.NORMAL);
    }

    public static InputPolicy loadInputPolicyFrom(InventoryHolder holder) {
        return loadPolicy(holder, PluginKey.INPUT_POLICY, InputPolicy.values(), InputPolicy.NORMAL);
    }

    public static OutputPolicy loadOutputPolicyFrom(InventoryHolder holder) {
        return loadPolicy(holder, PluginKey.OUTPUT_POLICY, OutputPolicy.values(), OutputPolicy.NORMAL);
    }

    public static LiquidsPolicy loadLiquidsPolicyFrom(InventoryHolder holder) {
        return loadPolicy(holder, PluginKey.LIQUIDS_POLICY, LiquidsPolicy.values(), LiquidsPolicy.NORMAL);
    }

    public static void saveReceivePolicyTo(InventoryHolder holder, SelectionPolicy policy) {
        savePolicy(holder, PluginKey.RECEIVE_POLICY, policy, SelectionPolicy.NORMAL);
    }

    public static void saveServePolicyTo(InventoryHolder holder, SelectionPolicy policy) {
        savePolicy(holder, PluginKey.SERVE_POLICY, policy, SelectionPolicy.NORMAL);
    }

    public static void saveInputPolicyTo(InventoryHolder holder, InputPolicy policy) {
        savePolicy(holder, PluginKey.INPUT_POLICY, policy, InputPolicy.NORMAL);
    }

    public static void saveOutputPolicyTo(InventoryHolder holder, OutputPolicy policy) {
        savePolicy(holder, PluginKey.OUTPUT_POLICY, policy, OutputPolicy.NORMAL);
    }

    public static void saveLiquidsPolicyTo(InventoryHolder holder, LiquidsPolicy policy) {
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

    public void applyTo(InventoryHolder holder) {
        saveReceivePolicyTo(holder, RECEIVE);
        saveServePolicyTo(holder, SERVE);
        saveInputPolicyTo(holder, INPUT);
        saveOutputPolicyTo(holder, OUTPUT);
        saveLiquidsPolicyTo(holder, LIQUIDS);
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

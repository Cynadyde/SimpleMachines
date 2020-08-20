package me.cynadyde.simplemachines.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public enum PluginKey {

    INPUT_POLICY,
    OUTPUT_POLICY,
    SERVE_POLICY,
    RECEIVE_POLICY,
    LIQUIDS_POLICY;

    private NamespacedKey obj;

    public NamespacedKey get() {
        if (obj == null) {
            throw new IllegalStateException(
                    "the plugin's keys have not been initialized!");
        }
        return obj;
    }

    public static void refresh(Plugin plugin) {
        for (PluginKey key : PluginKey.values()) {
            key.obj = new NamespacedKey(plugin, key.name().toLowerCase());
        }
    }
}

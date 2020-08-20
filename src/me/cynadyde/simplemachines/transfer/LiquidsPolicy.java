package me.cynadyde.simplemachines.transfer;

import org.bukkit.Material;

public enum LiquidsPolicy implements TransferPolicy {

    NORMAL {
        @Override
        public Material getToken() {
            return Material.AIR;
        }
    },

    FLOW {
        @Override
        public Material getToken() {
            return Material.IRON_TRAPDOOR;
        }
    }
}

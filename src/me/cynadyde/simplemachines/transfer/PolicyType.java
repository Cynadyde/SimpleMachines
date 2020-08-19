package me.cynadyde.simplemachines.transfer;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum PolicyType {
    INPUT(Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR),
    OUTPUT(Material.DARK_OAK_TRAPDOOR, Material.OAK_TRAPDOOR),
    SERVE(Material.JUNGLE_TRAPDOOR, Material.CRIMSON_TRAPDOOR),
    RETRIEVE(Material.ACACIA_TRAPDOOR, Material.WARPED_TRAPDOOR);

    private final List<Material> trapdoors;

    PolicyType(Material... trapdoors) {
        this.trapdoors = Collections.unmodifiableList(Arrays.asList(trapdoors));
    }

    public List<Material> getTrapdoors() {
        return trapdoors;
    }

    public boolean hasTrapdoor(Material trapdoor) {
        for (Material material : this.trapdoors) {
            if (trapdoor == material) {
                return true;
            }
        }
        return false;
    }
}

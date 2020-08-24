package me.cynadyde.simplemachines.util;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class BlockBreakJob extends BukkitRunnable {

    private final int id;
    private final Block block;
    private final int duration;
    private boolean ticked;
    private int ticks;
    private int stage;
    private Set<Player> receivers;

    public BlockBreakJob(Block block, int duration, Runnable finish) {
        this.id = Utils.RNG.nextInt();
        this.block = block;
        this.duration = duration;
        this.receivers = new HashSet<>();
    }

    @Override
    public void run() {
        if (!ticked) {
            cancel();
        }
        else {
            int prevStage = stage;
            stage = (int) Math.floor(10 * (ticks / (double) duration));
            if (stage != prevStage) {
                if (0 <= stage && stage < 10) {
                    Vector origin = block.getLocation().add(0.5, 0.5, 0.5).toVector();
                    for (Player player : block.getWorld().getPlayers()) {
                        if (player.getLocation().toVector().isInSphere(origin, 16)) {
                            ReflectiveUtils.updateBlockBreakAnimation(id, block, stage, player);
                            receivers.add(player);
                        }
                    }
                }
                else {
                    for (Player player : receivers) {
                        ReflectiveUtils.updateBlockBreakAnimation(id, block, stage, player);
                    }
                    receivers.clear();
                    cancel();
                }
            }
            ticked = false;
        }
    }

    public void tick() {
        ticked = true;
        ticks++;
    }
}

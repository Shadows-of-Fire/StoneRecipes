package shadows.stonerecipes.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import shadows.stonerecipes.StoneRecipes;

public class FlameParticleTask extends BukkitRunnable {

	private Player player;
	private BukkitTask task;

	public FlameParticleTask(Player player) {
		this.player = player;
	}

	@Override
	public void run() {
		if (this.player.isOnline() && this.player.getPotionEffect(PotionEffectType.LEVITATION) != null) {
			Location center = this.player.getLocation();
			this.player.getWorld().spawnParticle(Particle.FLAME, center.getX(), center.getY(), center.getZ(), 4, 0.07, 0, 0.07, 0, null);
		} else this.task.cancel();
	}

	public void start() {
		this.task = this.runTaskTimer(StoneRecipes.INSTANCE, 0, 3);
	}

}

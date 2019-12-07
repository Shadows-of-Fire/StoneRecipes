package shadows.stonerecipes.util;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import shadows.stonerecipes.StoneRecipes;

public class BukkitLambda extends BukkitRunnable {

	Runnable r;

	public BukkitLambda(Runnable r) {
		this.r = r;
	}

	@Override
	public void run() {
		r.run();
	}

	public static BukkitTask runLater(Runnable r, int ticks) {
		return new BukkitLambda(r).runTaskLater(StoneRecipes.INSTANCE, ticks);
	}

	public static BukkitTask runTimer(Runnable r, int ticks) {
		return new BukkitLambda(r).runTaskTimer(StoneRecipes.INSTANCE, 0, ticks);
	}

	public static BukkitTask runTimerAsync(Runnable r, int ticks) {
		return new BukkitLambda(r).runTaskTimerAsynchronously(StoneRecipes.INSTANCE, 0, ticks);
	}

}

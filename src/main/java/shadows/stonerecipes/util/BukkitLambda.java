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

	public static BukkitTask runLater(Runnable r, int timer) {
		return new BukkitLambda(r).runTaskLater(StoneRecipes.INSTANCE, timer);
	}

	public static BukkitTask runAsync(Runnable r) {
		return new BukkitLambda(r).runTaskAsynchronously(StoneRecipes.INSTANCE);
	}

	public static BukkitTask runTimer(Runnable r, int timer) {
		return new BukkitLambda(r).runTaskTimer(StoneRecipes.INSTANCE, 0, timer);
	}

	public static BukkitTask runTimerAsync(Runnable r, int timer) {
		return new BukkitLambda(r).runTaskTimerAsynchronously(StoneRecipes.INSTANCE, 0, timer);
	}

}

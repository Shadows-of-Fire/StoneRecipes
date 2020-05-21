package shadows.stonerecipes.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import shadows.stonerecipes.StoneRecipes;

public class RocketTask implements Runnable {

	int ticksExisted = 0;
	BukkitTask task;
	LivingEntity rocket;
	Player player;
	String worldName = StoneRecipes.INSTANCE.getConfig().getString("rocket.moon_name");

	public BukkitTask start(LivingEntity rocket, Player player) {
		this.rocket = rocket;
		this.player = player;
		return this.task = BukkitLambda.runTimer(this, 1);
	}

	@Override
	public void run() {
		if (this.ticksExisted % 20 == 0) {
			StoneRecipes.INSTANCE.getServer().dispatchCommand(StoneRecipes.INSTANCE.getServer().getConsoleSender(), "title " + player.getName() + " title {\"text\": \"Liftoff in: " + (5 - (this.ticksExisted / 20)) + "\"}");
		}
		if (this.ticksExisted++ >= 120) {
			StoneRecipes.INSTANCE.getServer().dispatchCommand(StoneRecipes.INSTANCE.getServer().getConsoleSender(), "title " + player.getName() + " title {\"text\": \"Liftoff!\"}");
			this.rocket.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 9999, 5, false, false));
			task.cancel();
			new RocketTask2().start(rocket, player);
			MachineUtils.playDistancedSound(rocket.getLocation().clone(), "rocket_launch", SoundCategory.MASTER, 80, 1);
		}
		this.rocket.getWorld().spawnParticle(Particle.SMOKE_LARGE, rocket.getLocation(), 3, 0.5, -2, 0.5, 0, null);
	}

	private static class RocketTask2 extends RocketTask {

		Block block;

		@Override
		public void run() {
			this.ticksExisted++;
			this.rocket.getWorld().spawnParticle(Particle.SMOKE_LARGE, rocket.getLocation().clone().add(0.5, -2, 0.5), 3, 0.5, 0, 0.5, 0, null);
			this.rocket.removePotionEffect(PotionEffectType.LEVITATION);
			this.rocket.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 9999, 5 + this.ticksExisted / 3, false, false));
			if (block == null && this.player.getLocation().getY() < 254) {
				block = this.player.getWorld().getBlockAt(this.player.getLocation().clone().add(0, 1, 0));
				if (block.getType() == Material.BARRIER) {
					block.setType(Material.AIR);
				} else if (block.getType() != Material.AIR) {
					this.rocket.remove();
					this.task.cancel();
					this.player.getWorld().createExplosion(this.player.getLocation(), 10);
				} else block = null;
			}
			if (this.rocket.getLocation().getY() > 600) {
				if (this.player.getLocation().getY() > 600) {
					this.player.teleport(new Location(StoneRecipes.INSTANCE.getServer().getWorld(worldName), player.getLocation().getX(), 200, player.getLocation().getZ()), TeleportCause.PLUGIN);
					this.player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 1000, 0));
				}
				this.rocket.remove();
				this.task.cancel();
				if (this.block != null) this.block.setType(Material.BARRIER);
			}
		}
	}
}

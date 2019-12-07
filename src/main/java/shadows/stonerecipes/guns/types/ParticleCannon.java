package shadows.stonerecipes.guns.types;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_14_R1.EntityLiving;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.guns.BasicGun;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.MachineUtils;

public class ParticleCannon extends BasicGun {

	Map<UUID, BeamTask> activeBeams = new HashMap<>();

	public ParticleCannon(StoneRecipes plugin) {
		super(plugin, "particle_cannon");
		this.range = 10;
		this.spreadDefault = 0;
		this.pellets = 1;
		this.par = Particle.CRIT_MAGIC;
	}

	@Override
	public void recoil(Player player) {
	}

	@Override
	public void shoot(ItemStack gun, Player player, StoneRecipes plugin) {
		if (!activeBeams.containsKey(player.getUniqueId())) {
			if (!usePower(gun)) return;
			if (!fireSound.isEmpty()) MachineUtils.playDistancedSound(player.getLocation(), fireSound, 90, 1);
			spread = spreadDefault;
			shootRay(player);
			recoil(player);
			activeBeams.put(player.getUniqueId(), new BeamTask(player, gun).start());
		} else if (activeBeams.get(player.getUniqueId()).fired) {
			activeBeams.remove(player.getUniqueId()).cancel();
			this.cooldown(player, gun);
		}
	}

	public void shootRay(Player player) {
		for (int i = 0; i < pellets; i++) {
			Vector dir = player.getLocation().getDirection();
			double palletSpread = RANDOM.nextInt(spread + 1) * 2 - spread;
			double zSpread = RANDOM.nextInt(spread + 1) * 2 - spread;
			double verticleSpread = RANDOM.nextInt(spread + 1) * 2 - spread;

			double sin = Math.sin(Math.toRadians(palletSpread));
			double sinZ = Math.sin(Math.toRadians(zSpread));
			double sinV = Math.sin(Math.toRadians(verticleSpread));

			dir.setX(dir.getX() + sin);
			dir.setZ(dir.getZ() + sinZ);
			dir.setY(dir.getY() + sinV);

			dir = dir.normalize();
			for (float j = 1; j < range; j += 0.5) {
				player.getWorld().spawnParticle(par, player.getEyeLocation().clone().add(dir.clone().multiply(j)), 0);
				RayTraceResult res = player.getWorld().rayTrace(player.getEyeLocation().clone().add(dir.clone().multiply(j)), dir, 0.5, FluidCollisionMode.NEVER, true, 1, null);
				if (res != null) {
					if (res.getHitBlock() != null) {
						break;
					}
					if (res.getHitEntity() != null && !res.getHitEntity().equals(player) && res.getHitEntity() instanceof LivingEntity) {
						this.dealDamage(player, (LivingEntity) res.getHitEntity(), damage);
					}
				}
			}
		}
	}

	private class BeamTask implements Runnable {
		Player attacker;
		ItemStack gun;
		BukkitTask task;
		boolean fired = false;

		private BeamTask(Player attacker, ItemStack gun) {
			this.attacker = attacker;
			this.gun = gun;
		}

		private BeamTask start() {
			this.task = BukkitLambda.runTimer(this, 2);
			return this;
		}

		private void cancel() {
			this.task.cancel();
		}

		@Override
		public void run() {
			if (!usePower(gun)) {
				task.cancel();
				activeBeams.remove(attacker.getUniqueId());
				cooldown(attacker, gun);
			}
			if (!fireSound.isEmpty()) MachineUtils.playDistancedSound(attacker.getLocation(), fireSound, 90, 0);
			spread = spreadDefault;
			shootRay(attacker);
			fired = true;
		}
	}

	@Override
	protected void dealDamageInternal(EntityPlayer player, EntityLiving hit, float amount) {
		hit.noDamageTicks = 0;
		hit.lastDamage = 0;
		super.dealDamageInternal(player, hit, amount);
	}

}

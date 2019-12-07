package shadows.stonerecipes.guns.types;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_14_R1.DamageSource;
import net.minecraft.server.v1_14_R1.EntityLiving;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.guns.BasicGun;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockRemovedEvent;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.MachineUtils;

public class Vaporizer extends BasicGun {

	public Vaporizer(StoneRecipes plugin) {
		super(plugin, "quantum_vaporizer");
		this.range = 15;
		this.spreadDefault = 0;
		this.pellets = 1;
		this.recoil = 0.5F;
	}

	@Override
	public void shoot(ItemStack gun, Player player, StoneRecipes plugin) {
		if (!usePower(gun)) return;
		if (!fireSound.isEmpty()) MachineUtils.playDistancedSound(player.getLocation(), fireSound, 90, 1);
		this.spread = spreadDefault;
		recoil(player);
		shootRay(player);
		cooldown(player, gun);
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
				player.getWorld().spawnParticle(Particle.REDSTONE, player.getEyeLocation().clone().add(dir.clone().multiply(j)), 0, new Particle.DustOptions(Color.RED, 1));
				RayTraceResult res = player.getWorld().rayTrace(player.getEyeLocation().clone().add(dir.clone().multiply(j)), dir, 0.5, FluidCollisionMode.NEVER, true, 1, null);
				if (res != null) {
					if (res.getHitBlock() != null) {
						BlockBreakEvent event = new BlockBreakEvent(res.getHitBlock(), player);
						plugin.getServer().getPluginManager().callEvent(event);
						if (!event.isCancelled() && res.getHitBlock().getType().getHardness() >= 0) {
							res.getHitBlock().getWorld().spawnParticle(Particle.SMOKE_LARGE, res.getHitBlock().getLocation().add(0.5, .5, .5), 0);
							if (res.getHitBlock().getType() == Material.NOTE_BLOCK) {
								Bukkit.getServer().getPluginManager().callEvent(new NoteBlockRemovedEvent(res.getHitBlock().getState(), null));
							}
							res.getHitBlock().setType(Material.AIR);
							break;
						}
					} else if (res.getHitEntity() != null && !res.getHitEntity().equals(player) && res.getHitEntity() instanceof LivingEntity) {
						this.dealDamage(player, (LivingEntity) res.getHitEntity(), this.damage);
						break;
					}

				}
			}
		}
	}

	@Override
	protected void dealDamageInternal(EntityPlayer player, EntityLiving hit, float amount) {
		hit.damageEntity(DamageSource.playerAttack(player), amount);
		new DamageOverTime(hit, DamageSource.BURN).start();
	}

	private class DamageOverTime implements Runnable {
		EntityLiving hit;
		BukkitTask task;
		DamageSource src;
		int timesRun = 0;

		private DamageOverTime(EntityLiving hit, DamageSource src) {
			this.hit = hit;
			this.src = src;
		}

		private void start() {
			this.task = BukkitLambda.runTimer(this, 5);
		}

		@Override
		public void run() {
			World w = hit.getBukkitEntity().getWorld();
			Location loc = hit.getBukkitEntity().getLocation();
			w.spawnParticle(Particle.DRIP_LAVA, loc, 0);
			w.spawnParticle(Particle.DRIP_LAVA, loc.clone().add(-0.05f, 0.05f, -0.05f), 0);
			w.spawnParticle(Particle.DRIP_LAVA, loc.clone().add(0.05f, 0.05f, 0.05f), 0);
			w.spawnParticle(Particle.DRIP_LAVA, loc.clone().add(0.05f, 0.05f, -0.05f), 0);
			w.spawnParticle(Particle.DRIP_LAVA, loc.clone().add(0.05f, -0.05f, 0.05f), 0);
			w.spawnParticle(Particle.DRIP_LAVA, loc.clone().add(-0.05f, 0.05f, 0.05f), 0);
			w.spawnParticle(Particle.DRIP_LAVA, loc.clone().add(0.05f, -0.05f, -0.05f), 0);
			hit.damageEntity(src, Vaporizer.this.damage / 8);
			if (!hit.isAlive() || timesRun++ >= 8) task.cancel();
		}
	}

}

package shadows.stonerecipes.guns.types;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import net.minecraft.server.v1_15_R1.DamageSource;
import net.minecraft.server.v1_15_R1.EntityDamageSource;
import net.minecraft.server.v1_15_R1.EntityHuman;
import net.minecraft.server.v1_15_R1.EntityLiving;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.guns.BasicGun;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;

public class Chainsword extends BasicGun {

	public static boolean dotAttacking = false;

	float dotDmg;

	public Chainsword(StoneRecipes plugin) {
		super(plugin, "chainsword");
		this.melee = true;
	}

	@Override
	public void loadConfig(PluginFile guns) {
		super.loadConfig(guns);
		dotDmg = (float) guns.getDouble(name + ".dotdmg", damage / 8F);
	}

	@Override
	public void punch(ItemStack gun, Player player, StoneRecipes plugin, LivingEntity target) {
		if (!usePower(gun)) return;
		if (!fireSound.isEmpty()) MachineUtils.playDistancedSound(player.getLocation(), fireSound, 30, 1);
		this.cooldown(player, gun);
		dealDamage(player, target, damage);
	}

	@Override
	protected void dealDamageInternal(EntityPlayer player, EntityLiving hit, float amount) {
		hit.damageEntity(new Source(player), amount);
		new DamageOverTime(hit, new Source(player)).start();
	}

	private static class Source extends EntityDamageSource {

		public Source(EntityHuman player) {
			super("player", player);
			this.setIgnoreArmor();
		}

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
			hit.noDamageTicks = 0;
			hit.lastDamage = 0;
			dotAttacking = true;
			hit.damageEntity(src, Chainsword.this.dotDmg);
			dotAttacking = false;
			if (!hit.isAlive() || timesRun++ >= 8) task.cancel();
		}
	}

}

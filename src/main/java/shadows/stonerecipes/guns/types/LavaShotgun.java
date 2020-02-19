package shadows.stonerecipes.guns.types;

import java.util.function.Predicate;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_15_R1.DamageSource;
import net.minecraft.server.v1_15_R1.EntityLiving;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.guns.BasicGun;
import shadows.stonerecipes.util.MachineUtils;

public class LavaShotgun extends BasicGun {

	static Predicate<Entity> selector = e -> e instanceof LivingEntity && !e.isDead();

	public LavaShotgun(StoneRecipes plugin) {
		super(plugin, "lava_shotgun");
		this.range = 10;
		this.spreadDefault = 25;
		this.pellets = 10;
		this.recoil = 5;
		this.par = Particle.LAVA;
	}

	@Override
	public void shoot(ItemStack gun, Player player, StoneRecipes plugin) {
		if (!usePower(gun)) return;
		if (!fireSound.isEmpty()) MachineUtils.playDistancedSound(player.getLocation(), fireSound, 90, 1);
		this.spread = spreadDefault;
		if (!player.isSneaking()) spread *= 1.2;
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
				player.getWorld().spawnParticle(par, player.getEyeLocation().clone().add(dir.clone().multiply(j)), 0);
				RayTraceResult res = player.getWorld().rayTrace(player.getEyeLocation().clone().add(dir.clone().multiply(j)), dir, 0.5, FluidCollisionMode.NEVER, true, 1, selector);
				if (res != null) {
					if (res.getHitBlock() != null) {
						break;
					}
					if (res.getHitEntity() != null && !res.getHitEntity().equals(player)) {
						this.dealDamage(player, (LivingEntity) res.getHitEntity(), damage);
						break;
					}
				}
			}
		}
	}

	@Override
	protected void dealDamageInternal(EntityPlayer player, EntityLiving hit, float amount) {
		hit.noDamageTicks = 0;
		hit.lastDamage = 0;
		hit.damageEntity(DamageSource.LAVA, amount);
		hit.setOnFire(8);
	}

}

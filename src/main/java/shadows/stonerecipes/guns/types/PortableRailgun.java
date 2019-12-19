package shadows.stonerecipes.guns.types;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.guns.BasicGun;
import shadows.stonerecipes.util.MachineUtils;

public class PortableRailgun extends BasicGun {

	static Color color = Color.fromRGB(0x00E6FF);

	public PortableRailgun(StoneRecipes plugin) {
		super(plugin, "portable_railgun");
		this.range = 35;
		this.spreadDefault = 0;
		this.pellets = 1;
		this.recoil = 0.9F;
	}

	@Override
	public void shoot(ItemStack gun, Player player, StoneRecipes plugin) {
		if (!usePower(gun)) return;
		if (!fireSound.isEmpty()) MachineUtils.playDistancedSound(player.getLocation(), fireSound, 90, 1);
		this.spread = spreadDefault;
		if (!player.isSneaking()) spread = 2;
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
				player.getWorld().spawnParticle(Particle.REDSTONE, player.getEyeLocation().clone().add(dir.clone().multiply(j)), 0, new Particle.DustOptions(color, 1));
				RayTraceResult res = player.getWorld().rayTrace(player.getEyeLocation().clone().add(dir.clone().multiply(j)), dir, 0.5, FluidCollisionMode.NEVER, true, 1, null);
				if (res != null) {
					if (res.getHitBlock() != null) {
						break;
					} else if (res.getHitEntity() != null && !res.getHitEntity().equals(player) && res.getHitEntity() instanceof LivingEntity) {
						this.dealDamage(player, (LivingEntity) res.getHitEntity(), this.damage);
					}

				}
			}
		}
	}

}

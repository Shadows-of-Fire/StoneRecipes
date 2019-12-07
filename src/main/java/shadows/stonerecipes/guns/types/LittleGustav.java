package shadows.stonerecipes.guns.types;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.guns.BasicGun;
import shadows.stonerecipes.listener.GunHandler;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;

public class LittleGustav extends BasicGun {

	int cloudRadius;
	int cloudTime = 20;
	float exPower = 8;

	public LittleGustav(StoneRecipes plugin) {
		super(plugin, "little_gustav");
		this.range = 30;
		this.spreadDefault = 4;
		this.recoil = 9;
	}

	@Override
	public void shoot(ItemStack gun, Player player, StoneRecipes plugin) {
		if (!usePower(gun)) return;
		if (!fireSound.isEmpty()) MachineUtils.playDistancedSound(player.getLocation(), fireSound, 90, 1);
		this.spread = spreadDefault;
		if (!player.isSneaking()) spread *= 1.2;
		recoil(player);
		shootProjectile(player);
		this.cooldown(player, gun);
	}

	@Override
	public void loadConfig(PluginFile guns) {
		super.loadConfig(guns);
		cloudRadius = guns.getInt(name + ".radius", 8);
		cloudTime = guns.getInt(name + ".cloudtime", cloudTime);
		exPower = (float) guns.getDouble(name + ".explosion");
	}

	public void shootProjectile(Player player) {
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

		dir = dir.normalize().multiply(range / 10);
		Snowball ball = player.launchProjectile(Snowball.class, dir);
		ball.setMetadata(GunHandler.GUN_OWNER, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
		ball.setMetadata(GunHandler.GUN, new FixedMetadataValue(plugin, "little_gustav"));
	}

	@Override
	public void onProjectileImpact(Player shooter, Entity hit, Block blockHit, Snowball snowball) {
		Location loc = hit == null ? blockHit.getLocation() : hit.getLocation();
		BlockBreakEvent event = new BlockBreakEvent(loc.getBlock(), shooter);
		plugin.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			loc.getWorld().createExplosion(loc, exPower);
		} else {
			loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), exPower, true, false);
		}
		AreaEffectCloud effect = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
		effect.setRadius(cloudRadius);
		effect.setColor(Color.BLUE);
		effect.setReapplicationDelay(40);
		effect.setDuration(260);
		effect.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 1, 0), false);
		effect.setParticle(Particle.REDSTONE, new Particle.DustOptions(Color.AQUA, 1));
		effect.setWaitTime(cloudTime);
	}
}

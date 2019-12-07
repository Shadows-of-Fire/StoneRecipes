package shadows.stonerecipes.guns.types;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.guns.BasicGun;
import shadows.stonerecipes.listener.GunHandler;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;

public class GrenadeLauncher extends BasicGun {

	float exPower = 5;

	public GrenadeLauncher(StoneRecipes plugin) {
		super(plugin, "grenade_launcher");
		this.range = 10;
		this.spreadDefault = 4;
		this.pellets = 1;
		this.recoil = 3;
	}

	@Override
	public void loadConfig(PluginFile guns) {
		super.loadConfig(guns);
		exPower = (float) guns.getDouble(name + ".explosion", exPower);
	}

	@Override
	public void shoot(ItemStack gun, Player player, StoneRecipes plugin) {
		if (!usePower(gun)) return;
		if (!fireSound.isEmpty()) MachineUtils.playDistancedSound(player.getLocation(), fireSound, 90, 1);
		this.spread = spreadDefault;
		if (!player.isSneaking()) spread *= 1.2;
		cooldown(player, gun);
		recoil(player);
		shootProjectile(player);
		BukkitLambda.runLater(() -> {
			spread *= 1.2;
			shootProjectile(player);
		}, 3);
		BukkitLambda.runLater(() -> {
			spread *= 1.2;
			shootProjectile(player);
		}, 6);
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

		dir = dir.normalize();
		Snowball ball = player.launchProjectile(Snowball.class, dir);
		ball.setMetadata(GunHandler.GUN_OWNER, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
		ball.setMetadata(GunHandler.GUN, new FixedMetadataValue(plugin, "grenade_launcher"));
	}

	@Override
	public void onProjectileImpact(Player shooter, Entity hit, Block blockHit, Snowball snowball) {
		Location loc = hit == null ? blockHit.getLocation() : hit.getLocation();
		double x = loc.getX();
		double y = loc.getY();
		double z = loc.getZ();
		loc.getWorld().createExplosion(x, y, z, exPower, true, false);
	}

}

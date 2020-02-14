package shadows.stonerecipes.guns;

import java.util.Random;

import javax.annotation.Nullable;

import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_15_R1.DamageSource;
import net.minecraft.server.v1_15_R1.EntityLiving;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;

public abstract class BasicGun {

	public static final Random RANDOM = new Random();

	protected final StoneRecipes plugin;
	protected final String name;

	protected int cooldown;
	protected int damage;
	protected int cost;
	protected String chargeSound;
	protected String fireSound;
	protected String idleSound;
	protected boolean melee = false;
	protected float recoil = 0F;
	protected int range;
	protected int spreadDefault;
	protected int pellets;
	protected int spread;
	protected Particle par;

	public BasicGun(StoneRecipes plugin, String name) {
		this.plugin = plugin;
		this.name = name;
	}

	public void loadConfig(PluginFile guns) {
		if (!guns.contains(name)) { throw new IllegalArgumentException("No such gun " + name); }
		damage = guns.getInt(name + ".damage", 5);
		cooldown = guns.getInt(name + ".cooldown", 20);
		cost = guns.getInt(name + ".cost", 50);
		chargeSound = guns.getString(name + ".chargeSound", "");
		fireSound = guns.getString(name + ".fireSound", "");
		idleSound = guns.getString(name + ".idleSound", "");
		range = guns.getInt(name + ".range", range);
		recoil = (float) guns.getDouble(name + ".recoil", recoil);
	}

	public void punch(ItemStack gun, Player player, StoneRecipes plugin, LivingEntity target) {
	}

	public void shoot(ItemStack gun, Player player, StoneRecipes plugin) {
	}

	public void recoil(Player player) {
		if (recoil <= 0) return;
		Vector v = player.getLocation().getDirection();
		v.multiply(-1);
		player.setVelocity(v.multiply(recoil));
	}

	public boolean usePower(ItemStack item) {
		int power = Charger.getPower(item);
		if (power >= this.cost) {
			Charger.usePower(item, cost);
			return true;
		}
		return false;
	}

	public final void dealDamage(Player attacker, LivingEntity hit, float amount) {
		dealDamageInternal(((CraftPlayer) attacker).getHandle(), ((CraftLivingEntity) hit).getHandle(), amount);
	}

	protected void dealDamageInternal(EntityPlayer player, EntityLiving hit, float amount) {
		hit.damageEntity(DamageSource.playerAttack(player), amount);
	}

	public String getName() {
		return name;
	}

	public void cooldown(Player holder, ItemStack stack) {
		StoneRecipes.INSTANCE.getGuns().cooldown(holder, name);
		BukkitLambda.runLater(() -> {
			if (!chargeSound.isEmpty()) MachineUtils.playDistancedSound(holder.getLocation(), chargeSound, 15, 1);
			StoneRecipes.INSTANCE.getGuns().endCooldown(holder, name);
		}, cooldown);
	}

	public void onProjectileImpact(Player shooter, @Nullable Entity hit, @Nullable Block blockHit, Snowball snowball) {

	}

	public boolean isMelee() {
		return melee;
	}

	public int getCost() {
		return cost;
	}

}

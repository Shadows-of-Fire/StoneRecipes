package shadows.stonerecipes.guns.types;

import org.bukkit.Color;
import org.bukkit.craftbukkit.v1_16_R2.event.CraftEventFactory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.v1_16_R2.DamageSource;
import net.minecraft.server.v1_16_R2.EntityLightning;
import net.minecraft.server.v1_16_R2.EntityLiving;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.EntityTypes;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.guns.BasicGun;
import shadows.stonerecipes.util.Laser;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;

public class LightningGauntlet extends BasicGun {

	private static Color LIGHT_BLUE = Color.fromRGB(0xADD8E6);

	float effectDmg;

	public LightningGauntlet(StoneRecipes plugin, String name) {
		super(plugin, name);
		this.melee = true;
	}

	@Override
	public void loadConfig(PluginFile guns) {
		super.loadConfig(guns);
		effectDmg = (float) guns.getDouble(name + ".effectdmg", damage * 0.75F);
	}

	@Override
	public void punch(ItemStack gun, Player player, StoneRecipes plugin, LivingEntity target) {
		if (!usePower(gun)) return;
		if (!fireSound.isEmpty()) MachineUtils.playDistancedSound(player.getLocation(), fireSound, 30, 1);
		this.cooldown(player, gun);
		this.dealDamage(player, target, damage);
		player.getWorld().getEntitiesByClass(LivingEntity.class).stream().filter(e -> e.getLocation().distanceSquared(target.getLocation()) < 8 * 8).forEach(e -> {
			if (e != player) {
				new Laser(e.getLocation(), target.getLocation(), LIGHT_BLUE).connect();
				this.dealDamage(player, e, effectDmg);
			}
		});
	}

	@Override
	protected void dealDamageInternal(EntityPlayer player, EntityLiving hit, float amount) {
		CraftEventFactory.blockDamage = null;
		CraftEventFactory.entityDamage = new EntityLightning(EntityTypes.LIGHTNING_BOLT, player.world);
		hit.damageEntity(DamageSource.LIGHTNING, amount);
	}

}

package shadows.stonerecipes.listener;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.util.BukkitLambda;

public class NanosuitHandler implements Listener {

	public static final String HELM = "nano_helmet";
	public static final String CHEST = "nano_chestplate";
	public static final String LEGS = "nano_leggings";
	public static final String BOOTS = "nano_boots";
	public static final int DURATION = 20 * 30; //30 seconds in ticks

	public int nightVisCost = 1;
	public int speedCost = 1;
	public int resCost = 1;
	public int jumpCost = 1;
	public double fallDmgCost = 0.5;
	public double damageCost = 0.5;
	public double fullSetProt = 0.001;

	public NanosuitHandler() {
		FileConfiguration file = StoneRecipes.INSTANCE.getConfig();
		nightVisCost = file.getInt("nanosuit.night_vision_cost", 1);
		speedCost = file.getInt("nanosuit.speed_cost", 1);
		resCost = file.getInt("nanosuit.resistance_cost", 1);
		fallDmgCost = file.getDouble("nanosuit.fall_damage_cost", 0.5);
		damageCost = file.getDouble("nanosuit.damage_cost", 0.5);
		jumpCost = file.getInt("nanosuit.jump_boost_cost", 1);
		fullSetProt = file.getDouble("nanosuit.full_set_prot", 0.001);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		new PlayerTickHandler(e.getPlayer()).start();
	}

	@EventHandler
	public void onPlayerDamage(EntityDamageEvent e) {
		if (!e.getEntityType().equals(EntityType.PLAYER)) return;
		Player player = (Player) e.getEntity();
		if (e.getCause().equals(DamageCause.FALL)) {
			ItemStack boots = player.getInventory().getBoots();
			int power = Charger.getPower(boots);
			if (power <= 0) return;
			float powerNeeded = (float) (e.getDamage() * fallDmgCost);
			if (powerNeeded > 0 && power < powerNeeded) {
				float mult = (powerNeeded - power) / powerNeeded;
				e.setDamage(e.getDamage() * mult);
				Charger.usePower(boots, power);
				return;
			}
			Charger.usePower(boots, Math.round(powerNeeded));
			e.setDamage(0.001F);
			return;
		} else if (hasFullSet(player)) {
			Charger.usePower(player.getInventory().getHelmet(), 1);
			Charger.usePower(player.getInventory().getChestplate(), 1);
			Charger.usePower(player.getInventory().getLeggings(), 1);
			Charger.usePower(player.getInventory().getBoots(), 1);
			e.setDamage(e.getDamage() * fullSetProt);
		}
	}

	/**
	 * Checks if this player has the full nanosuit, and all pieces are not at 0 power.
	 */
	public boolean hasFullSet(Player player) {
		for (ItemStack piece : player.getInventory().getArmorContents()) {
			if (Charger.getPower(piece) <= 0) return false;
		}
		return true;
	}

	private class PlayerTickHandler implements Runnable {
		Player p;
		BukkitTask task;

		private PlayerTickHandler(Player p) {
			this.p = p;
		}

		private void start() {
			this.task = BukkitLambda.runTimer(this, 20);
		}

		@Override
		public void run() {
			if (!p.isOnline()) task.cancel();
			ItemStack helm = p.getInventory().getHelmet();
			PotionEffect nightVis = p.getPotionEffect(PotionEffectType.NIGHT_VISION);
			if ((nightVis == null || nightVis.getDuration() <= 240) && Charger.getPower(helm) > nightVisCost) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, DURATION, 0), true);
				Charger.usePower(helm, nightVisCost);
			}
			ItemStack legs = p.getInventory().getLeggings();
			PotionEffect speed = p.getPotionEffect(PotionEffectType.SPEED);
			if ((speed == null || speed.getDuration() <= 40) && Charger.getPower(legs) > nightVisCost) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, DURATION, 1), true);
				Charger.usePower(legs, speedCost);
			}
			ItemStack chest = p.getInventory().getChestplate();
			PotionEffect res = p.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			if ((res == null || res.getDuration() <= 40) && Charger.getPower(chest) > resCost) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, DURATION, 1), true);
				Charger.usePower(chest, resCost);
			}
			ItemStack boots = p.getInventory().getBoots();
			PotionEffect jump = p.getPotionEffect(PotionEffectType.JUMP);
			if ((jump == null || jump.getDuration() <= 40) && Charger.getPower(boots) > jumpCost) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, DURATION, 1), true);
				Charger.usePower(boots, jumpCost);
			}
		}
	}

}

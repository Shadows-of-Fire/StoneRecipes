package shadows.stonerecipes.listener;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.common.base.Strings;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.item.ItemData;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.Keys;

public final class MoonHandler implements Listener {

	private final Map<UUID, Integer> suffocating = new HashMap<>();
	private WeakReference<World> moonWorld = new WeakReference<>(null);

	public MoonHandler() {
		BukkitLambda.runTimerAsync(() -> {
			if (moonWorld.get() == null) moonWorld = new WeakReference<>(Bukkit.getWorld(StoneRecipes.moonWorldName));
			for (Player p : moonWorld.get().getPlayers()) {

				int current = getOxygen(p.getInventory().getHelmet());

				if (current <= 0) {
					suffocating.putIfAbsent(p.getUniqueId(), 10);
					suffocating.put(p.getUniqueId(), suffocating.get(p.getUniqueId()) - 1);

					if (suffocating.get(p.getUniqueId()) < 0) {
						suffocating.put(p.getUniqueId(), 0);
					}

					if (suffocating.get(p.getUniqueId()) <= 0) {
						BukkitLambda.runLater(() -> {
							p.damage(1);
						}, 0);
					}

					p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getActionBar(suffocating.getOrDefault(p.getUniqueId(), 10), 10)));
					return;
				} else {
					suffocating.remove(p.getUniqueId());
					useOxygen(p.getInventory().getHelmet(), getOxygenCost(p.getInventory().getHelmet()));
					p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getActionBar(10, 10)));
				}

			}
		}, 20);
	}

	@EventHandler
	public void worldChange(PlayerChangedWorldEvent e) {
		if (e.getFrom().getName().equals(StoneRecipes.moonWorldName)) {
			e.getPlayer().removePotionEffect(PotionEffectType.SLOW_FALLING);
			e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
		} else if (e.getPlayer().getWorld().getName().equals(StoneRecipes.moonWorldName)) {
			e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, StoneRecipes.moonFeatherFall));
			e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, StoneRecipes.moonJumpBoost));
		}
	}

	@EventHandler
	public void worldChange(PlayerJoinEvent e) {
		if (!e.getPlayer().getWorld().getName().equals(StoneRecipes.moonWorldName)) {
			e.getPlayer().removePotionEffect(PotionEffectType.SLOW_FALLING);
			e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
		} else {
			e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, StoneRecipes.moonFeatherFall));
			e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, StoneRecipes.moonJumpBoost));
		}
	}

	@EventHandler
	public void respawn(PlayerRespawnEvent e) {
		if (!e.getRespawnLocation().getWorld().getName().equals(StoneRecipes.moonWorldName)) {
			e.getPlayer().removePotionEffect(PotionEffectType.SLOW_FALLING);
			e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
		} else {
			e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, StoneRecipes.moonFeatherFall));
			e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, StoneRecipes.moonJumpBoost));
		}
	}

	@EventHandler
	public void useHelmet(PlayerInteractEvent e) {
		if (getMaxOxygen(e.getItem()) > 0 && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
			e.setCancelled(true);
			e.getPlayer().getInventory().remove(e.getItem());
			e.getPlayer().getInventory().setHelmet(e.getItem());
		}
	}

	public static boolean hasOxygenHelmet(Player p) {
		ItemStack helmet = p.getInventory().getHelmet();
		return getMaxOxygen(helmet) > 0;
	}

	public static int getOxygen(ItemStack stack) {
		if (ItemData.isEmpty(stack) || !stack.hasItemMeta()) return -1;
		return stack.getItemMeta().getPersistentDataContainer().getOrDefault(Keys.OXYGEN, PersistentDataType.INTEGER, -1);
	}

	public static int getOxygenCost(ItemStack stack) {
		if (ItemData.isEmpty(stack) || !stack.hasItemMeta()) return 10;
		return stack.getItemMeta().getPersistentDataContainer().getOrDefault(Keys.OXYGEN_COST, PersistentDataType.INTEGER, 10);
	}

	public static int getMaxOxygen(ItemStack stack) {
		if (ItemData.isEmpty(stack) || !stack.hasItemMeta()) return -1;
		return stack.getItemMeta().getPersistentDataContainer().getOrDefault(Keys.MAX_OXYGEN, PersistentDataType.INTEGER, -1);
	}

	public static void setOxygen(ItemStack stack, int power) {
		if (!stack.hasItemMeta()) return;
		int max = getMaxOxygen(stack);
		if (power > max) power = max;
		ItemMeta meta = stack.getItemMeta();
		meta.getPersistentDataContainer().set(Keys.OXYGEN, PersistentDataType.INTEGER, power);
		List<String> lore = meta.getLore();
		lore.set(0, ChatColor.translateAlternateColorCodes('&', String.format("&r&aOxygen: %d/%d", power, max)));
		meta.setLore(lore);
		stack.setItemMeta(meta);
	}

	public static void useOxygen(ItemStack stack, int power) {
		setOxygen(stack, getOxygen(stack) - power);
	}

	public static String getActionBar(int current, int max) {

		float percent = (float) current / max;
		int progressBars = (int) (10 * percent);

		return Strings.repeat("" + GravityCell.FULL.getDisplay(), progressBars) + Strings.repeat("" + GravityCell.EMPTY.getDisplay(), 10 - progressBars);
	}

	public static enum GravityCell {
		FULL("\u00a7b\u2588"),
		EMPTY("\u00a7c\u2588");

		private String display;

		GravityCell(String display) {
			this.display = display;
		}

		public String getDisplay() {
			return display;
		}
	}
}

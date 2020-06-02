package shadows.stonerecipes.listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.guns.BasicGun;
import shadows.stonerecipes.guns.types.Chainsword;
import shadows.stonerecipes.guns.types.GrenadeLauncher;
import shadows.stonerecipes.guns.types.LavaShotgun;
import shadows.stonerecipes.guns.types.LightningGauntlet;
import shadows.stonerecipes.guns.types.LittleGustav;
import shadows.stonerecipes.guns.types.ParticleCannon;
import shadows.stonerecipes.guns.types.PortableRailgun;
import shadows.stonerecipes.guns.types.Vaporizer;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.PluginFile;

public class GunHandler implements Listener {

	public static final String GUN_OWNER = "GunOwner";
	public static final String GUN = "Gun";
	public static final NamespacedKey SCOPED = new NamespacedKey(StoneRecipes.INSTANCE, "scoped");

	protected final StoneRecipes plugin;
	protected final PluginFile gunFile;

	/**
	 * Maps of item names -> gun objects.
	 */
	protected final Map<String, BasicGun> guns = new HashMap<>();

	protected final Map<String, ItemStack> scoping = new HashMap<>();

	protected final Set<String> cooldowns = new HashSet<>();

	public GunHandler(StoneRecipes plugin) {
		this.plugin = plugin;
		this.gunFile = new PluginFile(plugin, "guns.yml");
	}

	private <T> void loadGun(String name, BiFunction<StoneRecipes, String, BasicGun> ctor) {
		try {
			guns.put(name, ctor.apply(plugin, name));
			guns.get(name).loadConfig(gunFile);
			ItemStack gun = plugin.getItems().getItemHolder(name).getStack();
			ItemMeta meta = gun.getItemMeta();
			List<String> lore = meta.getLore();
			lore.add(1, ChatColor.translateAlternateColorCodes('&', String.format("&r&aPower Cost: %d", guns.get(name).getCost())));
			meta.setLore(lore);
			gun.setItemMeta(meta);
		} catch (Throwable t) {
			plugin.getLogger().severe("Failed to load gun " + name);
			t.printStackTrace();
		}
	}

	public void loadGuns() {
		loadGun("lava_shotgun", LavaShotgun::new);
		loadGun("portable_railgun", PortableRailgun::new);
		loadGun("quantum_vaporizer", Vaporizer::new);
		loadGun("particle_cannon", ParticleCannon::new);
		loadGun("grenade_launcher", GrenadeLauncher::new);
		loadGun("little_gustav", LittleGustav::new);
		loadGun("lightning_gauntlet", LightningGauntlet::new);
		loadGun("chainsword", Chainsword::new);

		loadGun("gold_lava_shotgun", LavaShotgun::new);
		loadGun("gold_portable_railgun", PortableRailgun::new);
		loadGun("gold_quantum_vaporizer", Vaporizer::new);
		loadGun("gold_particle_cannon", ParticleCannon::new);
		loadGun("gold_grenade_launcher", GrenadeLauncher::new);
		loadGun("gold_little_gustav", LittleGustav::new);
		loadGun("gold_lightning_gauntlet", LightningGauntlet::new);
		loadGun("gold_chainsword", Chainsword::new);

		loadGun("rgb_lava_shotgun", LavaShotgun::new);
		loadGun("rgb_portable_railgun", PortableRailgun::new);
		loadGun("rgb_quantum_vaporizer", Vaporizer::new);
		loadGun("rgb_particle_cannon", ParticleCannon::new);
		loadGun("rgb_grenade_launcher", GrenadeLauncher::new);
		loadGun("rgb_little_gustav", LittleGustav::new);
		loadGun("rgb_lightning_gauntlet", LightningGauntlet::new);
		loadGun("rgb_chainsword", Chainsword::new);
	}

	@EventHandler
	public void onPlayerUseGun(PlayerInteractEvent e) {
		BasicGun gun = guns.get(ItemData.getItemId(e.getItem()));
		if (gun == null || e.getHand() != EquipmentSlot.HAND || e.useItemInHand() == Result.DENY) return;
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (!isOnCooldown(e.getPlayer(), gun.getName())) gun.shoot(e.getItem(), e.getPlayer(), plugin);
		}
	}

	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
		if (e.getHand() == EquipmentSlot.HAND && e.getRightClicked() instanceof LivingEntity) {
			ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
			String gunName = ItemData.getItemId(item);
			if (!guns.containsKey(gunName)) { return; }
			if (!isOnCooldown(e.getPlayer(), gunName)) {
				BasicGun gun = guns.get(gunName);
				if (!gun.isMelee()) gun.shoot(item, e.getPlayer(), plugin);
				else gun.punch(item, e.getPlayer(), plugin, (LivingEntity) e.getRightClicked());
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onEntityHurt(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Player && e.getDamage() > 0 && e.getCause() == DamageCause.ENTITY_ATTACK && e.getEntity() instanceof LivingEntity) {
			Player p = (Player) e.getDamager();
			ItemStack item = p.getInventory().getItemInMainHand();
			String gunName = ItemData.getItemId(item);
			if (!guns.containsKey(gunName)) return;
			if (!Chainsword.dotAttacking && !isOnCooldown((Player) e.getDamager(), gunName)) guns.get(gunName).punch(item, p, plugin, (LivingEntity) e.getEntity());
		}
	}

	@EventHandler
	public void onPlayerScope(PlayerToggleSneakEvent e) {
		if (e.getPlayer().getInventory().getItemInMainHand() != null) {
			ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
			String gunName = ItemData.getItemId(item);
			if (guns.containsKey(gunName) && !guns.get(gunName).isMelee()) {
				if (e.isSneaking()) {
					scope(e.getPlayer(), item);
				} else {
					unscope(e.getPlayer());
				}
			}
		}
	}

	@EventHandler
	public void onPlayerSwapHotbarItem(PlayerItemHeldEvent e) {
		unscope(e.getPlayer());
		if (e.getPlayer().getInventory().getItem(e.getPreviousSlot()) != null) {
			ItemStack item = e.getPlayer().getInventory().getItem(e.getPreviousSlot());
			if (ItemData.getItemId(item).equals("little_gustav")) {
				e.getPlayer().removePotionEffect(PotionEffectType.SLOW);
			}
		}
		if (e.getPlayer().getInventory().getItem(e.getNewSlot()) != null) {
			ItemStack item = e.getPlayer().getInventory().getItem(e.getNewSlot());
			if (ItemData.getItemId(item).equals("little_gustav")) {
				e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 99999, 2));
			}
		}
	}

	@EventHandler
	public void onPlayerDropGustav(PlayerDropItemEvent e) {
		ItemStack item = e.getItemDrop().getItemStack();
		if (ItemData.getItemId(item).equals("little_gustav")) {
			e.getPlayer().removePotionEffect(PotionEffectType.SLOW);
		}
		unscope(e.getPlayer());
	}

	@EventHandler
	public void inventoryClick(InventoryClickEvent e) {
		if (e.getSlot() != ((Player) e.getWhoClicked()).getInventory().getHeldItemSlot()) { return; }
		if (e.getCurrentItem() != null) {
			ItemStack item = e.getCurrentItem();
			if (ItemData.getItemId(item).equals("little_gustav")) {
				e.getWhoClicked().removePotionEffect(PotionEffectType.SLOW);
			}
		}
		if (e.getCursor() != null) {
			ItemStack item = e.getCursor();
			if (ItemData.getItemId(item).equals("little_gustav")) {
				e.getWhoClicked().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 99999, 2));
			}
		}
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e) {
		unscope(e.getPlayer());
	}

	@EventHandler
	public void playerSwapItem(PlayerSwapHandItemsEvent e) {
		unscope(e.getPlayer());
	}

	@SuppressWarnings("deprecation")
	public void unscope(Player player) {
		if (scoping.containsKey(player.getName())) {
			ItemStack stack = scoping.remove(player.getName());
			stack.setDurability((short) (stack.getDurability() - 1));
			ItemMeta meta = stack.getItemMeta();
			meta.getPersistentDataContainer().set(SCOPED, PersistentDataType.BYTE, (byte) 0);
			stack.setItemMeta(meta);
		}
	}

	@SuppressWarnings("deprecation")
	public void scope(Player player, ItemStack item) {
		if (!scoping.containsKey(player.getName())) {
			ItemMeta meta = item.getItemMeta();
			if (meta.getPersistentDataContainer().getOrDefault(SCOPED, PersistentDataType.BYTE, (byte) 0) == 0) item.setDurability((short) (item.getDurability() + 1));
			scoping.put(player.getName(), item);
			meta = item.getItemMeta();
			meta.getPersistentDataContainer().set(SCOPED, PersistentDataType.BYTE, (byte) 1);
			item.setItemMeta(meta);
		}
	}

	@EventHandler
	public void onGunProjectileHit(ProjectileHitEvent e) {
		if (e.getEntity() instanceof Snowball) {
			Snowball ball = (Snowball) e.getEntity();
			if (ball.hasMetadata(GUN_OWNER) && ball.hasMetadata(GUN)) {
				String gun = ball.getMetadata(GUN).get(0).asString();
				Player player = Bukkit.getServer().getPlayer(UUID.fromString(ball.getMetadata(GUN_OWNER).get(0).asString()));
				guns.get(gun).onProjectileImpact(player, e.getHitEntity(), e.getHitBlock(), ball);
			}
		}
	}

	public boolean isOnCooldown(Player player, String gun) {
		return cooldowns.contains(player.getName() + '|' + gun);
	}

	public void cooldown(Player player, String gun) {
		cooldowns.add(player.getName() + '|' + gun);
	}

	public void endCooldown(Player player, String gun) {
		cooldowns.remove(player.getName() + '|' + gun);
	}
}

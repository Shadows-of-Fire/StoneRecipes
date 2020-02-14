package shadows.stonerecipes.tileentity.machine;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class PlayerTeleporter extends PoweredMachine {

	protected int cooldown;
	protected boolean isCoolingDown = false;
	protected WorldPos link = WorldPos.INVALID;
	protected BukkitTask effect;

	public PlayerTeleporter(WorldPos pos) {
		super("player_teleporter", "Player Teleporter", "config.yml", pos);
		this.updater = false;
	}

	@Override
	public void loadConfigData(PluginFile file) {
		this.timer = 5;
		this.cooldown = StoneRecipes.INSTANCE.getConfig().getInt("playerTP.cooldown");
		this.powerCost = StoneRecipes.INSTANCE.getConfig().getInt("playerTP.powerCost");
		this.start_progress = StoneRecipes.INSTANCE.getConfig().getInt("playerTP.start_progress");
		this.maxPower = StoneRecipes.INSTANCE.getConfig().getInt("playerTP.maxPower");
	}

	@Override
	public void read(PluginFile file) {
		super.read(file);
		if (file.isString(pos + ".link")) this.link = new WorldPos(file.getString(pos + ".link"));
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".link", link.toString());
	}

	@Override
	public void setupContainer() {
		super.setupContainer();
		onPowerChange();
	}

	@SuppressWarnings("deprecation")
	private void onPowerChange() {
		ItemMeta barMeta = this.powerBar.getItemMeta();
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.YELLOW + "" + this.getPower() + "/" + this.maxPower);
		barMeta.setLore(lore);
		this.powerBar.setItemMeta(barMeta);
		for (int i = 1; i < 8; i++) {
			inventory.setItemInternal(i, powerBar);
		}
		guiTex.setDurability((short) (start_progress + Math.min(9, getPower() / (maxPower / 10))));
		inventory.setItemInternal(8, guiTex);
	}

	public void teleport(Player player) {
		if (!link.equals(WorldPos.INVALID) && getPower() >= powerCost && !isCoolingDown) {
			if (player.getWorld().getBlockAt(link.toLocation()).getType() != Material.NOTE_BLOCK) {
				player.sendMessage("The destination teleporter appears to have been removed.");
				link = WorldPos.INVALID;
				return;
			}
			isCoolingDown = true;
			Vector direction = player.getLocation().getDirection().clone();
			Location locationTP = link.toLocation().add(0.5, 1, 0.5);
			locationTP.setDirection(direction);
			player.teleport(locationTP);
			player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
			this.usePower(powerCost);
			BukkitLambda.runLater(() -> {
				isCoolingDown = false;
			}, cooldown);
		}
	}

	@Override
	public void start() {
		super.start();
		effect = BukkitLambda.runTimerAsync(() -> {
			if (getPower() < powerCost || isCoolingDown || link.equals(WorldPos.INVALID)) return;
			for (float t = 0; t < 2 * Math.PI; t += 0.15) {
				double x = Math.sin(t) / 2 + 0.5;
				double y = Math.cos(t) + 2;
				location.getWorld().spawnParticle(Particle.REDSTONE, location.clone().add(x, y, 0.5), 1, new Particle.DustOptions(Color.BLUE, 1));
			}
		}, 8);
	}

	@Override
	public void unload() {
		super.unload();
		if (effect != null && !effect.isCancelled()) {
			effect.cancel();
		}
	}

	public void setLink(WorldPos link) {
		this.link = link;
	}

	public WorldPos getLink() {
		return link;
	}

	@Override
	protected void dropItems() {
	}

}

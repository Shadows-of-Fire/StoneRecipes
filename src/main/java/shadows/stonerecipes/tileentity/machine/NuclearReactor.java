package shadows.stonerecipes.tileentity.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.ReactorHandler;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class NuclearReactor extends PowerGenerator {

	protected NoteBlockInventory powerInv;
	protected int maxHeat = 10;
	protected int uraniumHeat = 1;
	protected int uraniumPower = 1;
	protected int coolantHeat = 1;
	protected int packedIceHeat = 1;
	protected int iceHeat = 1;
	protected int chambers = -1;
	protected boolean exploded = false;
	protected final ItemStack lock = new ItemStack(Material.DIAMOND_HOE);

	protected static final BlockFace[] FACES = ReactorHandler.CHAMBER_FACES;

	@SuppressWarnings("deprecation")
	public NuclearReactor(WorldPos pos) {
		super("nuclear_reactor", "Nuclear Reactor", "config.yml", pos);
		powerInv = new NoteBlockInventory(null, 9, "Reactor Energy Gauge");
		ItemMeta meta = lock.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Section Locked - Add Chambers");
		meta.setUnbreakable(true);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
		lock.setItemMeta(meta);
		lock.setDurability((short) 371);
		this.updateChambers();
		this.onPowerChanged();
	}

	public void updateChambers() {
		int oldChambers = chambers;
		chambers = 0;
		for (BlockFace face : FACES)
			if (StoneRecipes.INSTANCE.getReactors().isChamber(location.getBlock().getRelative(face))) chambers++;
		if (chambers == oldChambers) return;
		int slots = 9 * Math.min(chambers + 1, 6);
		ItemStack[] contents = inventory.getContents();
		for (int i = 0; i < contents.length; i++) {
			if (i >= slots) {
				if (contents[i] != null && !ItemData.isSimilar(lock, contents[i])) {
					location.getWorld().dropItem(location, contents[i]);
				}
				inventory.setItemInternal(i, null);
			}
		}
		for (int i = 4; i < 54; i += 9) {
			if (i > slots) inventory.setItemInternal(i, lock);
			else inventory.setItemInternal(i, null);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onPowerChanged() {
		ItemMeta barMeta = this.powerBar.getItemMeta();
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.YELLOW + "" + this.getPower() + "/" + this.maxPower);
		barMeta.setLore(lore);
		this.powerBar.setItemMeta(barMeta);
		this.guiTex.setItemMeta(barMeta);
		guiTex.setDurability((short) (start_progress + Math.min(9, getPower() / (maxPower / 10))));
		for (int i = 0; i < 9; i++) {
			powerInv.setItemInternal(i, powerBar);
		}
		powerInv.setItemInternal(8, guiTex);
	}

	@Override
	public boolean isClickableSlot(int slot) {
		return slot < 9 * (chambers + 1);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void read(PluginFile file) {
		super.read(file);
		if (file.isList(pos.toString() + ".inv")) {
			List<ItemStack> content = (List<ItemStack>) file.getList(pos + ".inv");
			for (int i = 0; i < content.size(); i++) {
				if (content.get(i) != null) {
					if (i > inventory.getSize() && !ItemData.isSimilar(lock, content.get(i))) {
						location.getWorld().dropItem(location, content.get(i));
					} else {
						inventory.setItemInternal(i, content.get(i));
					}
				}
			}
		} else StoneRecipes.INSTANCE.getLogger().info("Failed to read inventory for a " + name + " at " + pos.translated());
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".inv", Arrays.asList(inventory.getContents()));
		file.set(pos + ".chambers", chambers);
	}

	public void explode() {
		exploded = true;
		if (inventory.getViewers() != null && !inventory.getViewers().isEmpty()) {
			for (HumanEntity viewer : inventory.getViewers()) {
				viewer.closeInventory();
			}
		}
		location.getBlock().setType(Material.AIR, false);
		location.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), 50, true, true);
		StoneRecipes.INSTANCE.getReactors().removeReactor(pos);
	}

	public int getHeat(int slot) {
		int heat = uraniumHeat;
		slots = new IntArrayList();
		int reduction = 0;
		slots.add(slot);
		if (slot > 8 && !slots.contains(slot - 9)) {
			reduction = cool(reduction, slot - 9);
		}
		if (slot % 8 != 0 && !slots.contains(slot + 1)) {
			reduction = cool(reduction, slot + 1);
		}
		if (slot % 9 != 0 && !slots.contains(slot - 1)) {
			reduction = cool(reduction, slot - 1);
		}
		if (slot < inventory.getSize() - 10 && !slots.contains(slot + 9)) {
			reduction = cool(reduction, slot + 9);
		}
		depleteItem(inventory.getItem(slot), slot);
		return heat;
	}

	IntList slots;

	public int cool(int total, int slot) {
		slots.add(slot);
		if (slot < inventory.getSize() && inventory.getItem(slot) != null && !inventory.getItem(slot).getType().equals(Material.AIR)) {
			if (ItemData.isSimilar(inventory.getItem(slot), StoneRecipes.INSTANCE.getItems().getItem("coolant"))) {
				total += coolantHeat;
				depleteItem(inventory.getItem(slot), slot);
			} else if (inventory.getItem(slot).getType().equals(Material.ICE)) {
				total += iceHeat;
				depleteItem(inventory.getItem(slot), slot);
			} else if (inventory.getItem(slot).getType().equals(Material.PACKED_ICE)) {
				total += packedIceHeat;
				depleteItem(inventory.getItem(slot), slot);
			} else if (ItemData.isSimilar(inventory.getItem(slot), StoneRecipes.INSTANCE.getItems().getItem("heat_plate"))) {
				if (slot > 8 && !slots.contains(slot - 9)) {
					total = cool(total, slot - 9);
				}
				if (slot % 8 != 0 && !slots.contains(slot + 1)) {
					total = cool(total, slot + 1);
				}
				if (slot % 9 != 0 && !slots.contains(slot - 1)) {
					total = cool(total, slot - 1);
				}
				if (slot < inventory.getSize() - 10 && !slots.contains(slot + 9)) {
					total = cool(total, slot + 9);
				}
				depleteItem(inventory.getItem(slot), slot);
			}
		}
		return total;
	}

	public void openPowerGUI(Player player) {
		onPowerChanged();
		player.openInventory(powerInv);
	}

	@Override
	public void loadConfigData(PluginFile file) {
		maxPower = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.maxPower");
		timer = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.timer");
		uraniumPower = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.uraniumPower");
		maxHeat = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.maxHeat");
		uraniumHeat = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.uraniumHeat");
		coolantHeat = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.coolantHeat");
		packedIceHeat = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.packedIceHeat");
		iceHeat = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.iceHeat");
		start_progress = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.start_progress");
	}

	public void depleteItem(ItemStack item, int slot) {
		if (item.getType().equals(Material.ICE) || item.getType().equals(Material.PACKED_ICE)) {
			if (item.getAmount() > 2) {
				item.setAmount(item.getAmount() - 2);
			} else {
				item.setType(Material.AIR);
			}
			return;
		}
		if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) { return; }
		ItemMeta meta = item.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		for (String line : meta.getLore()) {
			if (line.contains("Usage:")) {
				int current = Integer.parseInt(line.split(" ")[1].split("/")[0]);
				if (current == 1) {
					if (item.getAmount() == 1) {
						inventory.setItemInternal(slot, new ItemStack(Material.AIR));
						return;
					} else {
						item.setAmount(item.getAmount() - 1);
						lore.add("Usage: " + line.split(" ")[1].split("/")[1] + "/" + line.split(" ")[1].split("/")[1]);
					}

				} else {
					lore.add("Usage: " + (current - 1) + "/" + line.split(" ")[1].split("/")[1]);
				}
			} else {
				lore.add(line);
			}
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	@Override
	protected void dropItems() {
		Location dropLoc = location.clone().add(0.5, 0.5, 0.5);
		for (ItemStack s : inventory.getContents()) {
			if (s != null && s.getType() != Material.AIR && !ItemData.isSimilar(s, lock)) {
				location.getWorld().dropItem(dropLoc, s);
			}
		}
	}

	@Override
	public void setupContainer() {
	}

	@Override
	public void finish() {
		if (getPower() >= maxPower) return;

		int heat = 0;
		boolean fueled = false;
		for (int i = 0; i < inventory.getContents().length; i++) {
			if (inventory.getItem(i) != null) {
				ItemStack item = inventory.getItem(i);
				String id = ItemData.getItemId(item);
				if ("uranium_rod".equals(id) || "coolant".equals(id) || "heat_plate".equals(id)) {
					if ("uranium_rod".equals(id)) {
						heat += getHeat(i);
						addPower(uraniumPower);
						fueled = true;
					}
				} else {
					if (item.getType().equals(Material.ICE) || item.getType().equals(Material.PACKED_ICE)) {

					} else {
						return;
					}
				}
			}
		}
		if (getPower() > maxPower) return;
		if (heat > maxHeat) {
			explode();
			return;
		}
		if (!fueled) return;
		location.getWorld().playSound(location, "nuclear", 0.3f, 1f);
		float y = 1.5f;
		if (location.getBlock().getRelative(BlockFace.UP).getType().equals(Material.NOTE_BLOCK)) {
			y++;
		}
		if ((float) heat / (float) maxHeat < 0.5) {
			location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, location.clone().add(0.5, y, 0.5), 2);
		} else if ((float) heat / (float) maxHeat < 0.9) {
			location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location.clone().add(0.5, y, 0.5), 0, null);
		} else if ((float) heat / (float) maxHeat >= 0.9) {
			location.getWorld().spawnParticle(Particle.FLAME, location.clone().add(0.5, y + 0.1f, 0.5), 0, null);
			location.getWorld().spawnParticle(Particle.FLAME, location.clone().add(0.5, y, 0.5), 0, null);
			location.getWorld().spawnParticle(Particle.FLAME, location.clone().add(0.5, y - 0.1f, 0.5), 0, null);
		}
	}

	@Override
	public void onSlotClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == powerInv) e.setCancelled(true);
	}

	@Override
	public void handleShiftClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == powerInv) {
			e.setCancelled(true);
			return;
		}
		super.handleShiftClick(e);
	}

}

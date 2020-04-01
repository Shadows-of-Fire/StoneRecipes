package shadows.stonerecipes.tileentity.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.ReactorHandler;
import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class NuclearReactor extends PowerGenerator {

	protected NoteBlockInventory monitorInv;
	protected int maxHeat = 10;
	protected int uraniumHeat = 1;
	protected int uraniumPower = 1;
	protected int coolantHeat = 1;
	protected int packedIceHeat = 1;
	protected int iceHeat = 1;
	protected int chambers = -1;
	protected boolean exploded = false;
	protected final ItemStack lock = new ItemStack(Material.DIAMOND_HOE);
	protected final ItemStack heatBar;
	protected final ItemStack heatTex;

	protected static final BlockFace[] FACES = ReactorHandler.CHAMBER_FACES;

	@SuppressWarnings("deprecation")
	public NuclearReactor(WorldPos pos) {
		super("nuclear_reactor", "Nuclear Reactor", "config.yml", pos);
		monitorInv = new NoteBlockInventory(null, 18, "Reactor Monitor");
		ItemMeta meta = lock.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Section Locked - Add Chambers");
		meta.setUnbreakable(true);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
		lock.setItemMeta(meta);
		lock.setDurability((short) 371);
		this.updateChambers();
		this.onPowerChanged();
		this.heatBar = this.powerBar.clone();
		ItemMeta barMeta = heatBar.getItemMeta();
		barMeta.setDisplayName(ChatColor.GREEN + "Reactor Heat");
		heatBar.setItemMeta(barMeta);
		this.heatTex = guiTex.clone();
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
			monitorInv.setItemInternal(i, powerBar);
		}
		monitorInv.setItemInternal(8, guiTex);
	}

	@SuppressWarnings("deprecation")
	public void updateHeatBar(int heat) {
		ItemMeta barMeta = this.heatBar.getItemMeta();
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.YELLOW + "" + heat + "/" + this.maxHeat);
		barMeta.setLore(lore);
		this.heatBar.setItemMeta(barMeta);
		this.heatTex.setItemMeta(barMeta);
		heatTex.setDurability((short) (start_progress + Math.min(9, heat / (maxHeat / 10))));
		for (int i = 0; i < 9; i++) {
			monitorInv.setItemInternal(i + 9, heatBar);
		}
		monitorInv.setItemInternal(8 + 9, heatTex);
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
		} else StoneRecipes.INSTANCE.getLogger().info("Failed to read inventory for a " + displayName + " at " + pos.translated());
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".inv", Arrays.asList(inventory.getContents()));
		file.set(pos + ".chambers", chambers);
	}

	public void explode() {
		exploded = true;
		location.getBlock().setType(Material.AIR, false);
		location.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), 50, true, true);
		getType().remove(pos);
	}

	/**
	 * Ticks a uranium fuel rod.  Will use coolant if available.
	 * @param slot
	 * @return How much heat this rod generated, after cooling.
	 */
	public int spendUranium(int slot) {
		int heat = uraniumHeat;
		if (heat > 0 && offsetSlot(slot, BlockFace.NORTH) != -1) heat -= spendCoolant(true, heat, offsetSlot(slot, BlockFace.NORTH));
		if (heat > 0 && offsetSlot(slot, BlockFace.EAST) != -1) heat -= spendCoolant(true, heat, offsetSlot(slot, BlockFace.EAST));
		if (heat > 0 && offsetSlot(slot, BlockFace.SOUTH) != -1) heat -= spendCoolant(true, heat, offsetSlot(slot, BlockFace.SOUTH));
		if (heat > 0 && offsetSlot(slot, BlockFace.WEST) != -1) heat -= spendCoolant(true, heat, offsetSlot(slot, BlockFace.WEST));
		depleteItem(inventory.getItem(slot));
		addPower(uraniumPower);
		return Math.max(0, heat);
	}

	/**
	 * Attempts to use a coolant piece.
	 * @param slot The slot where the coolant is.
	 * @return How much heat this coolant prevented.
	 */
	public int spendCoolant(boolean plates, int heat, int slot) {
		if (slot < inventory.getSize() && inventory.getItem(slot) != null && !inventory.getItem(slot).getType().equals(Material.AIR)) {
			if (ItemData.getItemId(inventory.getItem(slot)).equals("coolant")) {
				depleteItem(inventory.getItem(slot));
				return coolantHeat;
			} else if (inventory.getItem(slot).getType().equals(Material.ICE)) {
				depleteItem(inventory.getItem(slot));
				return iceHeat;
			} else if (inventory.getItem(slot).getType().equals(Material.PACKED_ICE)) {
				depleteItem(inventory.getItem(slot));
				return packedIceHeat;
			} else if (plates && ItemData.getItemId(inventory.getItem(slot)).equals("heat_plate")) {
				int cooled = heat;
				if (cooled < heat && offsetSlot(slot, BlockFace.NORTH) != -1) cooled += spendCoolant(true, 0, offsetSlot(slot, BlockFace.NORTH));
				if (cooled < heat && offsetSlot(slot, BlockFace.EAST) != -1) cooled += spendCoolant(true, 0, offsetSlot(slot, BlockFace.EAST));
				if (cooled < heat && offsetSlot(slot, BlockFace.SOUTH) != -1) cooled += spendCoolant(true, 0, offsetSlot(slot, BlockFace.SOUTH));
				if (cooled < heat && offsetSlot(slot, BlockFace.WEST) != -1) cooled += spendCoolant(true, 0, offsetSlot(slot, BlockFace.WEST));
				depleteItem(inventory.getItem(slot));
				return Math.min(cooled, heat);
			}
		}
		return 0;
	}

	/**
	 * Offsets a slot in the given direction.
	 * @param slot The slot to offset.
	 * @param dir The direction to offset in.  This is treated as if north is to the top of the container in 2D space.
	 * @return The offset slot, or -1 if that offset would go out of bounds.
	 */
	private int offsetSlot(int slot, BlockFace dir) {
		switch (dir) {
		case NORTH:
			return slot > 8 ? slot - 9 : -1;
		case EAST:
			return slot % 8 != 0 ? slot + 1 : -1;
		case SOUTH:
			return slot + 9 < inventory.getSize() ? slot + 9 : -1;
		case WEST:
			return slot % 9 != 0 ? slot - 1 : -1;
		default:
			return -1;
		}
	}

	public void openPowerGUI(Player player) {
		onPowerChanged();
		player.openInventory(monitorInv);
	}

	@Override
	public void loadConfigData(PluginFile file) {
		maxPower = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.maxPower");
		timer = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.timer", 1);
		uraniumPower = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.uraniumPower");
		maxHeat = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.maxHeat");
		uraniumHeat = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.uraniumHeat");
		coolantHeat = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.coolantHeat");
		packedIceHeat = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.packedIceHeat");
		iceHeat = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.iceHeat");
		start_progress = StoneRecipes.INSTANCE.getConfig().getInt("nuclearReactor.start_progress");
	}

	/**
	 * Spends an item.  Uses up a charge on coolant, uranium or a heat plate, uses up an item on ice or packed ice.
	 */
	public void depleteItem(ItemStack item) {
		if (item.getType().equals(Material.ICE) || item.getType().equals(Material.PACKED_ICE)) {
			item.setAmount(item.getAmount() - 1);
			return;
		}
		if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
		ItemMeta meta = item.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		for (String line : meta.getLore()) {
			if (line.contains("Usage:")) {
				int current = Integer.parseInt(line.split(" ")[1].split("/")[0]);
				if (current == 1) {
					if (item.getAmount() == 1) {
						item.setAmount(0);
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

	/**
	 * Called every 5 ticks, this is the actual reactor update function.
	 */
	@Override
	public void finish() {
		if (getPower() >= maxPower) return;

		int heat = 0;
		boolean fueled = false;
		for (int i = 0; i < inventory.getContents().length; i++) {
			if (inventory.getItem(i) != null && getPower() + uraniumPower <= maxPower) {
				ItemStack item = inventory.getItem(i);
				String id = ItemData.getItemId(item);
				if ("uranium_rod".equals(id)) {
					fueled = true;
					heat += spendUranium(i);
				}
			}
		}

		updateHeatBar(heat);
		if (heat > maxHeat) {
			explode();
			return;
		}

		if (!fueled) return;

		//Make the reactor feel "alive"
		location.getWorld().playSound(location, "nuclear", 0.3f, 1f);
		float y = 1.5f;
		if (location.getBlock().getRelative(BlockFace.UP).getType().equals(Material.NOTE_BLOCK)) y++;
		float ratio = (float) heat / maxHeat;
		if (ratio < 0.5) {
			location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, location.clone().add(0.5, y, 0.5), 2);
		} else if (ratio < 0.9) {
			location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location.clone().add(0.5, y, 0.5), 0, null);
		} else if (ratio >= 0.9) {
			location.getWorld().spawnParticle(Particle.FLAME, location.clone().add(0.5, y + 0.1f, 0.5), 0, null);
			location.getWorld().spawnParticle(Particle.FLAME, location.clone().add(0.5, y, 0.5), 0, null);
			location.getWorld().spawnParticle(Particle.FLAME, location.clone().add(0.5, y - 0.1f, 0.5), 0, null);
		}
	}

	@Override
	public void onSlotClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == monitorInv) e.setCancelled(true);
	}

	@Override
	public void handleShiftClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == monitorInv) {
			e.setCancelled(true);
			return;
		}
		super.handleShiftClick(e);
	}

	@Override
	public NoteTileType<?> getType() {
		return NoteTypes.REACTOR;
	}

}

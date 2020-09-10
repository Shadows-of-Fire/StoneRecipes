package shadows.stonerecipes.tileentity.machine;

import java.lang.ref.WeakReference;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.item.ItemData;
import shadows.stonerecipes.listener.MoonHandler;
import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.WorldPos;

public class OxygenCompressor extends PoweredMachine {

	private WeakReference<World> moonWorld = new WeakReference<>(null);

	public OxygenCompressor(WorldPos pos) {
		super("oxygen_compressor", "Oxygen Compressor", "config.yml", pos);
	}

	@Override
	public void loadConfigData(PluginFile file) {
		this.timer = StoneRecipes.INSTANCE.getConfig().getInt("oxygenCompressor.timer", 1);
		this.start_progress = StoneRecipes.INSTANCE.getConfig().getInt("oxygenCompressor.gui");
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void timerTick() {
		if (moonWorld.get() == null) moonWorld = new WeakReference<>(Bukkit.getWorld(StoneRecipes.moonWorldName));
		if (this.pos.getDim().equals(moonWorld.get().getUID()) || getPower() == 0 || !canCharge(21) && !canCharge(22) && !canCharge(23)) {
			progress = 0;
			guiTex.setDurability((short) (start_progress + progress));
			return;
		}
		super.timerTick();
	}

	boolean canCharge(int slot) {
		ItemStack stack = inventory.getItem(slot);
		int oxygen = MoonHandler.getOxygen(stack);
		return oxygen != -1 && oxygen != MoonHandler.getMaxOxygen(stack);
	}

	@Override
	public void finish() {
		for (int i = 21; i < 24; i++) {
			charge(inventory.getItem(i));
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void read(PluginFile file) {
		super.read(file);
		inventory.setItemInternal(Slots.COAL_GEN_INPUT - 1, file.getItemStack(pos + ".slot1"));
		inventory.setItemInternal(Slots.COAL_GEN_INPUT, file.getItemStack(pos + ".slot2"));
		inventory.setItemInternal(Slots.COAL_GEN_INPUT + 1, file.getItemStack(pos + ".slot3"));
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".slot1", inventory.getItem(Slots.COAL_GEN_INPUT - 1));
		file.set(pos + ".slot2", inventory.getItem(Slots.COAL_GEN_INPUT));
		file.set(pos + ".slot3", inventory.getItem(Slots.COAL_GEN_INPUT + 1));
	}

	public void charge(ItemStack item) {
		if (item == null || !item.hasItemMeta() || item.getAmount() > 1) return;
		int oxygen = MoonHandler.getOxygen(item);
		int max = MoonHandler.getMaxOxygen(item);
		if (oxygen == -1 || oxygen >= max) return;
		else {
			int needed = Math.min(1, max - oxygen);
			int received = this.usePower(needed);
			MoonHandler.setOxygen(item, oxygen + received);
		}
	}

	@Override
	public void dropItems() {
		Location dropLoc = location.clone().add(0.5, 0.5, 0.5);
		for (int i = 0; i < inventory.getSize(); i++) {
			if (i != 8 && inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR) {
				location.getWorld().dropItem(dropLoc, inventory.getItem(i));
			}
		}
	}

	@Override
	public boolean isClickableSlot(int slot) {
		return slot >= 21 && slot <= 23;
	}

	@Override
	public void handleShiftClick(InventoryClickEvent e) {
		Inventory inv = e.getClickedInventory();
		if (inv == inventory && !isClickableSlot(e.getSlot())) {
			e.setCancelled(true);
			return;
		}
		ItemStack clicked = e.getCurrentItem();
		if (ItemData.isEmpty(clicked)) return;
		else {
			if (inv == inventory) {
				vanillaInvInsert(e.getView().getBottomInventory(), clicked);
			} else {
				boolean hotbar = e.getSlot() >= 0 && e.getSlot() < 9;
				if (MoonHandler.getOxygen(clicked) != -1) attemptMerge(inventory, clicked, Slots.COAL_GEN_INPUT - 1, Slots.COAL_GEN_INPUT + 2);
				if (!ItemData.isEmpty(clicked)) {
					if (hotbar) attemptMerge(e.getClickedInventory(), clicked, 9, 36);
					else attemptMerge(e.getClickedInventory(), clicked, 0, 9);
				}
			}
			updateAndCancel(e);
		}
	}

	static int[] slots = { Slots.COAL_GEN_INPUT - 1, Slots.COAL_GEN_INPUT, Slots.COAL_GEN_INPUT + 1 };

	@Override
	protected int[] getInputSlots() {
		return slots;
	}

	@Override
	protected int[] getOutputSlots() {
		return slots;
	}

	@Override
	protected boolean canExtract(ItemStack stack) {
		return MoonHandler.getOxygen(stack) == MoonHandler.getMaxOxygen(stack);
	}

	@Override
	public NoteTileType<?> getType() {
		return NoteTypes.OXYGEN_COMPRESSOR;
	}

}

package shadows.stonerecipes.tileentity.machine;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.WorldPos;

public class CoalGenerator extends PowerGenerator {

	private final ItemStack powerBar = new ItemStack(Material.DIAMOND_HOE);
	private final ItemStack emptyBar = new ItemStack(Material.DIAMOND_HOE);

	private int coalPower;

	private int barStart;
	private int whiteStart;

	public CoalGenerator(WorldPos pos) {
		super("generator", "Power Generator", pos);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void loadConfigData(PluginFile file) {
		this.timer = StoneRecipes.INSTANCE.getConfig().getInt("powerGenerator.timer", 1);
		this.start_progress = StoneRecipes.INSTANCE.getConfig().getInt("powerGenerator.gui");
		this.barStart = StoneRecipes.INSTANCE.getConfig().getInt("powerGenerator.bar");
		this.whiteStart = StoneRecipes.INSTANCE.getConfig().getInt("powerGenerator.white");
		this.maxPower = StoneRecipes.INSTANCE.getConfig().getInt("powerGenerator.max_power");
		this.coalPower = StoneRecipes.INSTANCE.getConfig().getInt("powerGenerator.coal_power");
		guiTex.setDurability((short) (start_progress));
		ItemMeta meta = guiTex.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(" ");
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
		guiTex.setItemMeta(meta);
		inventory.setItemInternal(Slots.GUI_TEX_SLOT, guiTex);
		this.powerBar.setDurability((short) this.barStart);
		ItemMeta barMeta = powerBar.getItemMeta();
		barMeta.setDisplayName(ChatColor.GREEN + "Power Storage");
		barMeta.setUnbreakable(true);
		barMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
		this.powerBar.setItemMeta(barMeta);
		this.emptyBar.setDurability((short) this.whiteStart);
		ItemMeta whiteMeta = emptyBar.getItemMeta();
		whiteMeta.setDisplayName(ChatColor.GREEN + "Power Storage");
		whiteMeta.setUnbreakable(true);
		whiteMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
		this.emptyBar.setItemMeta(whiteMeta);
		inventory.setItemInternal(1, this.emptyBar);
		inventory.setItemInternal(10, this.emptyBar);
		inventory.setItemInternal(19, this.emptyBar);
		inventory.setItemInternal(28, this.emptyBar);
		inventory.setItemInternal(37, this.emptyBar);
		inventory.setItemInternal(46, this.powerBar);
	}

	@Override
	public void setupContainer() {
		onPowerChanged();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void read(PluginFile file) {
		super.read(file);
		inventory.setItemInternal(Slots.COAL_GEN_INPUT, file.getItemStack(pos + ".coal"));
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".coal", inventory.getItem(Slots.COAL_GEN_INPUT));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onPowerChanged() {
		int damage = (int) ((double) this.getPower() / (double) this.maxPower * 10);
		this.powerBar.setDurability((short) (barStart + damage));
		ItemMeta barMeta = this.powerBar.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		lore.add(ChatColor.YELLOW + "" + this.getPower() + "/" + this.maxPower);
		barMeta.setLore(lore);
		this.powerBar.setItemMeta(barMeta);
		inventory.setItemInternal(46, powerBar);
		ItemMeta whiteMeta = this.emptyBar.getItemMeta();
		whiteMeta.setLore(lore);
		this.emptyBar.setItemMeta(whiteMeta);
		inventory.setItemInternal(1, this.emptyBar);
		inventory.setItemInternal(10, this.emptyBar);
		inventory.setItemInternal(19, this.emptyBar);
		inventory.setItemInternal(28, this.emptyBar);
		inventory.setItemInternal(37, this.emptyBar);
	}

	@Override
	public void timerTick() {
		if (inventory.getItem(Slots.COAL_GEN_INPUT) == null || this.getPower() + this.coalPower > this.maxPower) {
			return;
		}
		super.timerTick();
	}

	@Override
	public void finish() {
		ItemStack input = inventory.getItem(Slots.COAL_GEN_INPUT);
		input.setAmount(input.getAmount() - 1);
		addPower(coalPower);
		this.onPowerChanged();
	}

	@Override
	public boolean isClickableSlot(int slot) {
		return slot == Slots.COAL_GEN_INPUT;
	}

	@Override
	public void handleShiftClick(InventoryClickEvent e) {
		Inventory inv = e.getClickedInventory();
		if (inv == inventory && e.getSlot() != Slots.COAL_GEN_INPUT) {
			e.setCancelled(true);
			return;
		}
		ItemStack clicked = e.getCurrentItem();
		if (isEmpty(clicked)) return;
		else {
			if (inv == inventory) {
				vanillaInvInsert(e.getView().getBottomInventory(), clicked);
			} else {
				boolean hotbar = e.getSlot() >= 0 && e.getSlot() < 9;
				if (clicked.getType() == Material.COAL) attemptMerge(inventory, clicked, Slots.COAL_GEN_INPUT, Slots.COAL_GEN_INPUT + 1);
				if (!isEmpty(clicked)) {
					if (hotbar) attemptMerge(e.getClickedInventory(), clicked, 9, 36);
					else attemptMerge(e.getClickedInventory(), clicked, 0, 9);
				}
			}
			updateAndCancel(e);
		}
	}

	@Override
	public void onSlotClick(InventoryClickEvent e) {
		if (e.getSlot() == Slots.COAL_GEN_INPUT && e.getCursor() != null && e.getCursor().getType() != Material.AIR && e.getCursor().getType() != Material.COAL) {
			updateAndCancel(e);
		}
	}

	static int[] slots = { Slots.COAL_GEN_INPUT };

	@Override
	protected int[] getInputSlots() {
		return slots;
	}

}

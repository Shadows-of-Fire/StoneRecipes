package shadows.stonerecipes.tileentity.machine;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.WorldPos;

public class Charger extends PoweredMachine implements Listener {

	public Charger(WorldPos pos) {
		super("armor_charger", "Charger", "config.yml", pos);
	}

	@Override
	public void loadConfigData(PluginFile file) {
		this.timer = StoneRecipes.INSTANCE.getConfig().getInt("armorCharger.timer");
		this.start_progress = StoneRecipes.INSTANCE.getConfig().getInt("armorCharger.gui");
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void timerTick() {
		if (getPower() == 0 || (!canCharge(21) && !canCharge(22) && !canCharge(23))) {
			progress = 0;
			guiTex.setDurability((short) (start_progress + progress));
			return;
		}
		super.timerTick();
	}

	boolean canCharge(int slot) {
		ItemStack stack = inventory.getItem(slot);
		int power = getPower(stack);
		return power != -1 && power != getMaxPower(stack);
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
		int power = getPower(item);
		int max = getMaxPower(item);
		if (power == -1 || power >= max) return;
		else {
			int needed = Math.min(100, max - power);
			int received = this.usePower(needed);
			setPower(item, power + received);
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
		if (isEmpty(clicked)) return;
		else {
			if (inv == inventory) {
				vanillaInvInsert(e.getView().getBottomInventory(), clicked);
			} else {
				boolean hotbar = e.getSlot() >= 0 && e.getSlot() < 9;
				if (getPower(clicked) != -1) attemptMerge(inventory, clicked, Slots.COAL_GEN_INPUT - 1, Slots.COAL_GEN_INPUT + 2);
				if (!isEmpty(clicked)) {
					if (hotbar) attemptMerge(e.getClickedInventory(), clicked, 9, 36);
					else attemptMerge(e.getClickedInventory(), clicked, 0, 9);
				}
			}
			updateAndCancel(e);
		}
	}

	public static int getPower(ItemStack stack) {
		if (NoteTileEntity.isEmpty(stack) || !stack.hasItemMeta()) return -1;
		return stack.getItemMeta().getPersistentDataContainer().getOrDefault(ItemData.POWER, PersistentDataType.INTEGER, -1);
	}

	public static int getMaxPower(ItemStack stack) {
		if (NoteTileEntity.isEmpty(stack) || !stack.hasItemMeta()) return -1;
		return stack.getItemMeta().getPersistentDataContainer().getOrDefault(ItemData.MAX_POWER, PersistentDataType.INTEGER, -1);
	}

	public static void setPower(ItemStack stack, int power) {
		if (NoteTileEntity.isEmpty(stack) || !stack.hasItemMeta()) return;
		int max = getMaxPower(stack);
		if (power > max) power = max;
		ItemMeta meta = stack.getItemMeta();
		meta.getPersistentDataContainer().set(ItemData.POWER, PersistentDataType.INTEGER, power);
		List<String> lore = meta.getLore();
		lore.set(0, ChatColor.translateAlternateColorCodes('&', String.format("&r&aPower: %d/%d", power, max)));
		meta.setLore(lore);
		stack.setItemMeta(meta);
	}

	public static void usePower(ItemStack stack, int power) {
		setPower(stack, getPower(stack) - power);
	}

	@Override
	protected int getMaxStackSize() {
		return 1;
	}

}

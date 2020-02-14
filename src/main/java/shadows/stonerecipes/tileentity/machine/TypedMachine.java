package shadows.stonerecipes.tileentity.machine;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.WorldPos;

/**
 * A typed machine is a simple 1->1 input output machine.
 */
public class TypedMachine extends PoweredMachine {

	protected final String type;
	protected final ItemStack infoHoe = new ItemStack(Material.DIAMOND_HOE);
	protected String infoCmd;
	protected int upgradeTicks = 0;

	public TypedMachine(String type, WorldPos pos) {
		super(type, type, "machines.yml", pos);
		this.type = type;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void loadConfigData(PluginFile file) {
		this.timer = file.getInt(itemName + ".timer");
		this.start_progress = file.getInt(itemName + ".start_progress");
		this.powerCost = file.getInt(itemName + ".powerCost");
		this.maxPower = Math.max(maxPower, powerCost);
		this.infoCmd = file.getString(itemName + ".infocmd", "");
		infoHoe.setDurability((short) 77);
		ItemMeta meta = infoHoe.getItemMeta();
		meta.setDisplayName(ChatColor.AQUA + "Information");
		meta.setUnbreakable(true);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
		infoHoe.setItemMeta(meta);
	}

	public String getType() {
		return type;
	}

	@Override
	public void setupContainer() {
		super.setupContainer();
		inventory.setItemInternal(Slots.INFO, infoHoe);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void read(PluginFile file) {
		super.read(file);
		inventory.setItemInternal(Slots.INPUT, file.getItemStack(pos + ".input"));
		inventory.setItemInternal(Slots.OUTPUT, file.getItemStack(pos + ".output"));
		inventory.setItemInternal(Slots.UPGRADE_0, file.getItemStack(pos + ".up0"));
		inventory.setItemInternal(Slots.UPGRADE_1, file.getItemStack(pos + ".up1"));
		inventory.setItemInternal(Slots.UPGRADE_2, file.getItemStack(pos + ".up2"));
		inventory.setItemInternal(Slots.UPGRADE_3, file.getItemStack(pos + ".up3"));
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".type", type);
		file.set(pos + ".input", inventory.getItem(Slots.INPUT));
		file.set(pos + ".output", inventory.getItem(Slots.OUTPUT));
		file.set(pos + ".up0", inventory.getItem(Slots.UPGRADE_0));
		file.set(pos + ".up1", inventory.getItem(Slots.UPGRADE_1));
		file.set(pos + ".up2", inventory.getItem(Slots.UPGRADE_2));
		file.set(pos + ".up3", inventory.getItem(Slots.UPGRADE_3));
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void timerTick() {
		ItemStack input = inventory.getItem(Slots.INPUT);
		ItemStack output = StoneRecipes.INSTANCE.getMachines().getOutput(type, input);
		if (input == null || output == null || (inventory.getItem(Slots.OUTPUT) != null && !ItemData.isSimilar(inventory.getItem(Slots.OUTPUT), output))) {
			progress = 0;
			guiTex.setDurability((short) (start_progress + progress));
			return;
		}
		super.timerTick();
	}

	@Override
	protected void tickInternal() {
		super.tickInternal();
		upgradeTicks++;
		int bonus = 0;
		for (int i : Slots.UPGRADES) {
			ItemStack s = this.inventory.getItem(i);
			if (s != null && s.hasItemMeta()) {
				if (upgradeTicks % s.getItemMeta().getPersistentDataContainer().getOrDefault(ItemData.SPEED, PersistentDataType.SHORT, Short.MAX_VALUE) == 0) {
					bonus++;
				}
			}
		}
		for (int i = 0; i < bonus; i++)
			super.tickInternal();
	}

	@Override
	public void finish() {
		ItemStack input = inventory.getItem(Slots.INPUT);
		ItemStack output = inventory.getItem(Slots.OUTPUT);
		ItemStack recipeOut = StoneRecipes.INSTANCE.getMachines().getOutput(type, input);
		if (output != null && output.getType() != Material.AIR && output.getAmount() > 0) {
			if (ItemData.isSimilar(recipeOut, output) && output.getAmount() + recipeOut.getAmount() <= output.getMaxStackSize()) {
				this.usePower(powerCost);
				output.setAmount(output.getAmount() + recipeOut.getAmount());
			} else return;
		} else {
			this.usePower(powerCost);
			inventory.setItemInternal(Slots.OUTPUT, recipeOut.clone());
		}
		input.setAmount(input.getAmount() - 1);
	}

	@Override
	public void handleShiftClick(InventoryClickEvent e) {
		Inventory inv = e.getClickedInventory();
		if (inv == inventory && (!isClickableSlot(e.getSlot()) || e.getSlot() == Slots.INFO)) {
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
				if (StoneRecipes.INSTANCE.getMachines().getOutput(type, clicked) != null) attemptMerge(inventory, clicked, Slots.INPUT, Slots.INPUT + 1);
				if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(ItemData.SPEED, PersistentDataType.SHORT)) {
					attemptMerge(inventory, clicked, Slots.UPGRADE_0, Slots.UPGRADE_1 + 1);
					if (!isEmpty(clicked)) attemptMerge(inventory, clicked, Slots.UPGRADE_2, Slots.UPGRADE_3 + 1);
				}
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
		if (e.getSlot() == Slots.INFO) {
			Bukkit.getServer().dispatchCommand(e.getWhoClicked(), infoCmd);
			updateAndCancel(e);
		}
		if (e.getSlot() == Slots.UPGRADE_0 || e.getSlot() == Slots.UPGRADE_1 || e.getSlot() == Slots.UPGRADE_2 || e.getSlot() == Slots.UPGRADE_3) {
			if (e.getCursor() != null && e.getCursor().getType() != Material.AIR && (!e.getCursor().hasItemMeta() || !e.getCursor().getItemMeta().getPersistentDataContainer().has(ItemData.SPEED, PersistentDataType.SHORT))) {
				updateAndCancel(e);
			}
		}
	}

	@Override
	public boolean isClickableSlot(int slot) {
		return slot == Slots.INFO || slot == Slots.INPUT || slot == Slots.OUTPUT || slot == Slots.UPGRADE_0 || slot == Slots.UPGRADE_1 || slot == Slots.UPGRADE_2 || slot == Slots.UPGRADE_3;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj) && ((TypedMachine) obj).type.equals(type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getClass(), this.type, this.pos);
	}

	@Override
	public String toString() {
		return String.format("Machine: %s, Location: %s", StringUtils.capitalize(type), this.pos.translated());
	}

	@Override
	protected void dropItems() {
		Location dropLoc = location.clone().add(0.5, 0.5, 0.5);
		for (int i = 0; i < inventory.getSize(); i++) {
			if (i != Slots.GUI_TEX_SLOT && i != Slots.INFO && inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR) {
				location.getWorld().dropItem(dropLoc, inventory.getItem(i));
			}
		}
	}

	static int[] in = { Slots.INPUT }, out = { Slots.OUTPUT };

	@Override
	protected int[] getInputSlots() {
		return in;
	}

	@Override
	protected int[] getOutputSlots() {
		return out;
	}

}

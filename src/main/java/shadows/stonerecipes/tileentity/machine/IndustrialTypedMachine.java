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
import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.WorldPos;

/**
 * An industrial typed machine is a typed machine, but with 4 output and 4 input slots.
 */
public class IndustrialTypedMachine extends PoweredMachine {

	public static final int[] INPUTS = { Slots.INPUT, Slots.INPUT - 1, Slots.INPUT - 9, Slots.INPUT - 9 - 1 };
	public static final int[] OUTPUTS = { Slots.OUTPUT, Slots.OUTPUT + 1, Slots.OUTPUT - 9, Slots.OUTPUT - 9 + 1 };

	protected final String type;
	protected final ItemStack infoHoe = new ItemStack(Material.DIAMOND_HOE);
	protected String infoCmd;
	protected int upgradeTicks = 0;

	public IndustrialTypedMachine(String type, WorldPos pos) {
		super(type, type, "industrial_machines.yml", pos);
		this.type = type;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void loadConfigData(PluginFile file) {
		this.timer = file.getInt(itemName + ".timer", 1);
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

	@Override
	public NoteTileType<?> getType() {
		return NoteTypes.INDUSTRIAL_TYPED_MACHINE;
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
		for (int i = 0; i < 4; i++) {
			inventory.setItemInternal(INPUTS[i], file.getItemStack(pos + ".input" + i));
			inventory.setItemInternal(OUTPUTS[i], file.getItemStack(pos + ".output" + i));
		}
		inventory.setItemInternal(Slots.UPGRADE_0, file.getItemStack(pos + ".up0"));
		inventory.setItemInternal(Slots.UPGRADE_1, file.getItemStack(pos + ".up1"));
		inventory.setItemInternal(Slots.UPGRADE_2, file.getItemStack(pos + ".up2"));
		inventory.setItemInternal(Slots.UPGRADE_3, file.getItemStack(pos + ".up3"));
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".type", type);
		for (int i = 0; i < 4; i++) {
			file.set(pos + ".input" + i, inventory.getItem(INPUTS[i]));
			file.set(pos + ".output" + i, inventory.getItem(OUTPUTS[i]));
		}
		file.set(pos + ".up0", inventory.getItem(Slots.UPGRADE_0));
		file.set(pos + ".up1", inventory.getItem(Slots.UPGRADE_1));
		file.set(pos + ".up2", inventory.getItem(Slots.UPGRADE_2));
		file.set(pos + ".up3", inventory.getItem(Slots.UPGRADE_3));
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void timerTick() {
		boolean hasNoInputs = true;
		for (int i = 0; i < 4; i++) {
			ItemStack input = inventory.getItem(INPUTS[i]);
			ItemStack output = StoneRecipes.INSTANCE.getRecipes().getMachineOutput(type, input);
			if (input != null && (output == null || ItemData.isSimilar(inventory.getItem(Slots.OUTPUT), output))) {
				hasNoInputs = false;
			}
		}
		if (hasNoInputs) {
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
		for (int i = 0; i < 4; i++) {
			ItemStack input = inventory.getItem(INPUTS[i]);
			ItemStack output = inventory.getItem(OUTPUTS[i]);
			ItemStack recipeOut = StoneRecipes.INSTANCE.getRecipes().getMachineOutput(type, input);
			if (output != null && output.getType() != Material.AIR && output.getAmount() > 0) {
				if (ItemData.isSimilar(recipeOut, output) && output.getAmount() + recipeOut.getAmount() <= output.getMaxStackSize()) {
					this.usePower(powerCost);
					output.setAmount(output.getAmount() + recipeOut.getAmount());
				} else return;
			} else {
				this.usePower(powerCost);
				inventory.setItemInternal(OUTPUTS[i], recipeOut.clone());
			}
			input.setAmount(input.getAmount() - 1);
		}
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
				if (StoneRecipes.INSTANCE.getRecipes().getMachineOutput(type, clicked) != null) {
					attemptMerge(inventory, clicked, Slots.INPUT - 1, Slots.INPUT + 2);
					if (!isEmpty(clicked)) attemptMerge(inventory, clicked, Slots.INPUT - 1 - 9, Slots.INPUT + 2 - 9);
				}
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
		for (int i = 0; i < 4; i++) {
			if (slot == INPUTS[i] || slot == OUTPUTS[i]) return true;
		}
		return slot == Slots.INFO || slot == Slots.UPGRADE_0 || slot == Slots.UPGRADE_1 || slot == Slots.UPGRADE_2 || slot == Slots.UPGRADE_3;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj) && ((IndustrialTypedMachine) obj).type.equals(type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getClass(), this.type, this.pos);
	}

	@Override
	public String toString() {
		return String.format("Industrial Machine: %s, Location: %s", StringUtils.capitalize(type), this.pos.translated());
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

	@Override
	protected int[] getInputSlots() {
		return INPUTS;
	}

	@Override
	protected int[] getOutputSlots() {
		return OUTPUTS;
	}

}

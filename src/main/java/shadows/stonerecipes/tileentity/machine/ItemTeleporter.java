package shadows.stonerecipes.tileentity.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.util.ITeleporter;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.WorldPos;

/**
 * The item teleporter is a sort of "hopper" between machines.
 * When unlinked, it will attempt to transfer items from above itself into itself, and then below itself.
 * When linked, it will attempt to transfer items from above itself to below the linked
 */
public class ItemTeleporter extends PoweredMachine implements ITeleporter {

	protected WorldPos link = WorldPos.INVALID;

	private ItemStack tpButton = new ItemStack(Material.DIAMOND_HOE);

	public ItemTeleporter(WorldPos pos) {
		super("item_teleporter", "Item Teleporter", "config.yml", pos);
		this.updater = false;
		this.timer = 10;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setupContainer() {
		super.setupContainer();
		ItemMeta buttonMeta = guiTex.getItemMeta();
		buttonMeta.setDisplayName(ChatColor.YELLOW + "Teleport Items");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.YELLOW + "Costs " + powerCost + " Power");
		buttonMeta.setLore(lore);
		this.tpButton.setItemMeta(buttonMeta);
		tpButton.setDurability((short) 370);
		onPowerChanged();
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
		for (int i = 0; i < 8; i++) {
			inventory.setItemInternal(i, powerBar);
		}
		inventory.setItemInternal(7, guiTex);
		inventory.setItemInternal(8, tpButton);
	}

	public void teleportItems() {
		if (link.equals(WorldPos.INVALID) || getPower() < powerCost) return;
		ItemTeleporter dest = StoneRecipes.INSTANCE.getTeleporters().hotloadItemT(link);
		if (dest == null) {
			this.link = WorldPos.INVALID;
			return;
		}
		Inventory temp = Bukkit.createInventory(null, 54);
		for (int i = 9; i < 54; i++) {
			temp.setItem(i, inventory.getItem(i));
		}
		for (int i = 9; i < 54; i++) {
			inventory.setItemInternal(i, dest.inventory.getItem(i));
		}
		for (int i = 9; i < 54; i++) {
			dest.inventory.setItemInternal(i, temp.getItem(i));
		}
		usePower(powerCost);
	}

	@Override
	public boolean isClickableSlot(int slot) {
		return slot >= Slots.GUI_TEX_SLOT;
	}

	@Override
	public void onSlotClick(InventoryClickEvent e) {
		if (e.getSlot() == Slots.GUI_TEX_SLOT) {
			e.setCancelled(true);
			teleportItems();
		}
	}

	@Override
	public void loadConfigData(PluginFile file) {
		this.timer = 5;
		this.powerCost = StoneRecipes.INSTANCE.getConfig().getInt("itemTP.powerCost");
		this.start_progress = StoneRecipes.INSTANCE.getConfig().getInt("itemTP.start_progress");
		this.maxPower = StoneRecipes.INSTANCE.getConfig().getInt("itemTP.maxPower");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(PluginFile file) {
		super.read(file);
		if (file.isList(pos.toString() + ".inv")) {
			List<ItemStack> content = (List<ItemStack>) file.getList(pos + ".inv");
			for (int i = 0; i < 54; i++) {
				if (content.get(i) != null) {
					inventory.setItemInternal(i, content.get(i));
				}
			}
			this.link = new WorldPos(file.getString(pos + ".link"));
		} else StoneRecipes.INSTANCE.getLogger().info("Failed to read inventory for a " + name + " at " + pos.translated());
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".link", link.toString());
		file.set(pos + ".inv", Arrays.asList(inventory.getContents()));
	}

	@Override
	public void dropItems() {
		Location dropLoc = location.clone().add(0.5, 0.5, 0.5);
		for (int i = 9; i < inventory.getSize(); i++) {
			if (inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR) {
				location.getWorld().dropItem(dropLoc, inventory.getItem(i));
			}
		}
	}

	@Override
	public void setLink(WorldPos link) {
		this.link = link;
	}

	@Override
	public WorldPos getLink() {
		return link;
	}

	@Override
	public void handleShiftClick(InventoryClickEvent e) {
		Inventory inv = e.getClickedInventory();
		if (inv == inventory && e.getSlot() < 9) {
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
				attemptMerge(inventory, clicked, 9, 54);
				if (!isEmpty(clicked)) {
					if (hotbar) attemptMerge(e.getClickedInventory(), clicked, 9, 36);
					else attemptMerge(e.getClickedInventory(), clicked, 0, 9);
				}
			}
			updateAndCancel(e);
		}
	}

	@Override
	protected void tickInternal() {
		if (++ticks % timer == 0) timerTick();
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void timerTick() {
		if (getPower() < powerCost) {
			progress = 0;
			guiTex.setDurability((short) (start_progress + progress));
			return;
		}
		super.timerTick();
	}

}

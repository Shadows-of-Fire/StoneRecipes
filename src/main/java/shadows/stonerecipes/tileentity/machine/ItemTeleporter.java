package shadows.stonerecipes.tileentity.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.DataHandler.Maps;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.Laser;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.WorldPos;

/**
 * The item teleporter is a sort of "hopper" between machines.
 * When unlinked, it will attempt to transfer items from above itself into itself, and then below itself.
 * When linked, it will attempt to transfer items from above itself to below the linked
 */
public class ItemTeleporter extends PoweredMachine {

	protected WorldPos destination = WorldPos.INVALID;

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

	@Override
	public boolean isClickableSlot(int slot) {
		return slot >= Slots.GUI_TEX_SLOT;
	}

	@Override
	public void onSlotClick(InventoryClickEvent e) {
		if (e.getSlot() == Slots.GUI_TEX_SLOT) {
			e.setCancelled(true);
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
			if (file.contains(pos + ".destination")) this.destination = new WorldPos(file.getString(pos + ".destination"));
		} else StoneRecipes.INSTANCE.getLogger().info("Failed to read inventory for a " + name + " at " + pos.translated());
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".destination", destination.toString());
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

	public void setDestination(WorldPos link) {
		this.destination = link;
	}

	public WorldPos getDestination() {
		return this.destination;
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
		NoteTileEntity above = Maps.ALL_MACHINES.get(this.pos.up());

		if (above != null) {
			ItemStack ext = above.extractItem(64, true);
			if (isEmpty(ext)) return;
			ItemStack res = this.insertItem(ext, false);
			above.extractItem(ext.getAmount() - (isEmpty(res) ? 0 : res.getAmount()), false);
		} else {
			BlockState state = this.location.getWorld().getBlockAt(location.clone().add(0, 1, 0)).getState();
			if (state instanceof InventoryHolder) {
				Inventory inv = state instanceof Chest ? ((Chest) state).getBlockInventory() : ((InventoryHolder) state).getInventory();
				ItemStack ext = ItemData.EMPTY;
				int slot = -1;
				for (int i = 0; i < inv.getSize(); i++) {
					ItemStack s = inv.getItem(i);
					if (!isEmpty(s)) {
						ext = s;
						slot = i;
						break;
					}
				}
				if (isEmpty(ext)) return;
				ItemStack res = this.insertItem(ext, false);
				ext.setAmount(ext.getAmount() - (isEmpty(res) ? 0 : res.getAmount()));
				inv.setItem(slot, ext);
			}
		}

		if (destination.equals(WorldPos.INVALID)) {
			NoteTileEntity below = Maps.ALL_MACHINES.get(this.pos.down());
			if (below != null) {
				ItemStack ext = this.extractItem(64, true);
				if (isEmpty(ext)) return;
				ItemStack res = below.insertItem(ext, false);
				this.extractItem(ext.getAmount() - (isEmpty(res) ? 0 : res.getAmount()), false);
			}
		} else {
			NoteTileEntity link = Maps.ITEM_TELEPORTERS.get(this.destination);
			if (link == null) {
				destination = WorldPos.INVALID;
				return;
			}
			ItemStack ext = this.extractItem(64, true);
			if (isEmpty(ext)) return;
			ItemStack res = link.insertItem(ext, false);
			if (res.getAmount() == ext.getAmount()) return;
			this.extractItem(ext.getAmount() - (isEmpty(res) ? 0 : res.getAmount()), false);
			new Laser(this.location.clone().add(0.5, 0.5, 0.5), this.destination.toLocation().add(0.5, 0.5, 0.5), Color.BLUE).connect();
		}
	}

	static int[] slots;
	static {
		IntList ints = new IntArrayList();
		for (int i = 9; i < 54; i++) {
			ints.add(i);
		}
		slots = ints.toIntArray();
	}

	@Override
	protected int[] getInputSlots() {
		return slots;
	}

	@Override
	protected int[] getOutputSlots() {
		return slots;
	}

}

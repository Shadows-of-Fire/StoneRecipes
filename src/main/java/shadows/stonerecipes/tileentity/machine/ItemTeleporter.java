package shadows.stonerecipes.tileentity.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
import shadows.stonerecipes.item.ItemData;
import shadows.stonerecipes.listener.DataHandler.Maps;
import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.tileentity.NoteTileEntity;
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
		buttonMeta.setDisplayName(" ");
		this.tpButton.setItemMeta(buttonMeta);
		tpButton.setDurability((short) 66);
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
		} else StoneRecipes.INSTANCE.getLogger().info("Failed to read inventory for a " + displayName + " at " + pos.translated());
		if (file.contains(pos + ".destination")) this.destination = new WorldPos(file.getString(pos + ".destination"));
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
		if (ItemData.isEmpty(clicked)) return;
		else {
			if (inv == inventory) {
				vanillaInvInsert(e.getView().getBottomInventory(), clicked);
			} else {
				boolean hotbar = e.getSlot() >= 0 && e.getSlot() < 9;
				attemptMerge(inventory, clicked, 9, 54);
				if (!ItemData.isEmpty(clicked)) {
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
	protected void timerTick() {
		if (receivesPower && ticks % 40 == 0) {
			int needed = this.maxPower - getPower();
			if (needed == 0) return;
			addPower(receivePower(this, needed));
		}
		extract();
		insert();
	}

	private void extract() {
		NoteTileEntity above = Maps.ALL_MACHINES.getOrDefault(this.pos.toChunkCoords(), Collections.emptyMap()).get(this.pos.up());
		if (above != null) {
			ItemStack ext = above.extractItem(64, true);
			if (ItemData.isEmpty(ext)) return;
			ItemStack res = this.insertItem(ext, false);
			above.extractItem(ext.getAmount() - (ItemData.isEmpty(res) ? 0 : res.getAmount()), false);
		} else {
			Block b = this.location.getWorld().getBlockAt(location.clone().add(0, 1, 0));
			BlockState state = b.getState();
			if (state instanceof InventoryHolder) {
				Inventory inv = state instanceof Chest ? ((Chest) state).getBlockInventory() : ((InventoryHolder) state).getInventory();
				ItemStack ext = ItemData.EMPTY;
				int slot = -1;
				for (int i = 0; i < inv.getSize(); i++) {
					ItemStack s = inv.getItem(i);
					if (!ItemData.isEmpty(s)) {
						ext = s;
						slot = i;
						break;
					}
				}
				if (ItemData.isEmpty(ext)) return;
				ItemStack res = this.insertItem(ext, false);
				ext.setAmount(ItemData.isEmpty(res) ? 0 : res.getAmount());
				inv.setItem(slot, ext);
			}
		}
	}

	private void insert() {
		if (destination.equals(WorldPos.INVALID)) {
			NoteTileEntity below = Maps.ALL_MACHINES.getOrDefault(this.pos.toChunkCoords(), Collections.emptyMap()).get(this.pos.down());
			if (below != null) {
				ItemStack ext = this.extractItem(64, true);
				if (ItemData.isEmpty(ext)) return;
				ItemStack res = below.insertItem(ext, false);
				this.extractItem(ext.getAmount() - (ItemData.isEmpty(res) ? 0 : res.getAmount()), false);
			} else {
				Block b = this.location.getWorld().getBlockAt(location.clone().add(0, -1, 0));
				BlockState state = b.getState();
				if (state instanceof InventoryHolder) {
					Inventory inv = state instanceof Chest ? ((Chest) state).getBlockInventory() : ((InventoryHolder) state).getInventory();
					ItemStack ext = this.extractItem(64, true);
					if (ItemData.isEmpty(ext)) return;
					ItemStack res = insertItemVanilla(inv, ext, false);
					this.extractItem(ext.getAmount() - (ItemData.isEmpty(res) ? 0 : res.getAmount()), false);
				}
			}
		} else {
			NoteTileEntity link = NoteTypes.ITEM_TELEPORTER.getMap().get(this.destination);
			if (link == null) {
				destination = WorldPos.INVALID;
				return;
			}
			if (this.getPower() == 0) return;
			ItemStack ext = this.extractItem(Math.min(this.getPower(), 64), true);
			if (ItemData.isEmpty(ext)) return;
			ItemStack res = link.insertItem(ext, false);
			if (!ItemData.isEmpty(res) && ext.getAmount() == res.getAmount()) return;
			int sent = ext.getAmount() - (ItemData.isEmpty(res) ? 0 : res.getAmount());
			this.extractItem(sent, false);
			new Laser(this.location.clone().add(0.5, 0.5, 0.5), this.destination.toLocation().add(0.5, 0.5, 0.5), Color.BLUE).connect();
			this.usePower(sent);
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

	private static ItemStack insertItemVanilla(Inventory inv, ItemStack stack, boolean simulate) {
		int max = Math.min(inv.getMaxStackSize(), stack.getMaxStackSize());
		for (int i = 0; i < inv.getSize(); i++) {
			if (ItemData.isEmpty(stack)) return ItemData.EMPTY;
			ItemStack slot = inv.getItem(i);
			if (ItemData.isSimilar(slot, stack) && slot.getAmount() < max) {
				ItemStack sClone = stack.clone();
				if (simulate) {
					//never actually called, NYI
					throw new Error("Not Yet Implemented.");
				} else {
					int old = slot.getAmount();
					slot.setAmount(Math.min(max, old + stack.getAmount()));
					sClone.setAmount(sClone.getAmount() - (slot.getAmount() - old));
				}
				stack = sClone;
			} else if (ItemData.isEmpty(slot)) {
				inv.setItem(i, stack.clone());
				return ItemData.EMPTY;
			}
		}
		return stack;
	}

	@Override
	public NoteTileType<?> getType() {
		return NoteTypes.ITEM_TELEPORTER;
	}
}

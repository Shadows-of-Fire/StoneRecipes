package shadows.stonerecipes.tileentity;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.text.WordUtils;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftInventoryCustom;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.DataHandler;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.TickHandler;
import shadows.stonerecipes.util.TickHandler.ITickable;
import shadows.stonerecipes.util.WorldPos;

/**
 * Class that represents an abstracted tile entity system.  These are used on note blocks to represent custom tile entities.
 */
public abstract class NoteTileEntity implements ITickable {

	public static final int MAX_PROGRESS = 5;

	protected final String name;
	protected final String itemName;
	protected final WorldPos pos;
	protected final Location location;
	protected final NoteBlockInventory inventory;
	protected final ItemStack guiTex = new ItemStack(Material.DIAMOND_HOE);
	private final String file;

	protected boolean updater = true;
	protected int start_progress;
	protected int progress = 0;
	protected int timer = 1;
	protected int ticks = 0;
	protected boolean dead = false;

	/**
	 * Basic machine constructor.
	 * @param itemName The name of the item that represents this machine.
	 * @param name The literal english name of this machine.  Used for GUI displays.
	 * @param file The file that this machine's data should be loaded from.  @Deprecated should be removed in the future.  Possibly changed to enum.
	 * @param pos Where this machine is in the world.  Creating a machine that is not where a Note Block is will cause a ClassCastException.
	 */
	public NoteTileEntity(String itemName, String name, @Deprecated String file, WorldPos pos) {
		this.name = name;
		this.itemName = itemName;
		this.pos = pos;
		this.location = pos.toLocation();
		this.inventory = new NoteBlockInventory(null, 54, localize(this.name));
		this.file = file;
	}

	private String localize(String name) {
		return WordUtils.capitalize(name.replace("_", " "));
	}

	public NoteBlockInventory getInv() {
		return inventory;
	}

	/**
	 * Starts the cycle of this machine.  This machine will then tick.
	 */
	public void start() {
		TickHandler.registerTickable(this);
		this.loadConfigData(new PluginFile(StoneRecipes.INSTANCE, file));
		DataHandler.needsUnload(location.getChunk());
	}

	/**
	 * Completes a cycle.  This is run (by default) every 5 updates.
	 */
	public void finish() {

	}

	@Override
	public final void tick() {
		tickInternal();
	}

	@Override
	public boolean isDead() {
		return dead;
	}

	/**
	 * Ticks this tile.  This method is called every game tick.
	 */
	protected void tickInternal() {
		if (++ticks % timer == 0) timerTick();
	}

	/**
	 * This method is called once the time specified by {@link NoteTileEntity#getTimer()} has elapsed
	 */
	@SuppressWarnings("deprecation")
	protected void timerTick() {
		if (++progress >= MAX_PROGRESS) {
			progress = 0;
			finish();
		}
		if (updater) {
			guiTex.setDurability((short) (start_progress + progress));
			inventory.setItemInternal(Slots.GUI_TEX_SLOT, guiTex);
		}
	}

	/**
	 * Shuts down this tile.
	 */
	@SuppressWarnings("deprecation")
	private void stop() {
		this.dead = true;
		progress = 0;
		guiTex.setDurability((short) start_progress);
		inventory.setItemInternal(Slots.GUI_TEX_SLOT, guiTex);
	}

	/**
	 * Loads static data needed for this machine to function.  Does not load any instance data.
	 * @param file The config file as specified in the constructor.
	 */
	public void loadConfigData(PluginFile file) {
		this.timer = file.getInt(name + ".timer", 1);
		this.start_progress = file.getInt(name + ".start_progress");
	}

	/**
	 * Sets up the container of this tile for viewing.
	 */
	@SuppressWarnings("deprecation")
	public void setupContainer() {
		guiTex.setDurability((short) (start_progress + progress));
		ItemMeta meta = guiTex.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(" ");
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
		guiTex.setItemMeta(meta);
		inventory.setItemInternal(Slots.GUI_TEX_SLOT, guiTex);
	}

	/**
	 * Helper method to open this machine's inventory.
	 */
	public void openInventory(Player player) {
		setupContainer();
		player.openInventory(inventory);
	}

	/**
	 * Writes all instance data for this machine to disk.
	 */
	public abstract void write(PluginFile file);

	/**
	 * Reads all instance data for this machine from disk.
	 */
	public abstract void read(PluginFile file);

	/**
	 * Called when this machine has been broken in-world.
	 */
	public final void destroy() {
		dropItems();
		unload();
		if (inventory.getViewers() != null) inventory.getViewers().forEach(HumanEntity::closeInventory);
	}

	/**
	 * Spawn additional drops from this tile into the world.  Note you do not have to drop yourself into the world, only extra stored items.
	 */
	protected void dropItems() {
		Location dropLoc = location.clone().add(0.5, 0.5, 0.5);
		for (int i = 0; i < inventory.getSize(); i++) {
			if (i != Slots.GUI_TEX_SLOT && inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR) {
				location.getWorld().dropItem(dropLoc, inventory.getItem(i));
			}
		}
	}

	/**
	 * Special wrapped inventory class that allows for machine access from {@link InventoryClickEvent}
	 */
	public class NoteBlockInventory extends CraftInventoryCustom {

		public NoteBlockInventory(InventoryHolder owner, int size, String title) {
			super(owner, size, title);
		}

		@Override
		@Deprecated
		public void setItem(int index, ItemStack item) {
			if (isClickableSlot(index)) super.setItem(index, item);
			else Thread.dumpStack();
		}

		public void setItemInternal(int index, ItemStack item) {
			super.setItem(index, item);
		}

		@Override
		public Location getLocation() {
			return NoteTileEntity.this.location;
		}

		public NoteTileEntity getMachine() {
			return NoteTileEntity.this;
		}

	}

	/**
	 * @return The position of this tile.
	 */
	public WorldPos getPos() {
		return pos;
	}

	/**
	 * @return The position of this tile.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Unloads this machine from existance.  Should be accompanied by removal from a pos -> tile map.
	 */
	public void unload() {
		this.stop();
	}

	/**
	 * Called when a slot is shift clicked in this tile's inventory.
	 * Only called for valid spots as specified by {@link NoteTileEntity#isClickableSlot(int)}
	 */
	public void handleShiftClick(InventoryClickEvent e) {
		Inventory inv = e.getClickedInventory();
		if (inv == inventory) {
			e.setCancelled(true);
			return;
		}
		ItemStack clicked = e.getCurrentItem();
		if (isEmpty(clicked)) return;
		else {
			boolean hotbar = e.getSlot() >= 0 && e.getSlot() < 9;
			if (hotbar) attemptMerge(e.getClickedInventory(), clicked, 9, 36);
			else attemptMerge(e.getClickedInventory(), clicked, 0, 9);
			updateAndCancel(e);
		}
	}

	/**
	 * Called when a slot is clicked in this tile's inventory.
	 * Only called for valid spots as specified by {@link NoteTileEntity#isClickableSlot(int)}
	 */
	public void onSlotClick(InventoryClickEvent e) {
	}

	/**
	 * Checks if a given slot can be clicked on.
	 */
	public boolean isClickableSlot(int slot) {
		return false;
	}

	/**
	 * An attempt to recreate the methods of vanilla minecraft that merge item stacks on shift click.
	 */
	public static void attemptMerge(Inventory inv, ItemStack stack, int slotStart, int slotEnd) {
		for (int i = slotStart; i < slotEnd; i++) {
			ItemStack cur = inv.getItem(i);
			if (isEmpty(cur)) {
				inv.setItem(i, stack.clone());
				stack.setAmount(0);
				return;
			} else if (ItemData.isSimilar(cur, stack)) {
				int newCur = Math.min(cur.getMaxStackSize(), cur.getAmount() + stack.getAmount());
				int taken = newCur - cur.getAmount();
				cur.setAmount(cur.getAmount() + taken);
				stack.setAmount(stack.getAmount() - taken);
				if (isEmpty(stack)) return;
			}
		}
	}

	/**
	 * An attempt to recreate the methods of vanilla minecraft that merge item stacks on shift click.
	 */
	public static void attemptMergeReverse(Inventory inv, ItemStack stack, int slotStart, int slotEnd) {
		for (int i = slotStart; i > slotEnd; i--) {
			ItemStack cur = inv.getItem(i);
			if (isEmpty(cur)) {
				inv.setItem(i, stack.clone());
				stack.setAmount(0);
				return;
			} else if (ItemData.isSimilar(cur, stack)) {
				int newCur = Math.min(cur.getMaxStackSize(), cur.getAmount() + stack.getAmount());
				int taken = newCur - cur.getAmount();
				cur.setAmount(cur.getAmount() + taken);
				stack.setAmount(stack.getAmount() - taken);
				if (isEmpty(stack)) return;
			}
		}
	}

	/**
	 * An attempt to recreate the methods of vanilla minecraft that merge item stacks on shift click.
	 */
	public static void vanillaInvInsert(Inventory inv, ItemStack stack) {
		for (int i = 0; i < 4; i++) {
			if (isEmpty(stack)) return;
			attemptMergeReverse(inv, stack, 8 + 9 * i, -1 + 9 * i);
		}
	}

	/**
	 * Checks if the given stack represents nothing.
	 */
	public static boolean isEmpty(ItemStack stack) {
		return stack == null || stack.getAmount() <= 0 || stack.getType() == Material.AIR;
	}

	@SuppressWarnings("deprecation")
	public static void updateAndCancel(InventoryClickEvent e) {
		((Player) e.getWhoClicked()).updateInventory();
		e.setCancelled(true);
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj.getClass() == this.getClass() && ((NoteTileEntity) obj).pos.equals(this.pos);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getClass(), this.pos);
	}

	@Override
	public String toString() {
		return String.format("Machine: %s, Location: %s", this.getClass().getSimpleName(), this.pos.translated());
	}

	/**
	 * Attempts to extract an item from this tile.
	 * @return The successfully extracted itemstack, or {@link ItemData#EMPTY} if nothing is available.
	 */
	public ItemStack extractItem(int maxCount, boolean simulate) {
		for (int i : getOutputSlots()) {
			ItemStack s = this.inventory.getItem(i);
			if (!isEmpty(s) && canExtract(s)) {
				ItemStack clone = s.clone();
				clone.setAmount(Math.min(clone.getAmount(), maxCount));
				if (!simulate) {
					s.setAmount(s.getAmount() - clone.getAmount());
				}
				return clone;
			}
		}
		return ItemData.EMPTY;
	}

	/**
	 * Attempts to insert an item into this tile. This may not modifiy the passed itemstack.
	 * @param stack The stack to be inserted.
	 * @return The remaining stack, or {@link ItemData#EMPTY} if the entire stack was inserted.
	 */
	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		int max = Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
		for (int i : getInputSlots()) {
			if (isEmpty(stack)) return ItemData.EMPTY;
			ItemStack slot = this.inventory.getItem(i);
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
			} else if (isEmpty(slot)) {
				inventory.setItemInternal(i, stack.clone());
				return ItemData.EMPTY;
			}
		}
		return stack;
	}

	protected int[] getInputSlots() {
		return new int[0];
	}

	protected int[] getOutputSlots() {
		return new int[0];
	}

	protected boolean canExtract(ItemStack stack) {
		return true;
	}
}

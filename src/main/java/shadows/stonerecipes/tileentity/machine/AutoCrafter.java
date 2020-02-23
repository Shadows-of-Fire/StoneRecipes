package shadows.stonerecipes.tileentity.machine;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import net.minecraft.server.v1_15_R1.IInventory;
import net.minecraft.server.v1_15_R1.IRecipe;
import net.minecraft.server.v1_15_R1.InventoryCrafting;
import net.minecraft.server.v1_15_R1.ItemStack;
import net.minecraft.server.v1_15_R1.MinecraftKey;
import net.minecraft.server.v1_15_R1.MinecraftServer;
import net.minecraft.server.v1_15_R1.Recipes;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.WorldPos;

public class AutoCrafter extends PoweredMachine {

	private LocalInvCrafting cInv = new LocalInvCrafting();
	private IRecipe<InventoryCrafting> recipe = null;
	private final org.bukkit.inventory.ItemStack refresh = new org.bukkit.inventory.ItemStack(Material.DIAMOND_HOE);

	@SuppressWarnings("deprecation")
	public AutoCrafter(WorldPos pos) {
		super("autocrafter", "Auto Crafter", "config.yml", pos);
		this.start_progress = 70;
		this.updater = false;
		refresh.setDurability((short) 65);
		ItemMeta meta = refresh.getItemMeta();
		meta.setDisplayName(ChatColor.AQUA + "Refresh Current Recipe");
		meta.setUnbreakable(true);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
		refresh.setItemMeta(meta);
		guiTex.setDurability((short) start_progress);
		meta = guiTex.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(" ");
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
		guiTex.setItemMeta(meta);
	}

	@Override
	public void setupContainer() {
		this.inventory.setItemInternal(Slots.GUI_TEX_SLOT, guiTex);
		this.inventory.setItemInternal(Slots.AUTOCRAFTER_REFRESH, refresh);
	}

	@Override
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void read(PluginFile file) {
		super.read(file);
		if (file.contains(pos + ".recipe")) {
			String key = file.getString(pos + ".recipe").replace('|', ':');
			recipe = (IRecipe<InventoryCrafting>) MinecraftServer.getServer().getCraftingManager().recipes.get(Recipes.CRAFTING).get(new MinecraftKey(key));
		}
		if (file.isList(pos + ".inv")) {
			List<org.bukkit.inventory.ItemStack> content = (List<org.bukkit.inventory.ItemStack>) file.getList(pos + ".inv");
			for (int i = 0; i < 54; i++) {
				if (content.get(i) != null) {
					inventory.setItemInternal(i, content.get(i));
				}
			}
		} else StoneRecipes.INSTANCE.getLogger().info("Failed to read inventory for a " + name + " at " + pos.translated());
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".recipe", recipe == null ? null : recipe.getKey().toString().replace(':', '|'));
		file.set(pos + ".inv", Arrays.asList(inventory.getContents()));
	}

	@Override
	public NoteTileType<?> getType() {
		return NoteTypes.AUTO_CRAFTER;
	}

	@Override
	protected void timerTick() {
		if (this.getPower() >= powerCost && recipe != null && recipe.a(cInv, ((CraftWorld) this.location.getWorld()).getHandle())) {
			Int2IntMap m = new Int2IntOpenHashMap();

			for (int i = 0; i < 9; i++) {
				int slot = i < 3 ? i : i < 6 ? i - 3 + 9 : i - 6 + 18;
				org.bukkit.inventory.ItemStack s = this.inventory.getItem(slot);
				if (!isEmpty(s)) {
					boolean matched = false;
					for (int j = 9 * 4; j < this.inventory.getSize(); j++) {
						org.bukkit.inventory.ItemStack s2 = this.inventory.getItem(j);
						if (s2 != null && ItemData.isSimilar(s, s2)) {
							int used = m.get(j);
							if (used == s2.getAmount()) continue;
							m.put(j, used + 1);
							matched = true;
							break;
						}
					}
					if (!matched) return;
				}
			}

			ItemStack out = recipe.a(cInv);
			org.bukkit.inventory.ItemStack bOut = CraftItemStack.asBukkitCopy(out);
			org.bukkit.inventory.ItemStack current = this.inventory.getItem(Slots.AUTOCRAFTER_OUTPUT);
			if (current == null) {
				this.inventory.setItemInternal(Slots.AUTOCRAFTER_OUTPUT, bOut);
			} else if (ItemData.isSimilar(bOut, current) && bOut.getAmount() + current.getAmount() <= current.getMaxStackSize()) {
				current.setAmount(current.getAmount() + bOut.getAmount());
			} else return;

			m.forEach((a, b) -> {
				org.bukkit.inventory.ItemStack s = this.inventory.getItem(a);
				s.setAmount(s.getAmount() - b);
			});
			this.usePower(this.powerCost);
		}
	}

	@Override
	public void onSlotClick(InventoryClickEvent e) {
		if (e.getSlot() == Slots.AUTOCRAFTER_REFRESH) {
			this.refreshRecipe();
			StoneRecipes.debug("Recipe updated to be %s", recipe == null ? null : recipe.getKey());
			updateAndCancel(e);
		}
	}

	@Override
	public boolean isClickableSlot(int slot) {
		return slot < 3 || slot >= 9 && slot < 12 || slot >= 18 && slot < 21 || slot == Slots.AUTOCRAFTER_OUTPUT || slot >= 9 * 4 || slot == Slots.AUTOCRAFTER_REFRESH;
	}

	static int[] out = new int[] { Slots.AUTOCRAFTER_OUTPUT };
	static int[] in = new int[18];
	static {
		for (int i = 0; i < 18; i++) {
			in[i] = 9 * 4 + i;
		}
	}

	@Override
	protected int[] getOutputSlots() {
		return out;
	}

	@Override
	protected int[] getInputSlots() {
		return in;
	}

	@Override
	protected void dropItems() {
		Location dropLoc = location.clone().add(0.5, 0.5, 0.5);
		for (int i = 0; i < inventory.getSize(); i++) {
			if (i != Slots.GUI_TEX_SLOT && i != Slots.AUTOCRAFTER_REFRESH && inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR) {
				location.getWorld().dropItem(dropLoc, inventory.getItem(i));
			}
		}
	}

	@Override
	public void handleShiftClick(InventoryClickEvent e) {
		Inventory inv = e.getClickedInventory();
		if (inv == inventory && (!isClickableSlot(e.getSlot()) || e.getSlot() == Slots.AUTOCRAFTER_REFRESH)) {
			e.setCancelled(true);
			return;
		}
		org.bukkit.inventory.ItemStack clicked = e.getCurrentItem();
		if (isEmpty(clicked)) return;
		else {
			if (inv == inventory) {
				vanillaInvInsert(e.getView().getBottomInventory(), clicked);
			} else {
				boolean hotbar = e.getSlot() >= 0 && e.getSlot() < 9;
				attemptMerge(inventory, clicked, 9 * 4, inventory.getSize());
				if (!isEmpty(clicked)) {
					if (hotbar) attemptMerge(e.getClickedInventory(), clicked, 9, 36);
					else attemptMerge(e.getClickedInventory(), clicked, 0, 9);
				}
			}
			updateAndCancel(e);
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
	private void refreshRecipe() {
		if (recipe != null && recipe.a(cInv, ((CraftWorld) this.location.getWorld()).getHandle())) return;
		Collection<IRecipe<?>> recipes = MinecraftServer.getServer().getCraftingManager().recipes.get(Recipes.CRAFTING).values();
		for (IRecipe r : recipes) {
			if (r.a(cInv, ((CraftWorld) this.location.getWorld()).getHandle())) {
				recipe = r;
				return;
			}
		}
		recipe = null;
	}

	public ItemStack getInputItem(int i) {
		int slot = i < 3 ? i : i < 6 ? i - 3 + 9 : i - 6 + 18;
		org.bukkit.inventory.ItemStack s = this.inventory.getItem(slot);
		if (isEmpty(s)) return ItemStack.a;
		return CraftItemStack.asNMSCopy(s);
	}

	private class LocalInvCrafting extends InventoryCrafting {

		public LocalInvCrafting() {
			super(null, 3, 3);
		}

		@Override
		public int getMaxStackSize() {
			return 64;
		}

		@Override
		public void setMaxStackSize(int size) {
		}

		@Override
		public Location getLocation() {
			return AutoCrafter.this.location.clone();
		}

		@Override
		public IRecipe<? extends IInventory> getCurrentRecipe() {
			return AutoCrafter.this.recipe;
		}

		@Override
		public void setCurrentRecipe(@SuppressWarnings("rawtypes") IRecipe currentRecipe) {
		}

		@Override
		public int getSize() {
			return 9;
		}

		@Override
		public ItemStack getItem(int i) {
			return i >= this.getSize() ? ItemStack.a : AutoCrafter.this.getInputItem(i);
		}

	}

}

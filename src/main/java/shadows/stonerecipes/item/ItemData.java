package shadows.stonerecipes.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Powerable;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import joptsimple.internal.Strings;
import net.minecraft.server.v1_16_R2.MojangsonParser;
import net.minecraft.server.v1_16_R2.NBTTagCompound;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.item.CustomItem.ItemFactory;
import shadows.stonerecipes.listener.MoonHandler;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.util.CustomBlock;
import shadows.stonerecipes.util.InstrumentalNote;
import shadows.stonerecipes.util.Keys;
import shadows.stonerecipes.util.PluginFile;

/**
 * Handles the loading, storage, and accession of custom items.
 * Custom items are diamond hoes with special nbt and damage values.
 */
public class ItemData {

	public static final ItemStack EMPTY = new ItemStack(Material.AIR);

	protected final Map<String, ItemFactory> itemOverrides = new HashMap<>();
	protected final Map<String, CustomItem> items = new HashMap<>();
	protected final Map<CustomBlock, CustomItem> blockToItem = new HashMap<>();
	protected final PluginFile itemFile;

	public ItemData(StoneRecipes plugin) {
		itemFile = new PluginFile(plugin, "items.yml");
		itemOverrides.put("dimensional_key", DimKeyItem::new);
		itemOverrides.put("rocketship", RocketshipItem::new);
		itemOverrides.put("jetpack_controller", JetpackControllerItem::new);
		itemOverrides.put("jetpack", JetpackItem::new);
		itemOverrides.put("battery_pack", BatteryPackItem::new);
		this.loadData();
	}

	protected void loadData() {
		items.clear();
		loadItems();
	}

	@SuppressWarnings("deprecation")
	public void loadItems() {
		for (String key : itemFile.getKeys(false)) {
			ItemStack item = new ItemStack(Material.valueOf(itemFile.getString(key + ".material")));
			if (itemFile.contains(key + ".nbt")) {
				String nbt = itemFile.getString(key + ".nbt");
				try {
					NBTTagCompound tag = MojangsonParser.parse(nbt);
					net.minecraft.server.v1_16_R2.ItemStack stk = CraftItemStack.asNMSCopy(item);
					stk.setTag(tag);
					item = CraftItemStack.asBukkitCopy(stk);
				} catch (CommandSyntaxException e) {
					e.printStackTrace();
				}
			}
			ItemMeta meta = item.getItemMeta();
			if (itemFile.contains(key + ".name")) {
				meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemFile.getString(key + ".name")));
			}
			if (itemFile.contains(key + ".unbreakable")) {
				meta.setUnbreakable(itemFile.getBoolean(key + ".unbreakable"));
			}
			if (itemFile.contains(key + ".durability")) {
				((Damageable) meta).setDamage(itemFile.getInt(key + ".durability"));
			}
			if (itemFile.contains(key + ".lore")) {
				List<String> lore = new ArrayList<>();
				for (String line : itemFile.getStringList(key + ".lore")) {
					lore.add(ChatColor.translateAlternateColorCodes('&', line));
				}
				meta.setLore(lore);
			}
			if (itemFile.contains(key + ".enchants")) {
				for (String enchant : itemFile.getStringList(key + ".enchants")) {
					String type = enchant.split(",")[0];
					int level = Integer.parseInt(enchant.split(",")[1]);
					meta.addEnchant(Enchantment.getByName(type), level, true);
				}
			}
			if (itemFile.contains(key + ".flags")) {
				for (String flag : itemFile.getStringList(key + ".flags")) {
					meta.addItemFlags(ItemFlag.valueOf(flag));
				}
			}
			if (itemFile.contains(key + ".speed")) {
				meta.getPersistentDataContainer().set(Keys.SPEED, PersistentDataType.SHORT, (short) Math.max(1, itemFile.getInt(key + ".speed")));
			}
			CustomBlock block = null;
			if (itemFile.contains(key + ".block")) {
				Instrument instr = Instrument.valueOf(itemFile.getString(key + ".block.instrument"));
				int note = itemFile.getInt(key + ".block.note");
				block = new CustomBlock(Material.NOTE_BLOCK, new InstrumentalNote(instr, note).asBlockData());
			}
			if (itemFile.contains(key + ".ore")) {
				Material mat = Material.valueOf(itemFile.getString(key + ".ore.type"));
				Directional data = (Directional) mat.createBlockData();
				data.setFacing(BlockFace.valueOf(itemFile.getString(key + ".ore.face")));
				block = new CustomBlock(mat, data);
			}
			NamespacedKey sound = new NamespacedKey("minecraft", "block.stone.place");
			if (itemFile.contains(key + ".sound")) {
				String sKey = itemFile.getString(key + ".sound");
				if (!sKey.contains(":")) {
					sound = new NamespacedKey("minecraft", sKey);
				} else sound = new NamespacedKey(sKey.split(":")[0], sKey.split(":")[1]);
			}
			if (itemFile.contains(key + ".power")) {
				int def = itemFile.getInt(key + ".power.default");
				int max = itemFile.getInt(key + ".power.max");
				List<String> lore = meta.getLore();
				if (lore == null) lore = new ArrayList<>();
				lore.add(0, ChatColor.translateAlternateColorCodes('&', String.format("&r&aPower: %d/%d", def, max)));
				meta.setLore(lore);
				meta.getPersistentDataContainer().set(Keys.MAX_POWER, PersistentDataType.INTEGER, max);
				meta.getPersistentDataContainer().set(Keys.POWER, PersistentDataType.INTEGER, def);
			}
			if (itemFile.contains(key + ".oxygen")) {
				int def = itemFile.getInt(key + ".oxygen.default");
				int max = itemFile.getInt(key + ".oxygen.max");
				int cost = itemFile.getInt(key + ".oxygen.cost");
				List<String> lore = meta.getLore();
				if (lore == null) lore = new ArrayList<>();
				lore.add(0, ChatColor.translateAlternateColorCodes('&', String.format("&r&aOxygen: %d/%d", def, max)));
				meta.setLore(lore);
				meta.getPersistentDataContainer().set(Keys.MAX_OXYGEN, PersistentDataType.INTEGER, max);
				meta.getPersistentDataContainer().set(Keys.OXYGEN, PersistentDataType.INTEGER, def);
				meta.getPersistentDataContainer().set(Keys.OXYGEN_COST, PersistentDataType.INTEGER, cost);
			}
			if (itemFile.contains(key + ".drill")) {
				int x = itemFile.getInt(key + ".drill.x", 1);
				int y = itemFile.getInt(key + ".drill.y", 1);
				int xOffset = itemFile.getInt(key + ".drill.x_offset", 0);
				int yOffset = itemFile.getInt(key + ".drill.y_offset", 0);
				int cost = itemFile.getInt(key + ".drill.cost", 5);
				meta.getPersistentDataContainer().set(Keys.DRILL_X, PersistentDataType.INTEGER, x);
				meta.getPersistentDataContainer().set(Keys.DRILL_Y, PersistentDataType.INTEGER, y);
				meta.getPersistentDataContainer().set(Keys.DRILL_X_OFF, PersistentDataType.INTEGER, xOffset);
				meta.getPersistentDataContainer().set(Keys.DRILL_Y_OFF, PersistentDataType.INTEGER, yOffset);
				meta.getPersistentDataContainer().set(Keys.DRILL_COST, PersistentDataType.INTEGER, cost);
			}
			meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, key);
			item.setItemMeta(meta);
			CustomItem cItem = itemOverrides.getOrDefault(key, CustomItem::new).apply(key, item, block, sound);
			items.put(key, cItem);
			if (block != null) {
				blockToItem.put(block, cItem);
				StoneRecipes.debug("Loaded item %s with block form %s.", cItem.getName(), block.getData().getAsString());
			} else StoneRecipes.debug("Loaded item %s.", cItem.getName());
		}
	}

	@SuppressWarnings("deprecation")
	public static boolean isSimilar(ItemStack one, ItemStack two) {

		if (one == null && two != null || two == null && one != null) return false;

		if (ItemData.isEmpty(one) != ItemData.isEmpty(two)) return false;

		if (!one.getType().equals(two.getType())) { return false; }

		if (one.getDurability() != two.getDurability()) { return false; }

		if (!one.hasItemMeta() && !two.hasItemMeta()) { return true; }

		if (!one.hasItemMeta() && two.hasItemMeta()) {
			if (!two.getItemMeta().hasDisplayName()) { return true; }
		}

		if (!two.hasItemMeta() && one.hasItemMeta()) {
			if (!one.getItemMeta().hasDisplayName()) { return true; }
		}

		if (!one.getItemMeta().hasDisplayName() && !two.getItemMeta().hasDisplayName()) { return true; }

		if (!one.getItemMeta().hasDisplayName() && two.getItemMeta().hasDisplayName() || !two.getItemMeta().hasDisplayName() && one.getItemMeta().hasDisplayName()) {
			return false;
		}

		if (one.getItemMeta().getDisplayName().equals(two.getItemMeta().getDisplayName())) { return true; }

		return false;
	}

	/**
	 * Get a custom item by it's name.
	 * @param itemName The name of the item as specified in items.yml
	 * @return An ItemStack representing that item.
	 */
	@Nullable
	public ItemStack getItem(String itemName) {
		ItemStack item = null;
		if (items.containsKey(itemName)) {
			item = items.get(itemName).getStack().clone();
		}
		if (item == null) StoneRecipes.debug("Invalid request for unloaded item %s", itemName);
		return item;
	}

	/**
	 * Gets an item from our custom items, or defaults to a Material type name.
	 * Does some special post-processing, like setting power levels to 0.
	 */
	public ItemStack getItemForRecipe(String itemName) {
		ItemStack item = null;
		if (items.containsKey(itemName)) {
			item = items.get(itemName).getStack().clone();
		}
		if (item == null) item = new ItemStack(Material.valueOf(itemName));
		if (Charger.getPower(item) > 0) Charger.setPower(item, 0);
		if (MoonHandler.getOxygen(item) > 0) MoonHandler.setOxygen(item, 0);
		return item;
	}

	public Collection<String> getNames() {
		return items.keySet();
	}

	@Nullable
	public CustomBlock getBlock(String itemId) {
		return items.containsKey(itemId) ? items.get(itemId).getBlock() : null;
	}

	public ItemStack getItem(Block block) {
		return getItem(new CustomBlock(block.getType(), block.getBlockData()));
	}

	public ItemStack getItem(CustomBlock block) {
		if (block.getBlock() == Material.NOTE_BLOCK) ((Powerable) block.getData()).setPowered(false);
		if (!blockToItem.containsKey(block)) {
			StoneRecipes.debug("Attempted to access item form for a block without an item form, " + block.getBlock());
			return new ItemStack(Material.AIR);
		}
		return blockToItem.get(block).getStack().clone();
	}

	public NamespacedKey getSound(CustomBlock block) {
		return blockToItem.get(block).getSound();
	}

	public NamespacedKey getSound(String itemId) {
		return items.get(itemId).getSound();
	}

	public CustomItem getItemHolder(String id) {
		return items.get(id);
	}

	/**
	 * Checks if the given stack represents nothing.
	 */
	public static boolean isEmpty(ItemStack stack) {
		return stack == null || stack.getAmount() <= 0 || stack.getType() == Material.AIR;
	}

	public static String getItemId(ItemStack stack) {
		if (ItemData.isEmpty(stack) || !stack.hasItemMeta()) return "";
		String id = stack.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
		if (Strings.isNullOrEmpty(id)) return "";
		return id;
	}

	public static CustomItem getItem(ItemStack stack) {
		return StoneRecipes.INSTANCE.getItems().getItemHolder(getItemId(stack));
	}

}

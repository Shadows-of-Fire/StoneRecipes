package shadows.stonerecipes.storage;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.helper.gson.GsonProvider;
import me.lucko.helper.gson.GsonSerializable;
import me.lucko.helper.gson.JsonBuilder;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.serialize.Serializers;
import shadows.stonerecipes.listener.MassStorageHandler;

@SuppressWarnings("deprecation")
public class Storage implements GsonSerializable {

	public static final ItemStack NEXT_PAGE = ItemStackBuilder.of(new ItemStack(Material.DIAMOND_HOE, 1, (short) 2)).breakable(false).name("Next Page").build();
	public static final ItemStack PREV_PAGE = ItemStackBuilder.of(new ItemStack(Material.DIAMOND_HOE, 1, (short) 3)).breakable(false).name("Previous Page").build();
	public static final ItemStack CANCEL_SEARCH = ItemStackBuilder.of(new ItemStack(Material.DIAMOND_HOE, 1, (short) 5)).breakable(false).name("Cancel Search").build();
	public static final ItemStack SEARCH = ItemStackBuilder.of(new ItemStack(Material.DIAMOND_HOE, 1, (short) 4)).breakable(false).name("Search").build();
	public static final ItemStack FILL = ItemStackBuilder.of(new ItemStack(Material.DIAMOND_HOE, 1, (short) 47)).breakable(false).name("&a").build();

	protected UUID owner;
	protected int pages;
	protected List<ItemStack> items;
	protected int currentlyOpenedPage;

	public Storage(UUID owner, int pages, List<ItemStack> allItems) {
		this.owner = owner;
		this.pages = pages;
		this.items = allItems.stream().filter(itemStack -> itemStack != null).collect(Collectors.toList());
	}

	public Storage openPage(Player p, int page) {

		if (page <= 0 || page > pages) { return this; }

		Inventory inv = Bukkit.createInventory(null, 54, "Mass Storage Device");

		if (this.pages > 1) {
			inv.setItem(45, PREV_PAGE);
			inv.setItem(53, NEXT_PAGE);
		} else {
			inv.setItem(45, FILL);
			inv.setItem(53, FILL);
		}

		inv.setItem(46, FILL);
		inv.setItem(47, FILL);
		inv.setItem(48, FILL);
		inv.setItem(49, SEARCH);
		inv.setItem(50, FILL);
		inv.setItem(51, FILL);
		inv.setItem(52, FILL);

		int invIndex = 0;
		for (int i = 0 + (page - 1) * 45; i < 45 * page; i++) {
			try {
				ItemStack item = this.items.get(i);
				if (item == null) {
					continue;
				}
				inv.setItem(invIndex, item);
				invIndex += 1;
			} catch (Exception e) {
				break;
			}
		}

		p.openInventory(inv);

		this.currentlyOpenedPage = page;

		return this;
	}

	public File getFile() {
		return new File(MassStorageHandler.USERS_DIR, this.owner.toString() + ".json");
	}

	@Nonnull
	@Override
	public JsonElement serialize() {
		return JsonBuilder.object().addIfAbsent("owner", this.owner.toString()).addIfAbsent("pages", pages).addIfAbsent("items", Serializers.serializeItemstacks(getItemsArray())).build();
	}

	public static Storage load(FileReader reader) {
		JsonObject obj = GsonProvider.readObject(reader);

		UUID owner = UUID.fromString(obj.get("owner").getAsString());
		int pages = obj.get("pages").getAsInt();
		ItemStack[] items = Serializers.deserializeItemstacks(obj.get("items"));

		return new Storage(owner, pages, getItemsArrayList(items));
	}

	private ItemStack[] getItemsArray() {
		ItemStack[] array = new ItemStack[this.items.size()];
		for (int i = 0; i < this.items.size(); i++) {
			array[i] = this.items.get(i);
		}
		return array;
	}

	public static ArrayList<ItemStack> getItemsArrayList(ItemStack[] array) {
		ArrayList<ItemStack> list = new ArrayList<>();
		for (int i = 0; i < array.length; i++) {
			if (array[i] == null) {
				continue;
			}
			list.add(array[i]);
		}
		return list;
	}

	public void updateItems(ItemStack[] contents) {

		for (int i = 0; i < 45; i++) {
			try {
				this.items.set(this.mapIndex(i), contents[i]);
			} catch (IndexOutOfBoundsException e) {
				this.items.add(contents[i]);
			}
		}
	}

	private int mapIndex(int index) {
		return index + (currentlyOpenedPage - 1) * 45;
	}

	public void query(String query) {
		this.items = this.items.stream().filter(itemStack -> itemStack != null).collect(Collectors.toList());
		this.items.sort(Comparator.comparing(itemStack -> itemStack.getType().name().toLowerCase().contains(query.toLowerCase())));
		Collections.reverse(this.items);
	}

	public int getCurrentPage() {
		return currentlyOpenedPage;
	}

	public void setPages(int pages) {
		this.pages = pages;
	}
}

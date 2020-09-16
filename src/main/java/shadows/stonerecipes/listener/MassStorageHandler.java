package shadows.stonerecipes.listener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.lucko.helper.Schedulers;
import me.lucko.helper.gson.GsonProvider;
import me.lucko.helper.signprompt.SignPromptFactory;
import me.lucko.helper.utils.Players;
import net.minecraft.server.v1_16_R2.ItemStack;
import net.minecraft.server.v1_16_R2.NBTTagCompound;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.storage.CustomPacketSignPromptFactory;
import shadows.stonerecipes.storage.Storage;
import shadows.stonerecipes.util.ItemAdapter;
import shadows.stonerecipes.util.ItemAdapter.NBTAdapter;

public class MassStorageHandler implements Listener {

	public static final File USERS_DIR = new File(StoneRecipes.INSTANCE.getDataFolder(), "mass_storage/users");
	public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ItemStack.class, ItemAdapter.INSTANCE).registerTypeAdapter(NBTTagCompound.class, NBTAdapter.INSTANCE).create();

	private final HashMap<UUID, Storage> storageCache = new HashMap<>();
	private final HashMap<UUID, Storage> openInventories = new HashMap<>();

	public MassStorageHandler() {
		if (!USERS_DIR.exists()) {
			USERS_DIR.mkdirs();
		}
		Players.all().forEach(p -> loadPlayerStorage(p));
	}

	public void saveAll() {
		for (Storage s : this.storageCache.values()) {

			if (!s.getFile().exists()) {
				try {
					s.getFile().createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try (FileWriter fw = new FileWriter(s.getFile())) {
				GsonProvider.writeObjectPretty(fw, s.serialize().getAsJsonObject());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		this.loadPlayerStorage(e.getPlayer());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		this.savePlayerStorage(e.getPlayer());
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (this.openInventories.containsKey(e.getWhoClicked().getUniqueId()) && this.openInventories.get(e.getWhoClicked().getUniqueId()) != null && e.getCurrentItem() != null) {
			Storage storage = this.openInventories.get(e.getWhoClicked().getUniqueId());

			if (e.getCurrentItem().isSimilar(Storage.FILL) || e.getCurrentItem().isSimilar(Storage.NEXT_PAGE) || e.getCurrentItem().isSimilar(Storage.PREV_PAGE) || e.getCurrentItem().isSimilar(Storage.SEARCH)) {
				e.setCancelled(true);
			}
			if (e.getCurrentItem().isSimilar(Storage.PREV_PAGE)) {
				this.openStoragePage((Player) e.getWhoClicked(), storage.getCurrentPage() - 1);
			} else if (e.getCurrentItem().isSimilar(Storage.NEXT_PAGE)) {
				this.openStoragePage((Player) e.getWhoClicked(), storage.getCurrentPage() + 1);
			} else if (e.getCurrentItem().isSimilar(Storage.SEARCH)) {
				e.getWhoClicked().closeInventory();
				new CustomPacketSignPromptFactory().openPrompt((Player) e.getWhoClicked(), new ArrayList<>(), lines -> {

					if (lines.get(0).isEmpty()) { return SignPromptFactory.Response.TRY_AGAIN; }

					Schedulers.sync().runLater(() -> {
						storage.query(lines.get(0));
						this.openStoragePage((Player) e.getWhoClicked(), 1);
					}, 20);

					return SignPromptFactory.Response.ACCEPTED;
				});
			}
		}
	}

	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if (this.openInventories.containsKey(e.getPlayer().getUniqueId())) {
			Storage storage = this.openInventories.get(e.getPlayer().getUniqueId());
			storage.updateItems(e.getInventory().getContents());

			this.openInventories.remove(e.getPlayer().getUniqueId());
		}
	}

	private void savePlayerStorage(Player player) {
		Schedulers.async().run(() -> {
			Storage s = this.storageCache.get(player.getUniqueId());

			if (s == null) { return; }

			if (!s.getFile().exists()) {
				try {
					s.getFile().createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try (FileWriter fw = new FileWriter(s.getFile())) {
				GsonProvider.writeObjectPretty(fw, s.serialize().getAsJsonObject());
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private void loadPlayerStorage(Player player) {
		Schedulers.async().run(() -> {
			try (FileReader reader = new FileReader(new File(USERS_DIR, player.getUniqueId().toString() + ".json"))) {
				this.storageCache.put(player.getUniqueId(), Storage.load(reader));
			} catch (Exception e) {
				//User does not have storage, create one
				this.storageCache.put(player.getUniqueId(), new Storage(player.getUniqueId(), 1, new ArrayList<>(45)));
				if (!(e instanceof FileNotFoundException)) e.printStackTrace();
			}
		});
	}

	public void openStoragePage(Player p, int page) {
		Storage storage = this.storageCache.get(p.getUniqueId());

		if (storage == null) {
			StoneRecipes.debug("Failed to open storage page for player " + p.getName() + ", as no storage instance existed.");
			return;
		}

		this.openInventories.put(p.getUniqueId(), storage.openPage(p, page));
	}

	public void setStorageCapacity(Player sender, int capacity) {
		Storage storage = this.storageCache.get(sender.getUniqueId());

		if (storage == null) {
			StoneRecipes.debug("Failed to change storage capacity for player " + sender.getName() + ", as no storage instance existed.");
			return;
		}

		storage.setPages(capacity);
	}

}

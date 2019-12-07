package shadows.stonerecipes.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockClickedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockRemovedEvent;
import shadows.stonerecipes.tileentity.machine.ItemTeleporter;
import shadows.stonerecipes.tileentity.machine.PlayerTeleporter;
import shadows.stonerecipes.tileentity.machine.PoweredMachine;
import shadows.stonerecipes.util.ITeleporter;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

/**
 * Handler for item and player teleportation.
 */
public class TeleportHandler implements Listener {

	protected final StoneRecipes plugin;
	protected final PluginFile playerData;
	protected final PluginFile itemData;

	/**
	 * All loaded player teleporters.
	 */
	protected final Map<WorldPos, PlayerTeleporter> playerTeleporters = new HashMap<>();

	/**
	 * All loaded item teleporters.
	 */
	protected final Map<WorldPos, ItemTeleporter> itemTeleporters = new HashMap<>();

	/**
	 * Map of actively linking player teleporters.  This is a map of linking players -> source teleporter.
	 */
	protected final Map<Player, WorldPos> playerLinks = new WeakHashMap<>();

	/**
	 * Map of actively linking item teleporters.  This is a map of linking players -> source teleporter.
	 */
	protected final Map<Player, WorldPos> itemLinks = new WeakHashMap<>();

	public TeleportHandler(StoneRecipes plugin) {
		this.plugin = plugin;
		playerData = new PluginFile(this.plugin, "data/playerTeleporters.yml");
		itemData = new PluginFile(this.plugin, "data/itemTeleporters.yml");
	}

	/**
	 * Tries to teleport a player when they start sneaking.
	 */
	@EventHandler
	public void onPlayerTryTeleport(PlayerToggleSneakEvent e) {
		if (e.isSneaking()) {
			Location loc = e.getPlayer().getLocation().subtract(0, 1, 0);
			WorldPos pos = new WorldPos(loc);
			if (playerTeleporters.containsKey(pos)) {
				playerTeleporters.get(pos).teleport(e.getPlayer());
			}
		}
	}

	/**
	 * Attempts to link two teleporters.  The source teleporter is determined from the player -> source link maps.
	 * @param teleporter The target teleporter that will be linked to.
	 * @param linker The player doing the linking.
	 * @param pos The position of the target teleporter.
	 */
	private void attemptLink(ITeleporter teleporter, Player linker, WorldPos pos) {
		Map<Player, WorldPos> curLinkers = teleporter.isPlayerTeleporter() ? playerLinks : itemLinks;
		WorldPos sourcePos = curLinkers.get(linker);
		if (sourcePos.equals(pos)) {
			linker.sendMessage(ChatColor.GREEN + "You cannot link a teleporter to itself.");
			return;
		}
		teleporter.setLink(curLinkers.get(linker));
		ITeleporter source = teleporter.isPlayerTeleporter() ? hotloadPlayerT(sourcePos) : hotloadItemT(sourcePos);
		if (source != null) {
			source.setLink(pos);
			linker.sendMessage(ChatColor.GREEN + "The teleporters at " + pos.translated() + " and " + sourcePos.translated() + " are now linked!");
		} else linker.sendMessage(ChatColor.RED + "The teleporter at " + sourcePos.translated() + " no longer exists!");
		curLinkers.remove(linker);
	}

	/**
	 * Handles the following teleport related interaction activities:
	 * Teleporter Linking
	 * Teleporter GUI Opening
	 * Placing Teleporters into the world.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onNotePlace(NoteBlockPlacedEvent e) {
		String type = e.getItemId();
		if (type.equals("player_teleporter")) {
			placePlayerT(new WorldPos(e.getBlock().getLocation()));
		} else if (type.equals("item_teleporter")) {
			placeItemT(new WorldPos(e.getBlock().getLocation()));
		}
	}

	/**
	 * Handles the following teleport related interaction activities:
	 * Teleporter Linking
	 * Teleporter GUI Opening
	 * Placing Teleporters into the world.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onNoteClick(NoteBlockClickedEvent e) {
		WorldPos pos = new WorldPos(e.getBlock().getLocation());
		if (playerTeleporters.containsKey(pos)) {
			PlayerTeleporter teleporter = playerTeleporters.get(pos);
			if (e.getClicker().isSneaking()) {
				if (playerLinks.containsKey(e.getClicker())) {
					attemptLink(teleporter, e.getClicker(), pos);
				} else {
					playerLinks.put(e.getClicker(), pos);
					if (!teleporter.getLink().equals(WorldPos.INVALID)) {
						PlayerTeleporter target = hotloadPlayerT(teleporter.getLink());
						if (target != null) target.setLink(WorldPos.INVALID);
						teleporter.setLink(WorldPos.INVALID);
					}
					e.getClicker().sendMessage(ChatColor.GREEN + "Shift-right click another Player Teleporter to link!");
				}
			} else teleporter.openInventory(e.getClicker());
			e.setSuccess();
		} else if (itemTeleporters.containsKey(pos)) {
			ItemTeleporter teleporter = itemTeleporters.get(pos);
			if (e.getClicker().isSneaking()) {
				if (itemLinks.containsKey(e.getClicker())) {
					attemptLink(teleporter, e.getClicker(), pos);
				} else {
					itemLinks.put(e.getClicker(), pos);
					if (!teleporter.getLink().equals(WorldPos.INVALID)) {
						ItemTeleporter target = hotloadItemT(teleporter.getLink());
						if (target != null) target.setLink(WorldPos.INVALID);
						teleporter.setLink(WorldPos.INVALID);
					}
					e.getClicker().sendMessage(ChatColor.GREEN + "Shift-right click another Item Teleporter to link!");
				}
			} else teleporter.openInventory(e.getClicker());
			e.setSuccess();
		}
	}

	@EventHandler
	public void onPlayerDestroyTeleporter(NoteBlockRemovedEvent e) {
		WorldPos pos = new WorldPos(e.getState().getLocation());
		if (playerTeleporters.containsKey(pos)) {
			removePlayerT(pos);
		} else if (itemTeleporters.containsKey(pos)) {
			removeItemT(pos);
		}
	}

	/**
	 * Places a player teleporter at the given pos.
	 */
	public void placePlayerT(WorldPos pos) {
		PlayerTeleporter tele = new PlayerTeleporter(pos);
		tele.start();
		playerTeleporters.put(pos, tele);
	}

	/**
	 * Places an item teleporter at the given pos.
	 */
	public void placeItemT(WorldPos pos) {
		ItemTeleporter tele = new ItemTeleporter(pos);
		tele.start();
		itemTeleporters.put(pos, tele);
	}

	/**
	 * Attempts to load a player teleporter for a given position.
	 * Does nothing if already loaded.
	 */
	private void loadPlayerT(WorldPos pos) {
		if (playerTeleporters.containsKey(pos)) return;
		PlayerTeleporter tele = new PlayerTeleporter(pos);
		MachineUtils.loadMachine(tele, playerData);
		playerTeleporters.put(pos, tele);
	}

	/**
	 * Attempts to load an item teleporter for a given position.
	 * Does nothing if already loaded.
	 */
	private void loadItemT(WorldPos pos) {
		if (itemTeleporters.containsKey(pos)) return;
		ItemTeleporter tele = new ItemTeleporter(pos);
		MachineUtils.loadMachine(tele, itemData);
		itemTeleporters.put(pos, tele);
	}

	/**
	 * Attempts to load a player teleporter for a given position.
	 * Does nothing if already loaded OR this location has no teleporter on disk.
	 */
	@Nullable
	public PlayerTeleporter hotloadPlayerT(WorldPos pos) {
		if (playerTeleporters.containsKey(pos)) return playerTeleporters.get(pos);
		if (!playerData.contains(pos.toString())) return null;
		PlayerTeleporter tele = new PlayerTeleporter(pos);
		MachineUtils.loadMachine(tele, playerData);
		playerTeleporters.put(pos, tele);
		return tele;
	}

	/**
	 * Attempts to load an item teleporter for a given position.
	 * Does nothing if already loaded OR this location has no teleporter on disk.
	 */
	@Nullable
	public ItemTeleporter hotloadItemT(WorldPos pos) {
		if (itemTeleporters.containsKey(pos)) return itemTeleporters.get(pos);
		if (!itemData.contains(pos.toString())) return null;
		ItemTeleporter tele = new ItemTeleporter(pos);
		MachineUtils.loadMachine(tele, itemData);
		itemTeleporters.put(pos, tele);
		return tele;
	}

	public void removePlayerT(WorldPos pos) {
		PlayerTeleporter removed = playerTeleporters.remove(pos);
		if (removed != null) {
			if (!removed.getLink().equals(WorldPos.INVALID)) {
				if (playerTeleporters.containsKey(removed.getLink())) {
					playerTeleporters.get(removed.getLink()).setLink(WorldPos.INVALID);
				} else if (playerData.contains(removed.getLink().toString())) {
					playerData.set(removed.getLink() + ".link", WorldPos.INVALID.toString());
				}
			}
			removed.destroy();
			playerData.set(pos.toString(), null);
			playerData.save();
		} else StoneRecipes.debug("Attempted to remove a player teleporter where one was not present at %s", pos);

	}

	public void removeItemT(WorldPos pos) {
		ItemTeleporter removed = itemTeleporters.remove(pos);
		if (removed != null) {
			if (!removed.getLink().equals(WorldPos.INVALID)) {
				if (itemTeleporters.containsKey(removed.getLink())) {
					itemTeleporters.get(removed.getLink()).setLink(WorldPos.INVALID);
				} else {
					itemData.set(removed.getLink() + ".link", WorldPos.INVALID.toString());
				}
			}
			removed.destroy();
			itemData.set(pos.toString(), null);
			itemData.save();
		} else StoneRecipes.debug("Attempted to remove an item teleporter where one was not present at %s", pos);
	}

	/**
	 * Loads the teleporters for a given chunk.
	 * @param world The chunk teleporters will be loaded for.
	 */
	public void load(Chunk chunk) {
		for (String s : playerData.getKeys(false)) {
			WorldPos pos = new WorldPos(s);
			if (pos.isInside(chunk)) {
				if (!pos.toLocation().getBlock().getType().equals(Material.NOTE_BLOCK)) {
					playerData.set(s, null);
					continue;
				}
				loadPlayerT(pos);
			}
		}
		for (String s : itemData.getKeys(false)) {
			WorldPos pos = new WorldPos(s);
			if (pos.isInside(chunk)) {
				if (!pos.toLocation().getBlock().getType().equals(Material.NOTE_BLOCK)) {
					itemData.set(s, null);
					continue;
				}
				loadItemT(pos);
			}
		}
	}

	/**
	 * Serializes all Machines and Generators in a chunk to disk.
	 * @param chunk The chunk to save things for.
	 */
	public void save(Chunk chunk) {
		for (WorldPos pos : playerTeleporters.keySet()) {
			if (pos.isInside(chunk)) {
				MachineUtils.saveMachine(playerTeleporters.get(pos), playerData);
			}
		}
		playerTeleporters.keySet().removeIf(pos -> pos.isInside(chunk));
		playerData.save();
		for (WorldPos pos : itemTeleporters.keySet()) {
			if (pos.isInside(chunk)) {
				MachineUtils.saveMachine(itemTeleporters.get(pos), itemData);
			}
		}
		itemTeleporters.keySet().removeIf(pos -> pos.isInside(chunk));
		itemData.save();
	}

	public Collection<? extends PoweredMachine> getTeleporters() {
		Set<PoweredMachine> set = new HashSet<>();
		set.addAll(itemTeleporters.values());
		set.addAll(playerTeleporters.values());
		return set;
	}

}

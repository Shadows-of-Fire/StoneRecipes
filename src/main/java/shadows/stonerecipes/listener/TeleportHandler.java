package shadows.stonerecipes.listener;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockClickedEvent;
import shadows.stonerecipes.listener.DataHandler.Maps;
import shadows.stonerecipes.tileentity.machine.ItemTeleporter;
import shadows.stonerecipes.tileentity.machine.PlayerTeleporter;
import shadows.stonerecipes.util.WorldPos;

/**
 * Handler for item and player teleportation.
 */
public class TeleportHandler implements Listener {

	/**
	 * Map of actively linking player teleporters.  This is a map of linking players -> source teleporter.
	 */
	protected final Map<Player, WorldPos> playerLinks = new WeakHashMap<>();

	/**
	 * Map of actively linking item teleporters.  This is a map of linking players -> source teleporter.
	 */
	protected final Map<Player, WorldPos> itemLinks = new WeakHashMap<>();

	/**
	 * Tries to teleport a player when they start sneaking.
	 */
	@EventHandler
	public void onPlayerTryTeleport(PlayerToggleSneakEvent e) {
		if (e.isSneaking()) {
			Location loc = e.getPlayer().getLocation().subtract(0, 1, 0);
			WorldPos pos = new WorldPos(loc);
			if (Maps.PLAYER_TELEPORTERS.contains(pos)) {
				Maps.PLAYER_TELEPORTERS.get(pos).teleport(e.getPlayer());
			}
		}
	}

	/**
	 * Handles teleporter linking.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onNoteClick(NoteBlockClickedEvent e) {
		WorldPos pos = new WorldPos(e.getBlock().getLocation());
		if (Maps.PLAYER_TELEPORTERS.contains(pos)) {
			PlayerTeleporter teleporter = Maps.PLAYER_TELEPORTERS.get(pos);
			if (e.getClicker().isSneaking()) {
				if (playerLinks.containsKey(e.getClicker())) {
					attemptLink(teleporter, e.getClicker(), pos);
					e.setSuccess();
				} else {
					playerLinks.put(e.getClicker(), pos);
					if (!teleporter.getLink().equals(WorldPos.INVALID)) {
						PlayerTeleporter target = hotloadPlayerT(teleporter.getLink());
						if (target != null) target.setLink(WorldPos.INVALID);
						teleporter.setLink(WorldPos.INVALID);
					}
					e.getClicker().sendMessage(ChatColor.GREEN + "Shift-right click another Player Teleporter to link!");
					e.setSuccess();
				}
			}
		} else if (Maps.ITEM_TELEPORTERS.contains(pos)) {
			if (e.getClicker().isSneaking()) {
				if (itemLinks.containsKey(e.getClicker())) {
					WorldPos sourcePos = itemLinks.get(e.getClicker());
					if (sourcePos.equals(pos)) {
						e.getClicker().sendMessage(ChatColor.GREEN + "You cannot link a teleporter to itself.");
						e.setSuccess();
						return;
					}
					ItemTeleporter source = hotloadItemT(sourcePos);
					if (source != null) {
						source.setDestination(pos);
						e.getClicker().sendMessage(ChatColor.GREEN + "The teleporter at " + sourcePos.translated() + " is now targetting " + pos.translated() + ".");
					} else e.getClicker().sendMessage(ChatColor.RED + "The teleporter at " + sourcePos.translated() + " no longer exists!");
					itemLinks.remove(e.getClicker());
					e.setSuccess();
				} else {
					itemLinks.put(e.getClicker(), pos);
					e.getClicker().sendMessage(ChatColor.GREEN + "Shift-right click another Item Teleporter to target!");
					e.setSuccess();
				}
			}
		}
	}

	/**
	 * Attempts to link two teleporters.  The source teleporter is determined from the player -> source link maps.
	 * @param teleporter The target teleporter that will be linked to.
	 * @param linker The player doing the linking.
	 * @param pos The position of the target teleporter.
	 */
	private void attemptLink(PlayerTeleporter teleporter, Player linker, WorldPos pos) {
		Map<Player, WorldPos> curLinkers = playerLinks;
		WorldPos sourcePos = curLinkers.get(linker);
		if (sourcePos.equals(pos)) {
			linker.sendMessage(ChatColor.GREEN + "You cannot link a teleporter to itself.");
			return;
		}
		teleporter.setLink(curLinkers.get(linker));
		PlayerTeleporter source = hotloadPlayerT(sourcePos);
		if (source != null) {
			source.setLink(pos);
			linker.sendMessage(ChatColor.GREEN + "The teleporters at " + pos.translated() + " and " + sourcePos.translated() + " are now linked!");
		} else linker.sendMessage(ChatColor.RED + "The teleporter at " + sourcePos.translated() + " no longer exists!");
		curLinkers.remove(linker);
	}

	/**
	 * Attempts to load a player teleporter for a given position.
	 * Does nothing if already loaded.
	 * If the teleporter isn't loaded, the chunk will be loaded, and it will be rechecked if the tile is present.
	 */
	@Nullable
	public PlayerTeleporter hotloadPlayerT(WorldPos pos) {
		if (Maps.PLAYER_TELEPORTERS.contains(pos)) return Maps.PLAYER_TELEPORTERS.get(pos);
		StoneRecipes.INSTANCE.getServer().getWorld(pos.getDim()).getChunkAt(pos.toChunkCoords().toLocation());
		return Maps.PLAYER_TELEPORTERS.get(pos);
	}

	/**
	 * Attempts to load an item teleporter for a given position.
	 * Does nothing if already loaded.
	 * If the teleporter isn't loaded, the chunk will be loaded, and it will be rechecked if the tile is present.
	 */
	@Nullable
	public ItemTeleporter hotloadItemT(WorldPos pos) {
		if (Maps.ITEM_TELEPORTERS.contains(pos)) return Maps.ITEM_TELEPORTERS.get(pos);
		StoneRecipes.INSTANCE.getServer().getWorld(pos.getDim()).getChunkAt(pos.toChunkCoords().toLocation());
		return Maps.ITEM_TELEPORTERS.get(pos);
	}

}

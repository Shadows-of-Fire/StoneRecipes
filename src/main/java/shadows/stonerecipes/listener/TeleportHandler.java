package shadows.stonerecipes.listener;

import java.util.Map;
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
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.tileentity.machine.ItemTeleporter;
import shadows.stonerecipes.tileentity.machine.PlayerTeleporter;
import shadows.stonerecipes.util.WorldPos;

/**
 * Handler for item and player teleportation.
 * Handles the actual teleportation of players as that information is not relayed back to the teleporters, and linking.
 */
public class TeleportHandler implements Listener {

	/**
	 * Map of actively linking player teleporters.  This is a map of linking players -> source teleporter.
	 */
	public static final Map<Player, WorldPos> PLAYER_LINKS = new WeakHashMap<>();

	/**
	 * Map of actively linking item teleporters.  This is a map of linking players -> source teleporter.
	 */
	public static final Map<Player, WorldPos> ITEM_LINKS = new WeakHashMap<>();

	/**
	 * Tries to teleport a player when they start sneaking.
	 */
	@EventHandler
	public void onPlayerTryTeleport(PlayerToggleSneakEvent e) {
		if (e.isSneaking()) {
			Location loc = e.getPlayer().getLocation().subtract(0, 1, 0);
			WorldPos pos = new WorldPos(loc);
			if (NoteTypes.PLAYER_TELEPORTER.getMap().contains(pos)) {
				NoteTypes.PLAYER_TELEPORTER.getMap().get(pos).teleport(e.getPlayer());
			}
		}
	}

	/**
	 * Attempts to link two teleporters.  The source teleporter is determined from the player -> source link maps.
	 * @param teleporter The target teleporter that will be linked to.
	 * @param linker The player doing the linking.
	 * @param pos The position of the target teleporter.
	 */
	public static void attemptLink(PlayerTeleporter teleporter, Player linker, WorldPos pos) {
		Map<Player, WorldPos> curLinkers = PLAYER_LINKS;
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
	public static PlayerTeleporter hotloadPlayerT(WorldPos pos) {
		if (NoteTypes.PLAYER_TELEPORTER.getMap().contains(pos)) return NoteTypes.PLAYER_TELEPORTER.getMap().get(pos);
		Chunk chunk = StoneRecipes.INSTANCE.getServer().getWorld(pos.getDim()).getChunkAt(pos.toChunkCoords().toLocation());
		if (chunk.getBlock(pos.x >> 4, pos.y, pos.z >> 4).getType() != Material.NOTE_BLOCK) return null;
		return NoteTypes.PLAYER_TELEPORTER.getMap().get(pos);
	}

	/**
	 * Attempts to load an item teleporter for a given position.
	 * Does nothing if already loaded.
	 * If the teleporter isn't loaded, the chunk will be loaded, and it will be rechecked if the tile is present.
	 */
	@Nullable
	public static ItemTeleporter hotloadItemT(WorldPos pos) {
		if (NoteTypes.ITEM_TELEPORTER.getMap().contains(pos)) return NoteTypes.ITEM_TELEPORTER.getMap().get(pos);
		Chunk chunk = StoneRecipes.INSTANCE.getServer().getWorld(pos.getDim()).getChunkAt(pos.toChunkCoords().toLocation());
		if (chunk.getBlock(pos.x >> 4, pos.y, pos.z >> 4).getType() != Material.NOTE_BLOCK) return null;
		return NoteTypes.ITEM_TELEPORTER.getMap().get(pos);
	}

}

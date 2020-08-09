package shadows.stonerecipes.listener;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockClickedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockRemovedEvent;
import shadows.stonerecipes.listener.DataHandler.Maps;
import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class CustomMachineHandler implements Listener {

	@EventHandler
	public void clicked(NoteBlockClickedEvent e) {
		WorldPos pos = new WorldPos(e.getBlock().getLocation());
		NoteTileEntity t = Maps.ALL_MACHINES.getOrDefault(pos.toChunkCoords(), Collections.emptyMap()).get(pos);
		if (t != null) {
			t.onClicked(e);
		}
	}

	@EventHandler
	public void placed(NoteBlockPlacedEvent e) {
		NoteTileType<?> t = NoteTypes.getTypeFor(e.getItemId());
		if (t != null) {
			t.place(e);
		}
	}

	@EventHandler
	public void removed(NoteBlockRemovedEvent e) {
		WorldPos pos = new WorldPos(e.getState().getLocation());
		NoteTileEntity t = Maps.ALL_MACHINES.getOrDefault(pos.toChunkCoords(), Collections.emptyMap()).get(pos);
		if (t != null) {
			t.getType().remove(pos);
		}
	}

	public void load(Chunk chunk) {
		PluginFile file = getFileFor(chunk);
		for (String s : file.getKeys(false)) {
			NoteTileType<?> type = NoteTypes.getTypeFor(file.getString(s + ".type"));
			try {
				type.load(file, s);
			} catch (Exception e) {
				StoneRecipes.INSTANCE.getLogger().info(String.format("An error occurred while loading a macine at %s with tile type %s.", s, type == null ? "null" : type.getId()));
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void save(Chunk chunk) {
		File trueFile = new File(StoneRecipes.INSTANCE.getDataFolder(), "data/" + chunk.getWorld().getName() + "/" + chunk.getX() + "_" + chunk.getZ());
		if (trueFile.exists()) trueFile.delete();
		PluginFile file = getFileFor(chunk);
		WorldPos chunkPos = new WorldPos(chunk.getWorld().getUID(), chunk.getX(), 0, chunk.getZ());
		Map<WorldPos, NoteTileEntity> map = Maps.ALL_MACHINES.remove(chunkPos);
		if (map == null || map.isEmpty()) return;

		for (NoteTileEntity tile : map.values()) {
			NoteTileType type = tile.getType();
			file.set(tile.getPos() + ".type", type.getId());
			type.save(file, tile);
		}

		if (StoneRecipes.INSTANCE.isEnabled()) BukkitLambda.runAsync(file::save);
		else file.save();
	}

	public static PluginFile getFileFor(Chunk chunk) {
		return new PluginFile(StoneRecipes.INSTANCE, "data/" + chunk.getWorld().getName() + "/" + chunk.getX() + "_" + chunk.getZ());
	}

}

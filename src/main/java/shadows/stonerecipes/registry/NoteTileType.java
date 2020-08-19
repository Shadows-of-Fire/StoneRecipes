package shadows.stonerecipes.registry;

import java.util.function.Function;

import org.bukkit.Material;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.DataHandler.MapWrapper;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class NoteTileType<T extends NoteTileEntity> {

	protected final String id;
	protected final MapWrapper<T> map = new MapWrapper<>();
	protected final Function<WorldPos, T> factory;

	public NoteTileType(String id, Function<WorldPos, T> factory) {
		this.id = id;
		this.factory = factory;
	}

	public void place(NoteBlockPlacedEvent e) {
		WorldPos pos = new WorldPos(e.getBlock().getLocation());
		T t = factory.apply(pos);
		t.start();
		map.put(pos, t);
		t.onPlaced(e.getPlacer());
	}

	public void remove(WorldPos pos) {
		T t = map.remove(pos);
		if (t == null) {
			StoneRecipes.debug("Attempted to remove a tile entity where one did not exist at %s", pos);
			return;
		}
		t.destroy();
	}

	public boolean accepts(String itemId) {
		return id.equals(itemId);
	}

	/**
	 * Loads a tile entity from a chunk file.
	 * @param file The file being loaded from.
	 * @param key The key, which is the serialized position of this tile entity.
	 */
	public void load(PluginFile file, String key) {
		WorldPos pos = new WorldPos(key);
		try {
			if (pos.toLocation().getBlock().getType() != Material.NOTE_BLOCK) {
				file.set(key, null);
				return;
			}
			T t = factory.apply(pos);
			t.read(file);
			t.start();
			map.put(pos, t);
		} catch (Exception e) {
			StoneRecipes.INSTANCE.getLogger().info("An error occurred while trying to load a " + this.getId() + " at " + pos);
			e.printStackTrace();
		}
	}

	/**
	 * Saves and unloads a given machine.
	 * @param t
	 */
	public void save(PluginFile file, T t) {
		try {
			t.write(file);
		} catch (Exception e) {
			StoneRecipes.INSTANCE.getLogger().info("An error occurred while trying to save a " + this.getId() + " at " + t.getPos());
			e.printStackTrace();
		}
		t.unload();
		map.remove(t.getPos());
	}

	public String getId() {
		return id;
	}

	public MapWrapper<T> getMap() {
		return map;
	}

}

package shadows.stonerecipes.registry;

import java.util.function.Function;

import org.bukkit.Chunk;
import org.bukkit.Material;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.DataHandler.MapWrapper;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class NoteTileType<T extends NoteTileEntity> {

	protected final String id;
	protected final PluginFile data;
	protected final MapWrapper<T> map;
	protected final Function<WorldPos, T> factory;

	public NoteTileType(String id, String fileName, MapWrapper<T> map, Function<WorldPos, T> factory) {
		this.id = id;
		this.data = new PluginFile(StoneRecipes.INSTANCE, fileName);
		this.map = map;
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
		data.set(pos.toString(), null);
		if (StoneRecipes.INSTANCE.isEnabled()) BukkitLambda.runAsync(data::save);
		else data.save();
	}

	public boolean accepts(String itemId) {
		return id.equals(itemId);
	}

	public void load(Chunk chunk) {
		for (String s : data.getKeys(false)) {
			WorldPos pos = new WorldPos(s);
			if (pos.isInside(chunk)) {
				try {
					if (pos.toLocation().getBlock().getType() != Material.NOTE_BLOCK) {
						data.set(s, null);
						continue;
					}
					T t = factory.apply(pos);
					MachineUtils.loadMachine(t, data);
					map.put(pos, t);
				} catch (Exception e) {
					StoneRecipes.INSTANCE.getLogger().info("An error occurred while trying to load a " + this.getId() + " at " + pos);
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Saves and unloads a given machine.
	 * @param t
	 */
	public void save(T t) {
		try {
			MachineUtils.saveMachine(t, data);
		} catch (Exception e) {
			StoneRecipes.INSTANCE.getLogger().info("An error occurred while trying to save a " + this.getId() + " at " + t.getPos());
			e.printStackTrace();
		}
		map.remove(t.getPos());
	}

	public void saveFile() {
		if (StoneRecipes.INSTANCE.isEnabled()) BukkitLambda.runAsync(data::save);
		else data.save();
	}

	public String getId() {
		return id;
	}

	public PluginFile getData() {
		return data;
	}

}

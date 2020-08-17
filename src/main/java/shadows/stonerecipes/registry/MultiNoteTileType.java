package shadows.stonerecipes.registry;

import java.util.Collection;
import java.util.function.BiFunction;

import org.bukkit.Material;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.DataHandler.MapWrapper;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class MultiNoteTileType<T extends NoteTileEntity> extends NoteTileType<T> {

	protected final Collection<String> ids;
	protected final BiFunction<String, WorldPos, T> factory;

	public MultiNoteTileType(String id, Collection<String> ids, MapWrapper<T> map, BiFunction<String, WorldPos, T> factory) {
		super(id, map, null);
		this.ids = ids;
		this.factory = factory;
	}

	@Override
	public void place(NoteBlockPlacedEvent e) {
		WorldPos pos = new WorldPos(e.getBlock().getLocation());
		T t = factory.apply(e.getItem().getName(), pos);
		t.start();
		map.put(pos, t);
		t.onPlaced(e.getPlacer());
	}

	@Override
	public boolean accepts(String itemId) {
		return ids.contains(itemId);
	}

	@Override
	public void load(PluginFile file, String key) {
		WorldPos pos = new WorldPos(key);
		String subtype = file.getString(pos + ".subtype");
		try {
			if (pos.toLocation().getBlock().getType() != Material.NOTE_BLOCK) {
				file.set(key, null);
				return;
			}
			T t = factory.apply(subtype, pos);
			t.read(file);
			t.start();
			map.put(pos, t);
		} catch (Exception e) {
			StoneRecipes.INSTANCE.getLogger().info("An error occurred while trying to load a " + this.getId() + " (" + subtype + ")" + " at " + pos);
			e.printStackTrace();
		}

	}

}

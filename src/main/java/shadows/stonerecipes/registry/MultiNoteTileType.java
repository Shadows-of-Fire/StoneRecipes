package shadows.stonerecipes.registry;

import java.util.Collection;
import java.util.function.BiFunction;

import org.bukkit.Chunk;
import org.bukkit.Material;

import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.DataHandler.MapWrapper;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.WorldPos;

public class MultiNoteTileType<T extends NoteTileEntity> extends NoteTileType<T> {

	protected final Collection<String> ids;
	protected final BiFunction<String, WorldPos, T> factory;

	public MultiNoteTileType(String id, String data, Collection<String> ids, MapWrapper<T> map, BiFunction<String, WorldPos, T> factory) {
		super(id, data, map, null);
		this.ids = ids;
		this.factory = factory;
	}

	@Override
	public void place(NoteBlockPlacedEvent e) {
		WorldPos pos = new WorldPos(e.getBlock().getLocation());
		T t = factory.apply(e.getItemId(), pos);
		t.start();
		map.put(pos, t);
		t.onPlaced(e.getPlacer());
	}

	@Override
	public boolean accepts(String itemId) {
		return ids.contains(itemId);
	}

	@Override
	public void load(Chunk chunk) {
		for (String s : data.getKeys(false)) {
			WorldPos pos = new WorldPos(s);
			if (pos.isInside(chunk)) {
				if (pos.toLocation().getBlock().getType() != Material.NOTE_BLOCK) {
					data.set(s, null);
					continue;
				}
				String type = data.getString(pos + ".type");
				T t = factory.apply(type, pos);
				MachineUtils.loadMachine(t, data);
				map.put(pos, t);
			}
		}
	}

}

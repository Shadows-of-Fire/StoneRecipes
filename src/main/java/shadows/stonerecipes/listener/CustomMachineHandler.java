package shadows.stonerecipes.listener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
		for (NoteTileType<?> t : NoteTypes.REGISTRY.values()) {
			try {
				t.load(chunk);
			} catch (Exception e) {
				StoneRecipes.INSTANCE.getLogger().info(String.format("An error occurred while loading chunk (%d, %d) for the tile type %s.", chunk.getX(), chunk.getZ(), t.getId()));
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void save(Chunk chunk) {
		WorldPos chunkPos = new WorldPos(chunk.getWorld().getUID(), chunk.getX(), 0, chunk.getZ());
		Map<WorldPos, NoteTileEntity> map = Maps.ALL_MACHINES.get(chunkPos);
		if (map == null || map.isEmpty()) return;

		Maps.ALL_MACHINES.remove(chunkPos);

		Set<NoteTileType> dirty = new HashSet<>();
		for (NoteTileEntity t : map.values()) {
			NoteTileType type = t.getType();
			type.save(t);
			dirty.add(type);
		}

		dirty.forEach(NoteTileType::saveFile);
	}

}

package shadows.stonerecipes.listener;

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
		NoteTileEntity t = Maps.ALL_MACHINES.get(new WorldPos(e.getBlock().getLocation()));
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
		NoteTileEntity t = Maps.ALL_MACHINES.get(pos);
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

	public void save(Chunk chunk) {
		for (NoteTileType<?> t : NoteTypes.REGISTRY.values())
			try {
				t.save(chunk);
			} catch (Exception e) {
				StoneRecipes.INSTANCE.getLogger().info(String.format("An error occurred while saving chunk (%d, %d) for the tile type %s.", chunk.getX(), chunk.getZ(), t.getId()));
				e.printStackTrace();
			}
	}

}

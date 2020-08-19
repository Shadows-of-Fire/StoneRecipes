package shadows.stonerecipes.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.util.WorldPos;

/**
 * Handles the world loading and unloading which triggers general loads for various systems.
 */
public class DataHandler implements Listener {

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		onChunkLoad(e.getChunk());
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent e) {
		onChunkUnload(e.getChunk());
	}

	@EventHandler
	public void onWorldUnload(WorldUnloadEvent e) {
		onWorldUnload(e.getWorld());
	}

	public void onChunkLoad(Chunk chunk) {
		StoneRecipes.INSTANCE.getMachineHandler().load(chunk);
		StoneRecipes.debug("Loaded chunk (%s,%s)", chunk.getX(), chunk.getZ());
	}

	public void onChunkUnload(Chunk chunk) {
		StoneRecipes.INSTANCE.getMachineHandler().save(chunk);
		StoneRecipes.debug("Unloaded chunk (%s,%s)", chunk.getX(), chunk.getZ());
	}

	public void onWorldUnload(World world) {
		for (Chunk c : world.getLoadedChunks())
			onChunkUnload(c);
	}

	public static long toLong(Chunk c) {
		return (long) c.getX() << 32L | c.getZ() & 0xffffffffL;
	}

	public static class Maps {

		public static final Map<WorldPos, NoteTileEntity> EMPTY = new HashMap<>(0);

		/**
		 * Main machine map.  This map is a map of chunk coordinates to individual positions.
		 */
		public static final Map<WorldPos, Map<WorldPos, NoteTileEntity>> ALL_MACHINES = new HashMap<>();
	}

	public static class MapWrapper<T extends NoteTileEntity> {
		private final Map<WorldPos, T> map;

		public MapWrapper() {
			this.map = new HashMap<>();
		}

		public void put(WorldPos pos, T t) {
			map.put(pos, t);
			Maps.ALL_MACHINES.computeIfAbsent(pos.toChunkCoords(), p -> new HashMap<>()).put(pos, t);
		}

		public T remove(WorldPos pos) {
			Maps.ALL_MACHINES.getOrDefault(pos.toChunkCoords(), Maps.EMPTY).remove(pos);
			return map.remove(pos);
		}

		public T get(WorldPos pos) {
			return map.get(pos);
		}

		public boolean contains(WorldPos pos) {
			return map.containsKey(pos);
		}

		public boolean contains(T t) {
			return map.containsValue(t);
		}

		public Set<WorldPos> keySet() {
			return map.keySet();
		}

		public Collection<T> values() {
			return map.values();
		}
	}

}

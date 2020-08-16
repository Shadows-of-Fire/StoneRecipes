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
import shadows.stonerecipes.tileentity.OreVeinTile;
import shadows.stonerecipes.tileentity.machine.AutoBreaker;
import shadows.stonerecipes.tileentity.machine.AutoCrafter;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.tileentity.machine.DualTypedMachine;
import shadows.stonerecipes.tileentity.machine.IndustrialTypedMachine;
import shadows.stonerecipes.tileentity.machine.ItemTeleporter;
import shadows.stonerecipes.tileentity.machine.NuclearReactor;
import shadows.stonerecipes.tileentity.machine.PlayerTeleporter;
import shadows.stonerecipes.tileentity.machine.PowerGenerator;
import shadows.stonerecipes.tileentity.machine.TypedMachine;
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
		public static final MapWrapper<Charger> CHARGERS = new MapWrapper<>();
		public static final MapWrapper<TypedMachine> TYPED_MACHINES = new MapWrapper<>();
		public static final MapWrapper<DualTypedMachine> DUAL_TYPED_MACHINES = new MapWrapper<>();
		public static final MapWrapper<PowerGenerator> GENERATORS = new MapWrapper<>();
		public static final MapWrapper<OreVeinTile> VEINS = new MapWrapper<>();
		public static final MapWrapper<NuclearReactor> REACTORS = new MapWrapper<>();
		public static final MapWrapper<PlayerTeleporter> PLAYER_TELEPORTERS = new MapWrapper<>();
		public static final MapWrapper<ItemTeleporter> ITEM_TELEPORTERS = new MapWrapper<>();
		public static final MapWrapper<AutoCrafter> AUTOCRAFTERS = new MapWrapper<>();
		public static final MapWrapper<IndustrialTypedMachine> INDUSTRIAL_TYPED_MACHINES = new MapWrapper<>();
		public static final MapWrapper<AutoBreaker> BREAKERS = new MapWrapper<>();
	}

	public static class MapWrapper<T extends NoteTileEntity> {
		private final Map<WorldPos, T> map;

		MapWrapper() {
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

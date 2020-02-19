package shadows.stonerecipes.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.tileentity.OreVeinTile;
import shadows.stonerecipes.tileentity.machine.AutoCrafter;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.tileentity.machine.ItemTeleporter;
import shadows.stonerecipes.tileentity.machine.NuclearReactor;
import shadows.stonerecipes.tileentity.machine.PlayerTeleporter;
import shadows.stonerecipes.tileentity.machine.PowerGenerator;
import shadows.stonerecipes.tileentity.machine.TypedMachine;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

/**
 * Handles the world loading and unloading which triggers general loads for various systems.
 */
public class DataHandler implements Listener {

	private static Map<UUID, LongSet> needsUnload = new HashMap<>();
	private static Map<UUID, LongSet> needsLoad = new HashMap<>();

	private PluginFile data = new PluginFile(StoneRecipes.INSTANCE, "data/chunks.yml");

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
		if (!needsLoad.computeIfAbsent(chunk.getWorld().getUID(), u -> new LongOpenHashSet()).contains(toLong(chunk))) return;
		StoneRecipes.INSTANCE.getMachineHandler().load(chunk);
		needsLoad.computeIfAbsent(chunk.getWorld().getUID(), u -> new LongOpenHashSet()).remove(toLong(chunk));
		StoneRecipes.debug("Loaded chunk (%s,%s)", chunk.getX(), chunk.getZ());
	}

	public void onChunkUnload(Chunk chunk) {
		if (!needsUnload.computeIfAbsent(chunk.getWorld().getUID(), u -> new LongOpenHashSet()).contains(toLong(chunk))) return;
		StoneRecipes.INSTANCE.getMachineHandler().save(chunk);
		needsUnload.computeIfAbsent(chunk.getWorld().getUID(), u -> new LongOpenHashSet()).remove(toLong(chunk));
		needsLoad.computeIfAbsent(chunk.getWorld().getUID(), u -> new LongOpenHashSet()).add(toLong(chunk));
		StoneRecipes.debug("Unloaded chunk (%s,%s)", chunk.getX(), chunk.getZ());
	}

	public void onWorldUnload(World world) {
		for (Chunk c : world.getLoadedChunks())
			onChunkUnload(c);
	}

	public static void needsUnload(Chunk c) {
		needsUnload.computeIfAbsent(c.getWorld().getUID(), u -> new LongOpenHashSet()).add(toLong(c));
	}

	public static long toLong(Chunk c) {
		return (long) c.getX() << 32L | c.getZ() & 0xffffffffL;
	}

	public void load() {
		for (String s : data.getKeys(false)) {
			UUID id = UUID.fromString(s);
			LongSet chunks = new LongOpenHashSet();
			for (String chunk : data.getConfigurationSection(s).getKeys(false)) {
				chunks.add(Long.parseLong(chunk));
			}
			needsLoad.put(id, chunks);
		}
	}

	public void save() {
		for (UUID id : needsLoad.keySet()) {
			ConfigurationSection sec = data.createSection(id.toString());
			for (Long s : needsLoad.get(id)) {
				sec.set(String.valueOf(s), 'c');
			}
		}
		data.save();
	}

	public static class Maps {
		public static final Map<WorldPos, NoteTileEntity> ALL_MACHINES = new HashMap<>();
		public static final MapWrapper<Charger> CHARGERS = new MapWrapper<>();
		public static final MapWrapper<TypedMachine> TYPED_MACHINES = new MapWrapper<>();
		public static final MapWrapper<PowerGenerator> GENERATORS = new MapWrapper<>();
		public static final MapWrapper<OreVeinTile> VEINS = new MapWrapper<>();
		public static final MapWrapper<NuclearReactor> REACTORS = new MapWrapper<>();
		public static final MapWrapper<PlayerTeleporter> PLAYER_TELEPORTERS = new MapWrapper<>();
		public static final MapWrapper<ItemTeleporter> ITEM_TELEPORTERS = new MapWrapper<>();
		public static final MapWrapper<AutoCrafter> AUTOCRAFTERS = new MapWrapper<>();
	}

	public static class MapWrapper<T extends NoteTileEntity> {
		private final Map<WorldPos, T> map;

		MapWrapper() {
			this.map = new HashMap<>();
		}

		public void put(WorldPos pos, T t) {
			map.put(pos, t);
			Maps.ALL_MACHINES.put(pos, t);
		}

		public T remove(WorldPos pos) {
			Maps.ALL_MACHINES.remove(pos);
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

		public void removeIf(Predicate<WorldPos> p) {
			map.keySet().removeIf(p);
			Maps.ALL_MACHINES.keySet().removeIf(p);
		}

		public Collection<T> values() {
			return map.values();
		}
	}

}

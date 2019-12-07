package shadows.stonerecipes.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import shadows.stonerecipes.util.PluginFile;

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
		StoneRecipes.INSTANCE.getMachines().load(chunk);
		StoneRecipes.INSTANCE.getTeleporters().load(chunk);
		StoneRecipes.INSTANCE.getChargers().load(chunk);
		StoneRecipes.INSTANCE.getReactors().load(chunk);
		StoneRecipes.INSTANCE.getOres().load(chunk);
		needsLoad.computeIfAbsent(chunk.getWorld().getUID(), u -> new LongOpenHashSet()).remove(toLong(chunk));
		StoneRecipes.debug("Loaded chunk (%s,%s)", chunk.getX(), chunk.getZ());
	}

	public void onChunkUnload(Chunk chunk) {
		if (!needsUnload.computeIfAbsent(chunk.getWorld().getUID(), u -> new LongOpenHashSet()).contains(toLong(chunk))) return;
		StoneRecipes.INSTANCE.getMachines().save(chunk);
		StoneRecipes.INSTANCE.getTeleporters().save(chunk);
		StoneRecipes.INSTANCE.getChargers().save(chunk);
		StoneRecipes.INSTANCE.getReactors().save(chunk);
		StoneRecipes.INSTANCE.getOres().save(chunk);
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
		return (((long) c.getX()) << 32L) | (c.getZ() & 0xffffffffL);
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

}

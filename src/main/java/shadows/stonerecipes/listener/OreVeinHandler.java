package shadows.stonerecipes.listener;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockRemovedEvent;
import shadows.stonerecipes.listener.DataHandler.Maps;
import shadows.stonerecipes.tileentity.OreVeinTile;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class OreVeinHandler implements Listener {

	protected final StoneRecipes plugin;
	protected final PluginFile config;
	protected final PluginFile data;
	protected final List<String> oreGens = new ArrayList<>();

	public OreVeinHandler(StoneRecipes plugin) {
		this.plugin = plugin;
		config = new PluginFile(this.plugin, "ore_veins.yml");
		this.data = new PluginFile(this.plugin, "data/ore_veins.yml");
		for (String generator : config.getKeys(false)) {
			oreGens.add(generator);
		}
	}

	@EventHandler
	public void onPlaced(NoteBlockPlacedEvent e) {
		if (oreGens.contains(e.getItemId())) {
			OreVeinTile vein = new OreVeinTile(e.getItemId(), new WorldPos(e.getBlock().getLocation()));
			vein.start();
			Maps.VEINS.put(vein.getPos(), vein);
		}
	}

	@EventHandler
	public void onPlayerDestroyVein(NoteBlockRemovedEvent e) {
		WorldPos pos = new WorldPos(e.getState().getLocation());
		OreVeinTile removing = Maps.VEINS.remove(pos);
		if (removing == null) return;
		removing.destroy();
		data.set(pos.toString(), null);
		data.save();
	}

	/**
	 * Loads the machines and generators for a given chunk.
	 * @param world The chunk being loaded.
	 */
	public void load(Chunk chunk) {
		for (String s : data.getKeys(false)) {
			WorldPos pos = new WorldPos(s);
			if (pos.isInside(chunk)) {
				if (!pos.toLocation().getBlock().getType().equals(Material.NOTE_BLOCK)) {
					data.set(s, null);
					continue;
				}
				String type = data.getString(pos + ".type");
				OreVeinTile machine = new OreVeinTile(type, pos);
				MachineUtils.loadMachine(machine, data);
				Maps.VEINS.put(pos, machine);
			}
		}
	}

	/**
	 * Serializes all Veins in a chunk to disk.
	 * @param chunk The chunk to save things for.
	 */
	public void save(Chunk chunk) {
		for (WorldPos pos : Maps.VEINS.keySet()) {
			if (pos.isInside(chunk)) {
				MachineUtils.saveMachine(Maps.VEINS.get(pos), data);
			}
		}
		Maps.VEINS.removeIf(pos -> pos.isInside(chunk));
		data.save();
	}

}

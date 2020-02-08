package shadows.stonerecipes.listener;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockClickedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockRemovedEvent;
import shadows.stonerecipes.listener.DataHandler.Maps;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class ChargerHandler implements Listener {

	protected final StoneRecipes plugin;
	protected final PluginFile data;

	public ChargerHandler(StoneRecipes plugin) {
		this.plugin = plugin;
		data = new PluginFile(plugin, "data/chargers.yml");
	}

	@EventHandler
	public void clicked(NoteBlockClickedEvent e) {
		WorldPos pos = new WorldPos(e.getBlock().getLocation());
		if (Maps.CHARGERS.contains(pos)) {
			openCharger(pos, e.getClicker());
			e.setSuccess();
		}
	}

	@EventHandler
	public void onPlayerOpenMachine(NoteBlockPlacedEvent e) {
		if (e.getItemId().equals("armor_charger")) {
			placeCharger(new WorldPos(e.getBlock().getLocation()));
		}
	}

	@EventHandler
	public void onPlayerDestroyMachine(NoteBlockRemovedEvent e) {
		WorldPos pos = new WorldPos(e.getState().getLocation());
		if (Maps.CHARGERS.contains(pos)) {
			removeCharger(pos);
		}
	}

	public void removeCharger(WorldPos pos) {
		Charger removing = Maps.CHARGERS.remove(pos);
		if (removing == null) {
			StoneRecipes.debug("Attempted to remove an armor charger where one did not exist at %s", pos);
			return;
		}
		removing.destroy();
		data.set(pos.toString(), null);
		data.save();
	}

	public void placeCharger(WorldPos pos) {
		Charger charger = new Charger(pos);
		charger.start();
		Maps.CHARGERS.put(pos, charger);
	}

	public void openCharger(WorldPos pos, Player player) {
		if (Maps.CHARGERS.contains(pos)) {
			Maps.CHARGERS.get(pos).openInventory(player);
		} else StoneRecipes.debug("Attempted to open an armor charger where one did not exist at %s", pos);
	}

	/**
	 * Loads the chargers for a given chunk.
	 * @param chunk The chunk being loaded.
	 */
	public void load(Chunk chunk) {
		for (String s : data.getKeys(false)) {
			WorldPos pos = new WorldPos(s);
			if (pos.isInside(chunk)) {
				if (pos.toLocation().getBlock().getType() != Material.NOTE_BLOCK) {
					data.set(s, null);
					continue;
				}
				Charger charger = new Charger(pos);
				MachineUtils.loadMachine(charger, data);
				Maps.CHARGERS.put(pos, charger);
			}
		}
	}

	/**
	 * Saves the chargers for a given chunk.
	 * @param chunk The chunk to save things for.
	 */
	public void save(Chunk chunk) {
		for (WorldPos pos : Maps.CHARGERS.keySet()) {
			if (pos.isInside(chunk)) {
				MachineUtils.saveMachine(Maps.CHARGERS.get(pos), data);
			}
		}
		Maps.CHARGERS.removeIf(pos -> pos.isInside(chunk));
		data.save();
	}

}

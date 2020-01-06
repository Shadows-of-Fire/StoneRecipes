package shadows.stonerecipes.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockClickedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockRemovedEvent;
import shadows.stonerecipes.tileentity.machine.NuclearReactor;
import shadows.stonerecipes.util.CustomBlock;
import shadows.stonerecipes.util.Laser;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class ReactorHandler implements Listener {

	public static final BlockFace[] CHAMBER_FACES = { BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH, BlockFace.DOWN, BlockFace.UP };
	public static final BlockFace[] BATTERY_FACES = { BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH, BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST };

	protected final StoneRecipes plugin;
	protected final PluginFile reactorData;
	protected final Map<WorldPos, NuclearReactor> reactors = new HashMap<>();
	protected final List<Laser> lasers = new ArrayList<>();
	protected final Predicate<NoteBlock> chamber;
	protected final Predicate<NoteBlock> battery;

	public ReactorHandler(StoneRecipes plugin) {
		this.plugin = plugin;
		reactorData = new PluginFile(this.plugin, "data/reactors.yml");
		CustomBlock note = plugin.getItems().getBlock("reactor_chamber");
		this.chamber = n -> note.match(n);
		CustomBlock note2 = plugin.getItems().getBlock("industrial_battery");
		this.battery = n -> note2.match(n);
	}

	@EventHandler
	public void onPlayerOpenReactor(NoteBlockClickedEvent e) {
		if (isChamber(e.getBlock())) {
			for (BlockFace face : CHAMBER_FACES) {
				if (e.getBlock().getRelative(face).getType().equals(Material.NOTE_BLOCK)) {
					WorldPos pos2 = new WorldPos(e.getBlock().getRelative(face).getLocation());
					if (reactors.containsKey(pos2)) {
						reactors.get(pos2).openInventory(e.getClicker());
						e.setSuccess();
						break;
					}
				}
			}
			return;
		}
		if (isBattery(e.getBlock())) {
			for (BlockFace face : BATTERY_FACES) {
				if (e.getBlock().getRelative(face).getType().equals(Material.NOTE_BLOCK)) {
					WorldPos pos2 = new WorldPos(e.getBlock().getRelative(face).getLocation());
					if (reactors.containsKey(pos2)) {
						reactors.get(pos2).openPowerGUI(e.getClicker());
						e.setSuccess();
						break;
					}
				}
			}
			return;
		}
		WorldPos pos = new WorldPos(e.getBlock().getLocation());
		if (reactors.containsKey(pos)) {
			openReactor(pos, e.getClicker());
			e.setSuccess();
			return;
		}
	}

	@EventHandler
	public void onPlayerDestroyReactor(NoteBlockRemovedEvent e) {
		WorldPos pos = new WorldPos(e.getState().getLocation());
		if (reactors.containsKey(pos)) {
			removeReactor(pos);
		} else if (chamber.test((NoteBlock) e.getState().getBlockData())) {
			updateChambers(e.getState().getLocation());
		}
	}

	@EventHandler
	public void onPlace(NoteBlockPlacedEvent e) {
		String type = e.getItemId();
		if (type.equals("nuclear_reactor")) {
			placeReactor(new WorldPos(e.getBlock().getLocation()));
		} else if (chamber.test((NoteBlock) e.getBlock().getBlockData())) {
			updateChambers(e.getBlock().getLocation());
		}
	}

	public boolean isChamber(Block block) {
		if (block.getType().equals(Material.NOTE_BLOCK)) return chamber.test((NoteBlock) block.getBlockData());
		return false;
	}

	public boolean isBattery(Block block) {
		if (block.getType().equals(Material.NOTE_BLOCK)) return battery.test((NoteBlock) block.getBlockData());
		return false;
	}

	public void updateChambers(Location loc) {
		Block block = loc.getBlock();
		for (BlockFace face : CHAMBER_FACES) {
			if (block.getRelative(face).getType().equals(Material.NOTE_BLOCK)) {
				WorldPos pos = new WorldPos(block.getRelative(face).getLocation());
				if (reactors.containsKey(pos)) {
					reactors.get(pos).updateChambers();
				}
			}
		}
	}

	public void placeReactor(WorldPos pos) {
		NuclearReactor reactor = new NuclearReactor(pos);
		reactor.start();
		reactors.put(pos, reactor);
	}

	public void openReactor(WorldPos pos, Player player) {
		if (reactors.containsKey(pos)) {
			reactors.get(pos).openInventory(player);
		} else StoneRecipes.debug("Attempted to open a reactor where one was not present at %s", pos);
	}

	public void removeReactor(WorldPos pos) {
		NuclearReactor removing = reactors.remove(pos);
		if (removing == null) {
			StoneRecipes.debug("Attempted to remove a reactor where one was not present at %s", pos);
			return;
		}
		removing.destroy();
		reactorData.set(pos.toString(), null);
		reactorData.save();
	}

	/**
	 * Loads the reactors for a given chunk.
	 * @param chunk The chunk being loaded.
	 */
	public void load(Chunk chunk) {
		for (String s : reactorData.getKeys(false)) {
			WorldPos pos = new WorldPos(s);
			if (pos.isInside(chunk)) {
				if (!pos.toLocation().getBlock().getType().equals(Material.NOTE_BLOCK)) {
					reactorData.set(s, null);
					continue;
				}
				NuclearReactor charger = new NuclearReactor(pos);
				MachineUtils.loadMachine(charger, reactorData);
				reactors.put(pos, charger);
			}
		}
	}

	/**
	 * Saves the reactors for a given chunk.
	 * @param chunk The chunk to save things for.
	 */
	public void save(Chunk chunk) {
		for (WorldPos pos : reactors.keySet()) {
			if (pos.isInside(chunk)) {
				MachineUtils.saveMachine(reactors.get(pos), reactorData);
			}
		}
		reactors.keySet().removeIf(pos -> pos.isInside(chunk));
		reactorData.save();
	}

	public Collection<NuclearReactor> getReactors() {
		return reactors.values();
	}
}

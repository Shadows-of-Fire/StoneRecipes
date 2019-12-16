package shadows.stonerecipes.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockClickedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockRemovedEvent;
import shadows.stonerecipes.tileentity.machine.CoalGenerator;
import shadows.stonerecipes.tileentity.machine.PowerGenerator;
import shadows.stonerecipes.tileentity.machine.PoweredMachine;
import shadows.stonerecipes.tileentity.machine.TypedMachine;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

/**
 * Event handler for everything machines require.
 */
public class MachineHandler implements Listener {

	private final StoneRecipes plugin;
	private final PluginFile machineData;
	private final PluginFile generatorData;
	private final PluginFile machineTypes;

	/**
	 * All machines in the world.
	 */
	private final Map<WorldPos, TypedMachine> typedMachines = new HashMap<>();

	/**
	 * All generators in the world.
	 */
	private final Map<WorldPos, PowerGenerator> generators = new HashMap<>();

	/**
	 * Machine recipe map.  Map of machine types to input-output pairs.
	 */
	private final Map<String, Map<ItemStack, ItemStack>> recipes = new HashMap<>();

	public MachineHandler(StoneRecipes plugin) {
		this.plugin = plugin;
		machineData = new PluginFile(this.plugin, "data/machines.yml");
		generatorData = new PluginFile(this.plugin, "data/generators.yml");
		machineTypes = new PluginFile(this.plugin, "machines.yml");
	}

	public void loadOutputs() {
		for (String type : machineTypes.getKeys(false)) {
			PluginFile machineOutput = new PluginFile(this.plugin, "machines/" + type + ".yml");
			Map<ItemStack, ItemStack> outputs = new HashMap<>();
			for (String input : machineOutput.getKeys(false)) {
				ItemStack stackIn = plugin.getItems().getItemForRecipe(input);
				String output = machineOutput.getString(input);
				int outcount = 1;
				if (output.contains(",")) {
					String[] split = output.split(",");
					output = split[0];
					outcount = Integer.parseInt(split[1]);
				}
				ItemStack stackOut = plugin.getItems().getItemForRecipe(output);
				stackOut.setAmount(outcount);
				if (stackIn != null && stackOut != null) outputs.put(stackIn, stackOut);
				else StoneRecipes.debug("Invalid machine recipe for %s.  Recipe %s -> %s was translated into %s -> %s.", type, input, machineOutput.getString(input), stackIn, stackOut);
			}
			recipes.put(type, outputs);
		}
	}

	public Collection<PowerGenerator> getGenerators() {
		return generators.values();
	}

	/**
	 * Handles machine gui opening.
	 */
	@EventHandler
	public void onInteract(NoteBlockClickedEvent e) {
		WorldPos pos = new WorldPos(e.getBlock().getLocation());
		if (MachineUtils.openGui(e.getClicker(), pos, typedMachines)) {
			e.setSuccess();
			return;
		} else if (MachineUtils.openGui(e.getClicker(), pos, generators)) {
			e.setSuccess();
			return;
		}
	}

	/**
	 * Handles all things that happen on right click.  Opening Machines, placing machines.
	 */
	@EventHandler
	public void onPlace(NoteBlockPlacedEvent e) {
		String type = e.getItemId();
		if (recipes.containsKey(type)) {
			placeMachine(type, new WorldPos(e.getBlock().getLocation()));
		} else if (type.equals("generator")) {
			placeGenerator(new WorldPos(e.getBlock().getLocation()));
		}
	}

	/**
	 * Handles the breaking of machines in the world to spawn custom drops.
	 */
	@EventHandler
	public void onPlayerDestroyMachine(NoteBlockRemovedEvent e) {
		WorldPos pos = new WorldPos(e.getState().getLocation());
		if (typedMachines.containsKey(pos)) {
			removeMachine(pos);
		} else if (generators.containsKey(pos)) {
			removeGenerator(pos);
		}
	}

	/**
	 * Adds a coal generator at this location
	 * @param pos The location to put a coal gen at.
	 */
	public void placeGenerator(WorldPos pos) {
		CoalGenerator gen = new CoalGenerator(pos);
		gen.start();
		generators.put(pos, gen);
	}

	/**
	 * Adds a machine of the given type at this location.
	 * @param type The machine type.
	 * @param pos The location to place it at.
	 */
	public void placeMachine(String type, WorldPos pos) {
		TypedMachine machine = new TypedMachine(type, pos);
		machine.start();
		typedMachines.put(pos, machine);
	}

	/**
	 * Removes a generator from existance, wipes it from runtime and data storage.
	 * @param pos The location of the generator to wipe.
	 */
	public void removeGenerator(WorldPos pos) {
		PowerGenerator removing = generators.remove(pos);
		if (removing == null) {
			StoneRecipes.debug("Attempted to remove a generator where one did not exist at %s.", pos);
			return;
		}
		removing.destroy();
		generatorData.set(pos.toString(), null);
		generatorData.save();
	}

	/**
	 * Opens a generator screen for a player attempting to use a generator.
	 * @param pos The position of the generator.
	 * @param player The opening player.
	 */
	public void openGenerator(WorldPos pos, Player player) {
		if (generators.containsKey(pos)) {
			generators.get(pos).openInventory(player);
		} else StoneRecipes.debug("Attempted to open a generator when one was not present at %s", pos);
	}

	/**
	 * Removes a machine from existance, wipes it from runtime and data storage.
	 * @param pos The location of the machine to wipe.
	 */
	public void removeMachine(WorldPos pos) {
		TypedMachine removing = typedMachines.remove(pos);
		if (removing == null) {
			StoneRecipes.debug("Attempted to remove a machine where one did not exist at %s.", pos);
			return;
		}
		removing.destroy();
		machineData.set(pos.toString(), null);
		machineData.save();
	}

	/**
	 * Opens a machine for a player.  A machine must be active at a location for this to work.
	 * @param pos The machine pos.
	 * @param player The opening player.
	 */
	public void openMachine(WorldPos pos, Player player) {
		if (typedMachines.containsKey(pos)) {
			typedMachines.get(pos).openInventory(player);
		} else StoneRecipes.debug("Attempted to open a machine when one was not present at %s", pos);
	}

	/**
	 * Loads the machines and generators for a given chunk.
	 * @param world The chunk being loaded.
	 */
	public void load(Chunk chunk) {
		for (String s : machineData.getKeys(false)) {
			WorldPos pos = new WorldPos(s);
			if (pos.isInside(chunk)) {
				if (!pos.toLocation().getBlock().getType().equals(Material.NOTE_BLOCK)) {
					machineData.set(s, null);
					continue;
				}
				String type = machineData.getString(pos + ".type");
				TypedMachine machine = new TypedMachine(type, pos);
				MachineUtils.loadMachine(machine, machineData);
				typedMachines.put(pos, machine);
			}
		}

		for (String loc : generatorData.getKeys(false)) {
			WorldPos pos = new WorldPos(loc);
			if (pos.isInside(chunk)) {
				if (!pos.toLocation().getBlock().getType().equals(Material.NOTE_BLOCK)) {
					generatorData.set(loc, null);
					continue;
				}
				CoalGenerator gen = new CoalGenerator(pos);
				MachineUtils.loadMachine(gen, generatorData);
				generators.put(pos, gen);
			}
		}
	}

	/**
	 * Serializes all Machines and Generators in a chunk to disk.
	 * @param chunk The chunk to save things for.
	 */
	public void save(Chunk chunk) {
		for (WorldPos pos : typedMachines.keySet()) {
			if (pos.isInside(chunk)) {
				MachineUtils.saveMachine(typedMachines.get(pos), machineData);
			}
		}
		typedMachines.keySet().removeIf(pos -> pos.isInside(chunk));
		machineData.save();

		for (WorldPos pos : generators.keySet()) {
			if (pos.isInside(chunk)) {
				MachineUtils.saveMachine(generators.get(pos), generatorData);
			}
		}
		generators.keySet().removeIf(pos -> pos.isInside(chunk));
		generatorData.save();
	}

	public PluginFile getMachineData() {
		return machineData;
	}

	public PluginFile getGeneratorData() {
		return generatorData;
	}

	public Map<ItemStack, ItemStack> getRecipes(String type) {
		return recipes.get(type);
	}

	@Nullable
	public ItemStack getOutput(String type, ItemStack input) {
		return getRecipes(type).entrySet().stream().filter(e -> ItemData.isSimilar(e.getKey(), input)).map(e -> e.getValue()).findFirst().orElse(null);
	}

	public Collection<? extends PoweredMachine> getMachines() {
		return typedMachines.values();
	}
}

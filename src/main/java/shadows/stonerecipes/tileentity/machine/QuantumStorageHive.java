package shadows.stonerecipes.tileentity.machine;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockClickedEvent;
import shadows.stonerecipes.listener.MassStorageHandler;
import shadows.stonerecipes.listener.ReactorHandler;
import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.util.CustomBlock;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

/**
 * This is the access point to a storage hive.
 * A storage hive is basically a void storage, and the number of pages accessible
 * at a given hive block depends on the number of nearby Quantum Storage Combs.
 */
public class QuantumStorageHive extends PoweredMachine {

	protected static final BlockFace[] FACES = ReactorHandler.CHAMBER_FACES;

	protected final Predicate<Block> comb;

	public QuantumStorageHive(WorldPos pos) {
		super("quantum_storage_hive", "Quantum Storage Hive", "config.yml", pos);
		CustomBlock note = StoneRecipes.INSTANCE.getItems().getBlock("quantum_storage_comb");
		this.comb = block -> block.getType().equals(Material.NOTE_BLOCK) && note.test((NoteBlock) block.getBlockData());
	}

	@SuppressWarnings("deprecation")
	@Override
	public void loadConfigData(PluginFile file) {
		this.powerCost = file.getInt(configId + ".powerCost");
		this.maxPower = Math.max(maxPower, powerCost);
		this.timer = 20;
	}

	@Override
	public NoteTileType<?> getType() {
		return NoteTypes.STORAGE_HIVE;
	}

	@Override
	public void onClicked(NoteBlockClickedEvent e) {
		if (this.getPower() > 0) {
			int combs = calculateCombs(location, new HashSet<>());
			MassStorageHandler msh = StoneRecipes.INSTANCE.getMassStorage();
			msh.setStorageCapacity(e.getClicker(), combs);
			msh.openStoragePage(e.getClicker(), 1);
			e.setSuccess();
		}
	}

	@Override
	public void timerTick() {
		this.usePower(powerCost);
	}

	public int calculateCombs(Location loc, Set<WorldPos> traversed) {
		int count = 1;
		for (BlockFace face : FACES) {
			Block block = loc.getBlock().getRelative(face);
			WorldPos pos = new WorldPos(block.getLocation());
			if (!traversed.contains(pos) && comb.test(block)) {
				count++;
				traversed.add(pos);
				count += calculateCombs(block.getLocation(), traversed);
			}
		}
		return count;
	}

}

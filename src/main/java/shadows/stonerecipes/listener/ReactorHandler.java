package shadows.stonerecipes.listener;

import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockClickedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockRemovedEvent;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.util.CustomBlock;
import shadows.stonerecipes.util.WorldPos;

public class ReactorHandler implements Listener {

	public static final BlockFace[] CHAMBER_FACES = { BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH, BlockFace.DOWN, BlockFace.UP };
	public static final BlockFace[] BATTERY_FACES = { BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH, BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST };

	protected final Predicate<NoteBlock> chamber;
	protected final Predicate<NoteBlock> battery;

	public ReactorHandler(StoneRecipes plugin) {
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
					if (NoteTypes.REACTOR.getMap().contains(pos2)) {
						NoteTypes.REACTOR.getMap().get(pos2).openInventory(e.getClicker());
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
					if (NoteTypes.REACTOR.getMap().contains(pos2)) {
						NoteTypes.REACTOR.getMap().get(pos2).openPowerGUI(e.getClicker());
						e.setSuccess();
						break;
					}
				}
			}
			return;
		}
	}

	@EventHandler
	public void onPlayerDestroyReactor(NoteBlockRemovedEvent e) {
		if (chamber.test((NoteBlock) e.getState().getBlockData())) {
			updateChambers(e.getState().getLocation());
		}
	}

	@EventHandler
	public void onPlace(NoteBlockPlacedEvent e) {
		if (chamber.test((NoteBlock) e.getBlock().getBlockData())) {
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
				if (NoteTypes.REACTOR.getMap().contains(pos)) {
					NoteTypes.REACTOR.getMap().get(pos).updateChambers();
				}
			}
		}
	}

}

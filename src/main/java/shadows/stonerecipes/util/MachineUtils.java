package shadows.stonerecipes.util;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import net.minecraft.server.v1_14_R1.EntityHuman;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.MinecraftKey;
import net.minecraft.server.v1_14_R1.PacketPlayOutCustomSoundEffect;
import net.minecraft.server.v1_14_R1.Vec3D;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.DataHandler.MapWrapper;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.tileentity.NoteTileEntity.NoteBlockInventory;

/**
 * Utility code for various machine related activities.
 */
public class MachineUtils {

	/**
	 * Attempts to open a machine gui based on a map of pos -> machine for a given position.
	 * @param player Player to open the gui for.
	 * @param pos Location of the block that was clicked.
	 * @param machines A map of worldpos -> machine that will be checked for machines to open.
	 * @return true if a machine was found at that location and a was opened, false otherwise.
	 */
	public static boolean openGui(Player player, WorldPos pos, MapWrapper<?> machines) {
		if (machines.contains(pos)) {
			machines.get(pos).openInventory(player);
			return true;
		}
		return false;
	}

	/**
	 * Retrieves a machine instance from an inventory.
	 * This method only works if the inventory is an instanceof {@link NoteBlockInventory}
	 * @param inv The inventory being checked.
	 * @param machineClass The class of the machine that should be returned.
	 * @return The machine that was stored within this inventory. or null if thet type does not match or the inv was not an {@link NoteBlockInventory}
	 */
	@Nullable
	public static <T> T getMachine(Inventory inv, Class<T> machineClass) {
		if (inv instanceof NoteBlockInventory) {
			NoteTileEntity machine = ((NoteBlockInventory) inv).getMachine();
			if (machineClass.isAssignableFrom(machine.getClass())) return machineClass.cast(machine);
		}
		return null;
	}

	/**
	 * Saves a machine to disk and unloads it from the game.  Handlers should remove this machine from their maps after calling this method.
	 */
	public static void saveMachine(NoteTileEntity machine, PluginFile file) {
		try {
			machine.write(file);
		} catch (Exception e) {
			StoneRecipes.INSTANCE.getLogger().info("A machine has thrown an exception trying to write state, it will not persist!");
			e.printStackTrace();
		}
		machine.unload();
	}

	/**
	 * Loads a machine from disk and starts it's run cycle.  Handlers should add this machine to their maps.
	 */
	public static void loadMachine(NoteTileEntity machine, PluginFile file) {
		try {
			machine.read(file);
		} catch (Exception e) {
			StoneRecipes.INSTANCE.getLogger().info("A machine has thrown an exception trying to read state, it will not persist!");
			e.printStackTrace();
		}
		machine.start();
	}

	/**
	 * Creates a note block at the given location with the provided Instrument and Note.
	 */
	public static void placeNoteBlock(Block block, CustomBlock cBlock) {
		cBlock.place(block);
		playSound(block, cBlock);
	}

	/**
	 * Plays the sound associated with this custom note block.
	 */
	public static void playSound(Block block, CustomBlock cBlock) {
		NamespacedKey sound = StoneRecipes.INSTANCE.getItems().getSound(cBlock);
		block.getWorld().playSound(block.getLocation(), sound.toString(), 1, 1);
	}

	public static void playDistancedSound(Location loc, String sound, float radius, float pitch) {
		playDistancedSound(loc, sound, SoundCategory.MASTER, radius, pitch);
	}

	public static void playDistancedSound(Location loc, String sound, SoundCategory category, float radius, float pitch) {
		if (loc != null && sound != null && category != null) {
			double x = loc.getX();
			double y = loc.getY();
			double z = loc.getZ();
			net.minecraft.server.v1_14_R1.World world = ((CraftWorld) loc.getWorld()).getHandle();
			for (EntityHuman p : world.getPlayers()) {
				double radsq = radius * radius;
				if (p.e(loc.getX(), loc.getY(), loc.getZ()) < radsq) { //getDistanceSq
					float realVol = 1F - (float) (p.e(loc.getX(), loc.getY(), loc.getZ()) / radsq);
					PacketPlayOutCustomSoundEffect packet = new PacketPlayOutCustomSoundEffect(new MinecraftKey(sound), net.minecraft.server.v1_14_R1.SoundCategory.valueOf(category.name()), new Vec3D(x, y, z), realVol, pitch);
					((EntityPlayer) p).playerConnection.sendPacket(packet);
				}
			}
		}
	}

}

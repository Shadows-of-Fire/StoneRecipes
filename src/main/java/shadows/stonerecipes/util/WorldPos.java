package shadows.stonerecipes.util;

import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import joptsimple.internal.Strings;
import shadows.stonerecipes.StoneRecipes;

/**
 * Utility class to statelessly represent a world and a position.  Unlike Location, WorldPos is immutable.
 */
public class WorldPos {

	/**
	 * Invalid WorldPos.  Represents nowhere.  Used for teleporters that are unlinked.
	 */
	public static final WorldPos INVALID = new WorldPos(UUID.fromString("5d011ece-2270-4b99-8eae-3b7741d9caf1"), -1, -1, -1);

	private final int x, y, z;

	private final UUID dim;

	public WorldPos(UUID dim, int x, int y, int z) {
		this.dim = dim;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public WorldPos(Location loc) {
		this(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	public WorldPos(String string) {
		String[] split = string.split("\\|");
		if (split.length != 4) {
			StoneRecipes.debug("Invalid WorldPos creation attempt: Split Size %d, Split Array: %s", split.length, Strings.join(split, "__"));
			throw new RuntimeException("Attempted to create invalid WorldPos from " + string);
		}
		this.dim = UUID.fromString(split[0]);
		this.x = Integer.parseInt(split[1]);
		this.y = Integer.parseInt(split[2]);
		this.z = Integer.parseInt(split[3]);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof WorldPos)) return false;
		WorldPos pos2 = (WorldPos) obj;
		return pos2.x == x && pos2.y == y && pos2.z == z && pos2.dim.equals(dim);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dim, x, y, z);
	}

	@Override
	public String toString() {
		return String.format("%s|%d|%d|%d", dim, x, y, z);
	}

	public Location toLocation() {
		return new Location(Bukkit.getWorld(dim), x, y, z);
	}

	public boolean isInside(World world) {
		return world.getUID().equals(dim);
	}

	public boolean isInside(Chunk chunk) {
		if (!isInside(chunk.getWorld())) return false;
		return ((x >> 4) == chunk.getX()) && ((z >> 4) == chunk.getZ());
	}

	public String translated() {
		return String.format("(%d, %d, %d)", x, y, z);
	}

	public WorldPos up() {
		return new WorldPos(dim, x, y + 1, z);
	}

	public WorldPos down() {
		return new WorldPos(dim, x, y - 1, z);
	}

	public WorldPos offset(int x, int y, int z) {
		return new WorldPos(dim, this.x + x, this.y + y, this.z + z);
	}

}

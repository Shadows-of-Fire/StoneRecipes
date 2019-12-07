package shadows.stonerecipes.util;

import java.lang.ref.WeakReference;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * A laser represents a point to point connection, but really this is just used for spawning particles in a line between two points.
 */
public class Laser {

	private final WeakReference<World> world;
	private final LinearEquation eq;
	private Color color = Color.YELLOW;

	/**
	 * Create a Laser instance
	 *
	 * @param start    Location where laser will starts
	 * @param end      Location where laser will ends
	 */
	public Laser(Location start, Location end) {
		this.world = new WeakReference<>(start.getWorld());
		this.eq = new LinearEquation(start.toVector(), end.toVector());
	}

	public Laser(Location start, Location end, Color color) {
		this(start, end);
		this.color = color;
	}

	public Laser(WorldPos start, WorldPos end) {
		this(start.toLocation().add(0.5, 0.5, 0.5), end.toLocation().add(0.5, 0.5, 0.5));
	}

	public void connect() {
		if (world.get() != null) {
			for (float f = 0; f < 1; f += 0.05F) {
				Vector lerp = eq.step(f);
				world.get().spawnParticle(Particle.REDSTONE, lerp.getX(), lerp.getY(), lerp.getZ(), 1, new DustOptions(color, 1));
			}
		}
	}

}
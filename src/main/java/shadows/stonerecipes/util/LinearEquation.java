package shadows.stonerecipes.util;

import org.bukkit.util.Vector;

/**
 * Helper class for creating a point to point line in 3d space.
 */
public class LinearEquation {

	protected Vector src;
	protected Vector dest;
	protected Vector vec;

	public LinearEquation(Vector src, Vector dest) {
		this.src = src;
		this.dest = dest;
		this.vec = src.clone().subtract(dest);
	}

	public Vector step(float time) {
		return new Vector(dest.getX() + time * vec.getX(), dest.getY() + time * vec.getY(), dest.getZ() + time * vec.getZ());
	}

}

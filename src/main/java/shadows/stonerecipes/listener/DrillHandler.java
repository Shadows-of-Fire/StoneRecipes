package shadows.stonerecipes.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_16_R2.BlockPosition;
import shadows.stonerecipes.item.ItemData;
import shadows.stonerecipes.tileentity.machine.AutoBreaker;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.util.Keys;

public final class DrillHandler implements Listener {

	private static Set<UUID> breakers = new HashSet<>();

	@EventHandler
	public void onDrillUsage(BlockBreakEvent e) {
		Player player = e.getPlayer();
		if (breakers.contains(player.getUniqueId()) || e.isCancelled()) return;
		ItemStack drill = e.getPlayer().getInventory().getItemInMainHand();
		if (ItemData.isEmpty(drill)) return;
		if (drill.getItemMeta() == null) return;
		PersistentDataContainer ctr = drill.getItemMeta().getPersistentDataContainer();
		int x = ctr.getOrDefault(Keys.DRILL_X, PersistentDataType.INTEGER, 0);
		int y = ctr.getOrDefault(Keys.DRILL_Y, PersistentDataType.INTEGER, 0);
		int xOffset = ctr.getOrDefault(Keys.DRILL_X_OFF, PersistentDataType.INTEGER, 0);
		int yOffset = ctr.getOrDefault(Keys.DRILL_Y_OFF, PersistentDataType.INTEGER, 0);
		int cost = ctr.getOrDefault(Keys.DRILL_COST, PersistentDataType.INTEGER, 0);
		if (x < 2 && y < 2) return;
		if (Charger.getPower(drill) < cost) return;
		int lowerY = (int) Math.ceil(-y / 2D), upperY = (int) Math.round(y / 2D);
		int lowerX = (int) Math.ceil(-x / 2D), upperX = (int) Math.round(x / 2D);
		RayTraceResult res = e.getPlayer().rayTraceBlocks(10);
		if (res == null || res.getHitBlock() == null || !res.getHitBlock().getLocation().equals(e.getBlock().getLocation())) return;

		BlockFace face = res.getHitBlockFace();
		World world = res.getHitBlock().getWorld();

		breakers.add(player.getUniqueId());
		for (int iy = lowerY; iy < upperY; iy++) {
			for (int ix = lowerX; ix < upperX; ix++) {
				Block block = world.getBlockAt(getRelative(e.getBlock().getLocation(), face, ix + xOffset, iy + yOffset));
				if (block.getType() != Material.AIR) {
					AutoBreaker.breakBlock(((CraftWorld) world).getHandle(), new BlockPosition(block.getX(), block.getY(), block.getZ()), ((CraftPlayer) player).getHandle());
				}
			}
		}
		breakers.remove(player.getUniqueId());
		Charger.usePower(drill, cost);
	}

	public static Location getRelative(Location loc, BlockFace face, int x, int y) {
		Location nLoc = loc.clone();
		nLoc.add(AXIS_VECTORS[getHorizontal(face).ordinal()].clone().multiply(x));
		nLoc.add(AXIS_VECTORS[getVertical(face).ordinal()].clone().multiply(y));
		return nLoc;
	}

	private static Vector[] AXIS_VECTORS = { new Vector(1, 0, 0), new Vector(0, 1, 0), new Vector(0, 0, 1) };

	public static Axis getHorizontal(BlockFace face) {
		switch (face) {
		case NORTH:
			return Axis.X;
		case EAST:
			return Axis.Z;
		case SOUTH:
			return Axis.X;
		case WEST:
			return Axis.Z;
		case UP:
			return Axis.Z;
		case DOWN:
			return Axis.Z;
		default:
			return Axis.X;
		}
	}

	public static Axis getVertical(BlockFace face) {
		switch (face) {
		case NORTH:
			return Axis.Y;
		case EAST:
			return Axis.Y;
		case SOUTH:
			return Axis.Y;
		case WEST:
			return Axis.Y;
		case UP:
			return Axis.X;
		case DOWN:
			return Axis.X;
		default:
			return Axis.X;
		}
	}
}

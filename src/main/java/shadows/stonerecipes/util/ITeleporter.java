package shadows.stonerecipes.util;

import shadows.stonerecipes.listener.TeleportHandler;
import shadows.stonerecipes.tileentity.machine.PlayerTeleporter;

/**
 * Represents one of the two teleporter types, used to generify code in {@link TeleportHandler}
 */
public interface ITeleporter {

	public WorldPos getLink();

	public void setLink(WorldPos link);

	default boolean isPlayerTeleporter() {
		return this instanceof PlayerTeleporter;
	}

}

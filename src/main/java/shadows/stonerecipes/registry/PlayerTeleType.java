package shadows.stonerecipes.registry;

import shadows.stoneblock.utility.BukkitLambda;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.DataHandler.Maps;
import shadows.stonerecipes.tileentity.machine.PlayerTeleporter;
import shadows.stonerecipes.util.WorldPos;

public class PlayerTeleType extends NoteTileType<PlayerTeleporter> {

	public PlayerTeleType() {
		super("player_teleporter", "data/playerTeleporters.yml", Maps.PLAYER_TELEPORTERS, PlayerTeleporter::new);
	}

	@Override
	public void remove(WorldPos pos) {
		PlayerTeleporter removed = Maps.PLAYER_TELEPORTERS.remove(pos);
		if (removed != null) {
			if (!removed.getLink().equals(WorldPos.INVALID)) {
				if (Maps.PLAYER_TELEPORTERS.contains(removed.getLink())) {
					Maps.PLAYER_TELEPORTERS.get(removed.getLink()).setLink(WorldPos.INVALID);
				} else if (data.contains(removed.getLink().toString())) {
					data.set(removed.getLink() + ".link", WorldPos.INVALID.toString());
				}
			}
			removed.destroy();
			data.set(pos.toString(), null);
			BukkitLambda.runAsync(data::save);
		} else StoneRecipes.debug("Attempted to remove a player teleporter where one was not present at %s", pos);
	}

}

package shadows.stonerecipes.registry;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.tileentity.machine.PlayerTeleporter;
import shadows.stonerecipes.util.WorldPos;

public class PlayerTeleType extends NoteTileType<PlayerTeleporter> {

	public PlayerTeleType() {
		super("player_teleporter", PlayerTeleporter::new);
	}

	@Override
	public void remove(WorldPos pos) {
		PlayerTeleporter removed = map.remove(pos);
		if (removed != null) {
			if (!removed.getLink().equals(WorldPos.INVALID)) {
				if (map.contains(removed.getLink())) {
					map.get(removed.getLink()).setLink(WorldPos.INVALID);
				}
			}
			removed.destroy();
		} else StoneRecipes.debug("Attempted to remove a player teleporter where one was not present at %s", pos);
	}

}

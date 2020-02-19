package shadows.stonerecipes.tileentity.machine;

import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.util.WorldPos;

public class AutoCrafter extends PoweredMachine {

	public AutoCrafter(WorldPos pos) {
		super("autocrafter", "Auto Crafter", "config.yml", pos);
		this.start_progress = 70;
	}

	@Override
	public NoteTileType<?> getType() {
		return NoteTypes.AUTO_CRAFTER;
	}

}

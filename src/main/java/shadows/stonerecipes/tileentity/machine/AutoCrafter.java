package shadows.stonerecipes.tileentity.machine;

import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class AutoCrafter extends NoteTileEntity {

	public AutoCrafter(WorldPos pos) {
		super("autocrafter", "Auto Crafter", "config.yml", pos);
	}

	@Override
	public void write(PluginFile file) {
	}

	@Override
	public void read(PluginFile file) {
	}

	@Override
	public NoteTileType<?> getType() {
		return NoteTypes.AUTO_CRAFTER;
	}

}

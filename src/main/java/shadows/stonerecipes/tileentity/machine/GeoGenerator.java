package shadows.stonerecipes.tileentity.machine;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockClickedEvent;
import shadows.stonerecipes.listener.ReactorHandler;
import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class GeoGenerator extends PowerGenerator {

	protected int powerPerLava;

	public GeoGenerator(WorldPos pos) {
		super("geo_generator", "Geothermal Generator", pos);
	}

	@Override
	public void loadConfigData(PluginFile file) {
		this.timer = StoneRecipes.INSTANCE.getConfig().getInt("geo_generator.timer", 1);
		this.maxPower = StoneRecipes.INSTANCE.getConfig().getInt("geo_generator.maxPower", 1000);
		this.powerPerLava = StoneRecipes.INSTANCE.getConfig().getInt("geo_generator.powerPerLava", 1);
	}

	@Override
	public void timerTick() {
		int lava = 0;
		for (BlockFace face : ReactorHandler.CHAMBER_FACES) {
			Block b = this.location.getBlock().getRelative(face);
			if (b.getType() == Material.LAVA && ((Levelled) b.getBlockData()).getLevel() == 0) {
				lava++;
			}
		}
		this.addPower(lava * powerPerLava);
	}

	@Override
	public void onClicked(NoteBlockClickedEvent e) {
	}

	@Override
	public NoteTileType<?> getType() {
		return NoteTypes.GEO_GENERATOR;
	}

}

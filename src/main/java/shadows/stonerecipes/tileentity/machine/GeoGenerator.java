package shadows.stonerecipes.tileentity.machine;

import java.util.function.Supplier;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockClickedEvent;
import shadows.stonerecipes.listener.ReactorHandler;
import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class GeoGenerator extends PowerGenerator {

	protected final Supplier<NoteTileType<?>> type;
	protected final String configId;
	protected int powerPerLava;

	public GeoGenerator(Supplier<NoteTileType<?>> type, String configId, WorldPos pos) {
		super("geo_generator", "Geothermal Generator", pos);
		this.type = type;
		this.configId = configId;
	}

	@Override
	public void loadConfigData(PluginFile file) {
		this.timer = StoneRecipes.INSTANCE.getConfig().getInt(configId + ".timer", 1);
		this.maxPower = StoneRecipes.INSTANCE.getConfig().getInt(configId + ".maxPower", 1000);
		this.powerPerLava = StoneRecipes.INSTANCE.getConfig().getInt(configId + ".powerPerLava", 1);
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
		if (lava > 0) this.addPower(lava * powerPerLava);
	}

	@Override
	public void onClicked(NoteBlockClickedEvent e) {
	}

	@Override
	public NoteTileType<?> getType() {
		return type.get();
	}

}

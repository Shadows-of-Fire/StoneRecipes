package shadows.stonerecipes.tileentity;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

public class OreVeinTile extends NoteTileEntity {

	protected BlockData ore;

	public OreVeinTile(String name, WorldPos pos) {
		super(name, name, "ore_veins.yml", pos);
	}

	/**
	 * Attempts to generate an ore block from within this ore.
	 */
	@Override
	public void finish() {
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					Block offset = location.getBlock().getRelative(x, y, z);
					if (canGenOreIn(offset)) {
						genOre(offset);
						return;
					}
				}
			}
		}
	}

	@Override
	public void loadConfigData(PluginFile file) {
		String ore = file.getString(name + ".ore");
		if (StoneRecipes.INSTANCE.getItems().getBlock(ore) != null) {
			this.ore = StoneRecipes.INSTANCE.getItems().getBlock(ore).asBlockData();
		} else {
			this.ore = Bukkit.createBlockData(Material.valueOf(ore));
		}
		this.timer = file.getInt(name + ".interval");
	}

	@Override
	public void read(PluginFile file) {
	}

	@Override
	public void write(PluginFile file) {
		file.set(pos + ".type", name);
	}

	@Override
	protected void tickInternal() {
		if (++this.ticks % timer == 0) finish();
	}

	@Override
	protected void timerTick() {
	}

	protected boolean canGenOreIn(Block block) {
		return block.getType() == Material.AIR;
	}

	protected void genOre(Block block) {
		block.setType(this.ore.getMaterial());
		block.setBlockData(this.ore.clone());
	}

}

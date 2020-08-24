package shadows.stonerecipes.tileentity.machine;

import java.util.Collections;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import shadows.stonerecipes.listener.DataHandler.Maps;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.util.Laser;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.WorldPos;

/**
 * Represents a machine that consumes energy.
 */
public abstract class PoweredMachine extends NoteTileEntity {

	protected int powerCost = 0;
	private int power = 0;
	protected int maxPower = 300;
	protected final ItemStack powerBar = new ItemStack(Material.DIAMOND_HOE);
	protected boolean receivesPower = true;

	@SuppressWarnings("deprecation")
	public PoweredMachine(String itemName, String name, String file, WorldPos pos) {
		super(itemName, name, file, pos);
		powerBar.setDurability((short) 66);
		ItemMeta barMeta = powerBar.getItemMeta();
		barMeta.setDisplayName(ChatColor.GREEN + "Power Storage");
		barMeta.setUnbreakable(true);
		barMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
		powerBar.setItemMeta(barMeta);
	}

	@Override
	public void loadConfigData(PluginFile file) {
		super.loadConfigData(file);
		this.powerCost = file.getInt(this.configId + ".powerCost");
	}

	@Override
	public void openInventory(Player player) {
		super.openInventory(player);
		this.onPowerChanged();
	}

	@Override
	protected void tickInternal() {
		super.tickInternal();
		if (receivesPower && ticks % 40 == 0) {
			int needed = this.maxPower - this.power;
			if (needed == 0) return;
			addPower(receivePower(this, needed));
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void timerTick() {
		if (getPower() < powerCost) {
			progress = 0;
			guiTex.setDurability((short) (start_progress + progress));
			return;
		}
		super.timerTick();
	}

	@Override
	public void read(PluginFile file) {
		power = file.getInt(pos + ".power");
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".power", power);
	}

	public final int getPower() {
		return power;
	}

	protected void addPower(int power) {
		this.power += power;
		this.power = this.power > maxPower ? maxPower : this.power;
		onPowerChanged();
	}

	/**
	 * Consumes power from this machine.  Returns amount consumed.
	 */
	protected int usePower(int power) {
		power = Math.min(this.power, power < 0 ? 0 : power);
		this.power -= power;
		onPowerChanged();
		return power;
	}

	public void onPowerChanged() {
	}

	/**
	 * Attempts to request power from all nearby generators (in a 3x3 chunk grid centered on the chunk this machine is in).
	 * @param machine The machine requesting power.
	 * @param amount The maximum amount of power needed.
	 * @return The amount of power gathered.
	 */
	public static int receivePower(PoweredMachine machine, int amount) {
		int power = 0;
		WorldPos curChunk = machine.pos.toChunkCoords();
		for (int x = -1; x < 2; x++) {
			for (int z = -1; z < 2; z++) {
				WorldPos chunk = new WorldPos(curChunk.getDim(), curChunk.x + x, 0, curChunk.z + z);
				for (NoteTileEntity te : Maps.ALL_MACHINES.getOrDefault(chunk, Collections.emptyMap()).values()) {
					if (te instanceof PowerGenerator && te.getLocation().distanceSquared(machine.getLocation()) < 16 * 16) {
						PowerGenerator gen = (PowerGenerator) te;
						power += gen.usePower(amount);
						amount -= power;
						if (power > 0) new Laser(gen.getPos(), machine.getPos()).connect();
						if (amount <= 0) return power;
					}
				}
			}
		}
		return power;
	}

}

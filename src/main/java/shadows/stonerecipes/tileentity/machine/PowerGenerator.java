package shadows.stonerecipes.tileentity.machine;

import org.bukkit.Location;

import shadows.stonerecipes.util.WorldPos;

/**
 * Represents a machine that generates power.
 */
public abstract class PowerGenerator extends PoweredMachine {

	public PowerGenerator(String itemName, String name, WorldPos pos) {
		this(itemName, name, "generators.yml", pos);
	}

	public PowerGenerator(String itemName, String name, String file, WorldPos pos) {
		super(itemName, name, file, pos);
		this.receivesPower = false;
		this.updater = false;
	}

	@Override
	protected void timerTick() {
		if (++progress >= MAX_PROGRESS) {
			progress = 0;
			finish();
		}
	}

	@Override
	public int usePower(int power) {
		return super.usePower(power);
	}

	@Override
	protected void dropItems() {
		Location dropLoc = location.clone().add(0.5, 0.5, 0.5);
		if (inventory.getItem(22) != null) {
			location.getWorld().dropItem(dropLoc, inventory.getItem(22));
		}
	}

}

package shadows.stonerecipes.util;

/**
 * Data class for certain int constants used.
 */
public final class Slots {

	private Slots() {
	}

	//TypedMachine Slots
	public static final int INPUT = 20;
	public static final int UPGRADE_0 = 43;
	public static final int UPGRADE_1 = UPGRADE_0 + 1;
	public static final int UPGRADE_2 = UPGRADE_0 + 9;
	public static final int UPGRADE_3 = UPGRADE_2 + 1;
	public static final int[] UPGRADES = { UPGRADE_0, UPGRADE_1, UPGRADE_2, UPGRADE_3 };
	public static final int OUTPUT = 24;
	public static final int INFO = OUTPUT + 9 + 2;

	public static final int COAL_GEN_INPUT = 22;

	public static final int GUI_TEX_SLOT = 8;

	public static final int AUTOCRAFTER_OUTPUT = 15;

	public static final int AUTOCRAFTER_REFRESH = 17;

	public static final int NORTH = 4;
	public static final int EAST = 14;
	public static final int SOUTH = 22;
	public static final int WEST = 12;

}

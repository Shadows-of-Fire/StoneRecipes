package shadows.stonerecipes.util;

import javax.annotation.Nullable;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/**
 * Holds all data needed to represent a custom item.
 */
public class CustomItem {

	private final String name;
	private final ItemStack stack;
	private final InstrumentalNote block;
	private final NamespacedKey sound;

	public CustomItem(String name, ItemStack stack, InstrumentalNote block, NamespacedKey sound) {
		this.name = name;
		this.stack = stack;
		this.block = block;
		this.sound = sound;
	}

	/**
	 * @return The internal item name of this custom item.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return The internal itemstack instance of this custom item.  This view is mutable, so do not fuck with it.
	 */
	public ItemStack getStack() {
		return stack;
	}

	/**
	 * @return The block form of this item, or null, if it cannot be placed.
	 */
	@Nullable
	public InstrumentalNote getBlock() {
		return block;
	}

	/**
	 * @return The id of the sound event to play when placed/broken.
	 */
	public NamespacedKey getSound() {
		return sound;
	}

}

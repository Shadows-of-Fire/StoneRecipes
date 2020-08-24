package shadows.stonerecipes.item;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.v1_16_R2.AxisAlignedBB;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.CustomBlockHandler.NoteBlockPlacedEvent;
import shadows.stonerecipes.util.CustomBlock;
import shadows.stonerecipes.util.MachineUtils;

/**
 * Holds all data needed to represent a custom item.
 */
public class CustomItem {

	private final String name;
	private final ItemStack stack;
	private final CustomBlock block;
	private final NamespacedKey sound;

	public CustomItem(String name, ItemStack stack, CustomBlock block, NamespacedKey sound) {
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
	public CustomBlock getBlock() {
		return block;
	}

	/**
	 * @return The id of the sound event to play when placed/broken.
	 */
	public NamespacedKey getSound() {
		return sound;
	}

	public void onItemUse(PlayerInteractEvent e) {
		Block block = e.getClickedBlock().getRelative(e.getBlockFace());
		if (e.getItem().getType() != Material.DIAMOND_HOE || !block.getType().equals(Material.AIR)) return;

		Location loc = block.getLocation();

		if (!((CraftWorld) block.getWorld()).getHandle().getEntities(null, new AxisAlignedBB(loc.getX(), loc.getY(), loc.getZ(), loc.getX() + 1, loc.getY() + 1, loc.getZ() + 1)).isEmpty()) return;

		CustomBlock cBlock = getBlock();
		if (cBlock != null) {
			BlockPlaceEvent ev = new BlockPlaceEvent(block, block.getState(), block, e.getItem(), e.getPlayer(), true, e.getHand());
			Bukkit.getServer().getPluginManager().callEvent(ev);
			if (ev.isCancelled()) return;
			MachineUtils.placeNoteBlock(block, cBlock);
			if (block.getType() == Material.NOTE_BLOCK) StoneRecipes.INSTANCE.getServer().getPluginManager().callEvent(new NoteBlockPlacedEvent(this, block, e.getItem(), e.getPlayer()));
			if (e.getPlayer().getGameMode() == GameMode.SURVIVAL) e.getItem().setAmount(e.getItem().getAmount() - 1);
		}
	}

	public static interface ItemFactory {
		CustomItem apply(String id, ItemStack stack, CustomBlock block, NamespacedKey sound);
	}

	@Override
	public String toString() {
		return "SR Item - ID: " + this.name;
	}

}

package shadows.stonerecipes.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import shadows.stonerecipes.util.CustomBlock;

public class DimKeyItem extends CustomItem {

	public DimKeyItem(String name, ItemStack stack, CustomBlock block, NamespacedKey sound) {
		super(name, stack, block, sound);
	}

	public void onItemUse(PlayerInteractEvent e) {
		if (e.getClickedBlock().getType() == Material.BLUE_ICE) {
			e.getClickedBlock().setType(Material.AIR);
			e.getItem().setAmount(0);
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					e.getClickedBlock().getRelative(x, 0, z).setType(Material.AIR);
				}
			}
		}
	}

}

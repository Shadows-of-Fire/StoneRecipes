package shadows.stonerecipes.item;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import shadows.stonerecipes.util.CustomBlock;
import shadows.stonerecipes.util.RocketTask;

public class RocketshipItem extends CustomItem {

	public RocketshipItem(String name, ItemStack stack, CustomBlock block, NamespacedKey sound) {
		super(name, stack, block, sound);
	}

	public void onItemUse(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null) {
			ArmorStand rocket = (ArmorStand) e.getPlayer().getWorld().spawnEntity(e.getClickedBlock().getLocation().clone().add(0.5, 0.5, 0.5), EntityType.ARMOR_STAND);
			rocket.setInvulnerable(true);
			rocket.setVisible(false);
			rocket.getEquipment().setHelmet(e.getItem().clone());
			e.getItem().setAmount(e.getItem().getAmount() - 1);
			rocket.addPassenger(e.getPlayer());
			new RocketTask().start(rocket, e.getPlayer());
		}
	}

}

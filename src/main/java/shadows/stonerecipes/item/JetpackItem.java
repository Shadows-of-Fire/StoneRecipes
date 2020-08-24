package shadows.stonerecipes.item;

import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import shadows.stonerecipes.util.CustomBlock;

public class JetpackItem extends CustomItem {

	public JetpackItem(String name, ItemStack stack, CustomBlock block, NamespacedKey sound) {
		super(name, stack, block, sound);
	}

	@Override
	public void onItemUse(PlayerInteractEvent e) {
		this.onItemRightClick(e);
	}

	@Override
	public boolean onItemRightClick(PlayerInteractEvent e) {
		ItemStack helm = e.getPlayer().getEquipment().getHelmet();
		EquipmentSlot slot = e.getHand();
		e.getPlayer().getEquipment().setHelmet(e.getItem());
		if (helm == null) helm = ItemData.EMPTY;
		if (slot == EquipmentSlot.HAND) {
			e.getPlayer().getEquipment().setItemInMainHand(helm);
		} else e.getPlayer().getEquipment().setItemInOffHand(helm);
		return true;
	}

}

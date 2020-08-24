package shadows.stonerecipes.item;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.util.CustomBlock;

public class BatteryPackItem extends CustomItem {

	public BatteryPackItem(String name, ItemStack stack, CustomBlock block, NamespacedKey sound) {
		super(name, stack, block, sound);
	}

	@Override
	public void onItemUse(PlayerInteractEvent e) {
		this.onItemRightClick(e);
	}

	@Override
	public boolean onItemRightClick(PlayerInteractEvent e) {
		EquipmentSlot hand = e.getHand();
		Player player = e.getPlayer();
		ItemStack battery = e.getItem();
		int power = Charger.getPower(battery);
		if (power <= 0) return false;
		ItemStack stack;
		if (hand == EquipmentSlot.HAND) stack = player.getInventory().getItemInOffHand();
		else stack = player.getInventory().getItemInMainHand();

		if (stack == null || Charger.getMaxPower(stack) <= 0) return false;
		int reqPower = Charger.getMaxPower(stack) - Charger.getPower(stack);

		if (reqPower <= 0) return false;
		Charger.usePower(stack, -Math.min(reqPower, power));
		Charger.usePower(battery, Math.min(reqPower, power));
		if (hand == EquipmentSlot.HAND) {
			player.getInventory().setItemInMainHand(battery);
			player.getInventory().setItemInOffHand(stack);
		} else {
			player.getInventory().setItemInOffHand(battery);
			player.getInventory().setItemInMainHand(stack);
		}

		e.setUseItemInHand(Result.ALLOW);
		return true;
	}

}

package shadows.stonerecipes.item;

import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.util.CustomBlock;
import shadows.stonerecipes.util.FlameParticleTask;

public class JetpackControllerItem extends CustomItem {

	public JetpackControllerItem(String name, ItemStack stack, CustomBlock block, NamespacedKey sound) {
		super(name, stack, block, sound);
	}

	@Override
	public void onItemUse(PlayerInteractEvent e) {
		this.onItemRightClick(e);
	}

	@Override
	public boolean onItemRightClick(PlayerInteractEvent e) {
		ItemStack helm = e.getPlayer().getEquipment().getHelmet();
		if ("jetpack".equals(ItemData.getItemId(helm)) && Charger.getPower(helm) >= StoneRecipes.jetCost) {
			if (e.getPlayer().getPotionEffect(PotionEffectType.LEVITATION) == null) {
				Charger.usePower(helm, StoneRecipes.jetCost);
				e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, StoneRecipes.jetTime, StoneRecipes.jetLevel - 1));
				new FlameParticleTask(e.getPlayer()).start();
				return true;
			}
		}
		return false;
	}

}

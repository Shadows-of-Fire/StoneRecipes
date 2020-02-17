package shadows.stonerecipes.listener;

import java.util.stream.Collectors;

import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import net.minecraft.server.v1_15_R1.EntityPlayer;
import shadows.stonerecipes.StoneRecipes;

public class CraftingHandler implements Listener {

	public CraftingHandler(StoneRecipes plugin) {
	}

	@EventHandler
	public void onPlayerCraft(PrepareItemCraftEvent e) {
		if (e.isRepair()) {
			e.getInventory().setResult(null);
			return;
		}
	}

	@EventHandler
	public void onlogin(PlayerJoinEvent e) {
		EntityPlayer player = ((CraftPlayer) e.getPlayer()).getHandle();
		player.discoverRecipes(player.server.getCraftingManager().recipes.values().stream().flatMap(r -> r.values().stream()).collect(Collectors.toSet()));
	}
}

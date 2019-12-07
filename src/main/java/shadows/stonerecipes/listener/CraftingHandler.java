package shadows.stonerecipes.listener;

import java.util.stream.Collectors;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Recipe;

import net.minecraft.server.v1_14_R1.EntityPlayer;
import shadows.stonerecipes.StoneRecipes;

public class CraftingHandler implements Listener {

	private final StoneRecipes plugin;

	public CraftingHandler(StoneRecipes plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerCraft(PrepareItemCraftEvent e) {
		Recipe rec = e.getRecipe();

		//not allowed to repair
		if (e.isRepair()) {
			e.getInventory().setResult(null);
			return;
		}

		//not allowed to craft noteblocks
		//TODO: Implement by removing note block recipe with data pack.
		if (rec instanceof Keyed && !((Keyed) rec).getKey().getNamespace().equals(plugin.getName()) && rec.getResult().getType().equals(Material.NOTE_BLOCK)) {
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

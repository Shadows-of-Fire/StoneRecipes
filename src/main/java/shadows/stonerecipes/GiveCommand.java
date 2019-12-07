package shadows.stonerecipes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import shadows.stonerecipes.tileentity.machine.Charger;

/**
 * Special item give command for StoneRecipes.  Handles conversion of custom item names into itemstacks.
 */
public class GiveCommand implements TabExecutor {

	StoneRecipes plugin;

	public GiveCommand(StoneRecipes plugin) {
		this.plugin = plugin;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			Player player = (Player) sender;
			Location loc = player.getTargetBlock(null, 100).getLocation().clone().add(0.5, 1.5, 0.5);
			player.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), Float.parseFloat(args[0]), false, false);
			return true;
		} else if (args.length >= 2) {
			if (Bukkit.getPlayer(args[0]) != null) {
				Player player = Bukkit.getPlayer(args[0]);
				ItemStack stack = plugin.getItems().getItem(args[1]);
				if (stack != null) {
					int amount = Integer.parseInt(args.length >= 3 ? args[2] : "1");
					stack.setAmount(amount);
					if (Charger.getMaxPower(stack) > 0) Charger.setPower(stack, Charger.getMaxPower(stack));
					player.getInventory().addItem(stack);
					sender.sendMessage(ChatColor.GREEN + "You gave " + args[0] + " " + amount + "x " + args[1]);
				} else {
					sender.sendMessage(ChatColor.RED + "Item does not exist!");
				}
			} else {
				sender.sendMessage(ChatColor.RED + "Player is not online!");
			}
			return true;
		}
		return false;
	}

	static String[] numbers = new String[64];
	static {
		for (int i = 1; i <= 64; i++)
			numbers[i - 1] = String.valueOf(i);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, Bukkit.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
		} else if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, plugin.getItems().getNames());
		} else if (args.length == 3) { return getListOfStringsMatchingLastWord(args, numbers); }
		return Collections.emptyList();
	}

	public static List<String> getListOfStringsMatchingLastWord(String[] args, String... possibilities) {
		return getListOfStringsMatchingLastWord(args, Arrays.asList(possibilities));
	}

	public static List<String> getListOfStringsMatchingLastWord(String[] inputArgs, Collection<?> possibleCompletions) {
		String s = inputArgs[inputArgs.length - 1];
		List<String> list = Lists.<String>newArrayList();

		if (!possibleCompletions.isEmpty()) {
			for (String s1 : Iterables.transform(possibleCompletions, Functions.toStringFunction())) {
				if (doesStringStartWith(s, s1)) {
					list.add(s1);
				}
			}

			if (list.isEmpty()) {
				for (Object object : possibleCompletions) {
					if (object instanceof NamespacedKey && doesStringStartWith(s, ((NamespacedKey) object).getKey())) {
						list.add(String.valueOf(object));
					}
				}
			}
		}

		return list;
	}

	public static boolean doesStringStartWith(String original, String region) {
		return region.regionMatches(true, 0, original, 0, original.length());
	}
}

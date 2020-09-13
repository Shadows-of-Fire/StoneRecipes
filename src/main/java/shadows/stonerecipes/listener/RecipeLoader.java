package shadows.stonerecipes.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.tuple.Pair;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftBlastingRecipe;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftFurnaceRecipe;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftShapedRecipe;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftShapelessRecipe;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.item.ItemData;
import shadows.stonerecipes.util.NBTBlastingRecipe;
import shadows.stonerecipes.util.NBTFurnaceRecipe;
import shadows.stonerecipes.util.NBTShapedRecipe;
import shadows.stonerecipes.util.NBTShapelessRecipe;
import shadows.stonerecipes.util.PluginFile;

/**
 * Loads custom recipes that are allowed to use our special item names.
 */
public class RecipeLoader implements Listener {

	/**
	 * Machine recipe map.  Map of machine types to input-output pairs.
	 */
	public static final Map<String, Map<ItemStack, ItemStack>> RECIPES = new HashMap<>();

	/**
	 * Machine recipe map.  Map of machine types to input-output pairs.
	 */
	public static final Map<String, Map<Pair<ItemStack, ItemStack>, ItemStack>> DUAL_RECIPES = new HashMap<>();

	/**
	 * A map of item ids to required permission nodes.  If there is no mapping, the item has no permission.
	 */
	public static final Map<String, Set<String>> PERMISSIONS = new HashMap<>();

	protected final StoneRecipes plugin;
	protected final PluginFile recipeFile;
	protected int id = 0;

	public RecipeLoader(StoneRecipes plugin) {
		this.plugin = plugin;
		recipeFile = new PluginFile(plugin, "recipes.yml");
		this.loadAllRecipes();
	}

	protected void loadAllRecipes() {
		this.loadRecipes();
		this.loadFurnaceRecipes();
		this.loadBlastRecipes();
		this.loadMachineRecipes();
		this.loadPermissions();
	}

	/**
	 * Handles the load of crafting recipes from the recipe file.
	 */
	protected void loadRecipes() {
		for (String recipe : recipeFile.getKeys(false)) {
			if (recipe.startsWith("SHAPELESS_")) {
				loadShapelessRecipe(recipe.replace("SHAPELESS_", ""));
			} else {
				loadShapedRecipe(recipe);
			}
		}
	}

	@SuppressWarnings("deprecation")
	protected void loadShapelessRecipe(String recipe) {
		try {
			ShapelessRecipe rec = new ShapelessRecipe(new NamespacedKey(plugin, "recipe_" + id++), getRecipeOutput(recipe));
			for (String ingredient : recipeFile.getStringList("SHAPELESS_" + recipe)) {
				RecipeChoice choice;
				ItemStack stack = plugin.getItems().getItemForRecipe(ingredient);
				if (!stack.hasItemMeta()) {
					List<Material> materials = new ArrayList<>();
					materials.add(stack.getType());
					choice = new RecipeChoice.MaterialChoice(materials);
				} else {
					choice = new RecipeChoice.ExactChoice(stack);
				}
				rec.addIngredient(choice);
			}
			NBTShapelessRecipe.createNBTRecipe(CraftShapelessRecipe.fromBukkitRecipe(rec));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	protected void loadShapedRecipe(String recipe) {
		try {
			ShapedRecipe rec = new ShapedRecipe(new NamespacedKey(plugin, "recipe_" + id++), getRecipeOutput(recipe));
			String[] shape = { "ABC", "DEF", "GHI" };
			char[] shapes = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I' };
			rec.shape(shape);
			int i = 1;
			for (String row : recipeFile.getStringList(recipe)) {
				for (String ingredient : row.split(",")) {
					if (!ingredient.equals("X")) {

						RecipeChoice choice;
						if (!plugin.getItems().getItemForRecipe(ingredient).hasItemMeta()) {
							List<Material> materials = new ArrayList<>();
							materials.add(plugin.getItems().getItemForRecipe(ingredient).getType());
							choice = new RecipeChoice.MaterialChoice(materials);
						} else {
							choice = new RecipeChoice.ExactChoice(plugin.getItems().getItemForRecipe(ingredient));
						}
						rec.setIngredient(shapes[i - 1], choice);
					}
					i++;
				}
			}
			NBTShapedRecipe.createNBTRecipe(CraftShapedRecipe.fromBukkitRecipe(rec));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles the load of furnace recipes from the furnace recipe file.
	 */
	@SuppressWarnings("deprecation")
	protected void loadFurnaceRecipes() {
		PluginFile recipes = new PluginFile(plugin, "furnaceRecipes.yml");
		for (String output : recipes.getKeys(false)) {
			RecipeChoice c;
			if (!plugin.getItems().getItemForRecipe(recipes.getString(output + ".input")).hasItemMeta()) {
				List<Material> materials = new ArrayList<>();
				materials.add(plugin.getItems().getItemForRecipe(recipes.getString(output + ".input")).getType());
				c = new RecipeChoice.MaterialChoice(materials);
			} else {
				c = new RecipeChoice.ExactChoice(plugin.getItems().getItemForRecipe(recipes.getString(output + ".input")));
			}
			FurnaceRecipe rec = new FurnaceRecipe(new NamespacedKey(plugin, "recipe_" + id++), plugin.getItems().getItemForRecipe(output), c, recipes.getInt(output + ".exp"), recipes.getInt(output + ".burntime"));
			NBTFurnaceRecipe.createNBTRecipe(CraftFurnaceRecipe.fromBukkitRecipe(rec));
		}
	}

	/**
	 * Handles the load of blast furnace recipes from the blast furnace recipe file.
	 */
	@SuppressWarnings("deprecation")
	protected void loadBlastRecipes() {
		PluginFile recipes = new PluginFile(plugin, "blastRecipes.yml");
		for (String output : recipes.getKeys(false)) {
			RecipeChoice c;
			if (!plugin.getItems().getItemForRecipe(recipes.getString(output + ".input")).hasItemMeta()) {
				List<Material> materials = new ArrayList<>();
				materials.add(plugin.getItems().getItemForRecipe(recipes.getString(output + ".input")).getType());
				c = new RecipeChoice.MaterialChoice(materials);
			} else {
				c = new RecipeChoice.ExactChoice(plugin.getItems().getItemForRecipe(recipes.getString(output + ".input")));
			}
			BlastingRecipe rec = new BlastingRecipe(new NamespacedKey(plugin, "recipe_" + id++), plugin.getItems().getItemForRecipe(output), c, recipes.getInt(output + ".exp"), recipes.getInt(output + ".burntime"));
			NBTBlastingRecipe.createNBTRecipe(CraftBlastingRecipe.fromBukkitRecipe(rec));
		}
	}

	/**
	 * Handles the load of custom machine recipes from each respective recipe file.
	 * Also loads dual recipes.
	 */
	protected void loadMachineRecipes() {
		RECIPES.clear();
		PluginFile machineTypes = new PluginFile(StoneRecipes.INSTANCE, "machines.yml");
		for (String type : machineTypes.getKeys(false)) {
			PluginFile machineOutput = new PluginFile(StoneRecipes.INSTANCE, "machines/" + type + ".yml");
			Map<ItemStack, ItemStack> outputs = new HashMap<>();
			for (String input : machineOutput.getKeys(false)) {
				ItemStack stackIn = StoneRecipes.INSTANCE.getItems().getItemForRecipe(input);
				String output = machineOutput.getString(input);
				int outcount = 1;
				if (output.contains(",")) {
					String[] split = output.split(",");
					output = split[0];
					outcount = Integer.parseInt(split[1]);
				}
				ItemStack stackOut = StoneRecipes.INSTANCE.getItems().getItemForRecipe(output);
				stackOut.setAmount(outcount);
				if (stackIn != null && stackOut != null) outputs.put(stackIn, stackOut);
				else StoneRecipes.debug("Invalid machine recipe for %s.  Recipe %s -> %s was translated into %s -> %s.", type, input, machineOutput.getString(input), stackIn, stackOut);
			}
			RECIPES.put(type, outputs);
		}

		DUAL_RECIPES.clear();
		machineTypes = new PluginFile(StoneRecipes.INSTANCE, "dual_machines.yml");
		for (String type : machineTypes.getKeys(false)) {
			PluginFile machineOutput = new PluginFile(StoneRecipes.INSTANCE, "dual_machines/" + type + ".yml");
			Map<Pair<ItemStack, ItemStack>, ItemStack> outputs = new HashMap<>();
			for (String input : machineOutput.getKeys(false)) {
				String[] inputs = input.split(",");
				ItemStack leftInput = StoneRecipes.INSTANCE.getItems().getItemForRecipe(inputs[0]);
				ItemStack rightInput = StoneRecipes.INSTANCE.getItems().getItemForRecipe(inputs[1]);
				String output = machineOutput.getString(input);
				int outcount = 1;
				if (output.contains(",")) {
					String[] split = output.split(",");
					output = split[0];
					outcount = Integer.parseInt(split[1]);
				}
				ItemStack stackOut = StoneRecipes.INSTANCE.getItems().getItemForRecipe(output);
				stackOut.setAmount(outcount);
				if (leftInput != null && rightInput != null && stackOut != null) outputs.put(Pair.of(leftInput, rightInput), stackOut);
				else StoneRecipes.debug("Invalid machine recipe for %s.  Recipe %s -> %s was translated into %s, %s -> %s.", type, input, machineOutput.getString(input), leftInput, rightInput, stackOut);
			}
			DUAL_RECIPES.put(type, outputs);
		}
	}

	protected void loadPermissions() {
		PluginFile perms = new PluginFile(StoneRecipes.INSTANCE, "recipeperms.yml");
		for (String s : perms.getKeys(false)) {
			Set<String> permset = new HashSet<>();
			for (String perm : perms.getStringList(s)) {
				permset.add(perm);
			}
			PERMISSIONS.put(s, permset);
		}
	}

	/**
	 * Parses a string key into a recipe output.
	 * Format for input string is <ITEM NAME>,<STACK SIZE>
	 * Stack size and comma are optional.
	 */
	protected ItemStack getRecipeOutput(String key) {
		String[] split = key.split(",");
		ItemStack stack = plugin.getItems().getItemForRecipe(split[0]);
		if (split.length == 2) stack.setAmount(Integer.parseInt(split[1]));
		return stack;
	}

	@Nullable
	public ItemStack getMachineOutput(String type, ItemStack input) {
		return RECIPES.get(type).entrySet().stream().filter(e -> ItemData.isSimilar(e.getKey(), input)).map(e -> e.getValue()).findFirst().orElse(null);
	}

	@Nullable
	public ItemStack getDualMachineOutput(String type, ItemStack left, ItemStack right) {
		return DUAL_RECIPES.get(type).entrySet().stream().filter(e -> ItemData.isSimilar(e.getKey().getLeft(), left) && ItemData.isSimilar(e.getKey().getRight(), right)).map(e -> e.getValue()).findFirst().orElse(null);
	}

	public boolean isValidLeftInput(String type, ItemStack left) {
		return DUAL_RECIPES.get(type).entrySet().stream().filter(e -> ItemData.isSimilar(e.getKey().getLeft(), left)).findAny().isPresent();
	}

	public boolean isValidRightInput(String type, ItemStack right) {
		return DUAL_RECIPES.get(type).entrySet().stream().filter(e -> ItemData.isSimilar(e.getKey().getRight(), right)).findAny().isPresent();
	}

	@EventHandler
	public void onlogin(PlayerJoinEvent e) {
		EntityPlayer player = ((CraftPlayer) e.getPlayer()).getHandle();
		player.discoverRecipes(player.server.getCraftingManager().recipes.values().stream().flatMap(r -> r.values().stream()).collect(Collectors.toSet()));
	}

	@EventHandler
	public void onCraft(PrepareItemCraftEvent e) {
		if (e.getRecipe() == null || e.getRecipe().getResult() == null || e.getInventory() == null || e.getViewers() == null) return;
		ItemStack out = e.getRecipe().getResult();
		String id = ItemData.getItemId(out);
		Set<String> perm = PERMISSIONS.get(id);
		if (perm != null && !e.getViewers().isEmpty()) {
			Player player = (Player) e.getViewers().get(0);
			if (hasPerm(player, perm)) return;
			e.getInventory().setResult(ItemData.EMPTY);
		}
	}

	/**
	 * Checks if a player has any of the permissions contained in the passed set.
	 * @param player The player to check permissions for.
	 * @param perm A set of desired permissions.
	 * @return If the player has at least one of the perms in the set.
	 */
	public static boolean hasPerm(Player player, Set<String> perm) {
		LuckPerms api = LuckPermsProvider.get();
		if (api == null) return false;
		User u = api.getUserManager().getUser(player.getUniqueId());
		if (u == null) return false;
		Collection<Node> nodes = u.getNodes();
		if (nodes != null) {
			if (nodes.stream().map(Node::getKey).anyMatch(perm::contains)) return true;
		}
		return false;
	}

}

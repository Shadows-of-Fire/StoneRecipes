package shadows.stonerecipes.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftShapelessRecipe;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import net.minecraft.server.v1_15_R1.EntityPlayer;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.util.ItemData;
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

	protected final StoneRecipes plugin;
	protected final PluginFile recipeFile;
	protected int id = 0;

	public RecipeLoader(StoneRecipes plugin) {
		this.plugin = plugin;
		recipeFile = new PluginFile(plugin, "recipes.yml");
	}

	public void loadRecipes() {
		for (String recipe : recipeFile.getKeys(false)) {
			if (recipe.startsWith("SHAPELESS_")) {
				loadShapelessRecipe(recipe.replace("SHAPELESS_", ""));
			} else {
				loadShapedRecipe(recipe);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void loadFurnaceRecipes() {
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
			plugin.getServer().addRecipe(rec);
		}
	}

	@SuppressWarnings("deprecation")
	public void loadBlastRecipes() {
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
			plugin.getServer().addRecipe(rec);
		}
	}

	@SuppressWarnings("deprecation")
	public void loadShapelessRecipe(String recipe) {
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
	public void loadShapedRecipe(String recipe) {
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
			plugin.getServer().addRecipe(rec);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ItemStack getRecipeOutput(String key) {
		String[] split = key.split(",");
		ItemStack stack = plugin.getItems().getItemForRecipe(split[0]);
		if (split.length == 2) stack.setAmount(Integer.parseInt(split[1]));
		return stack;
	}

	@EventHandler
	public void onlogin(PlayerJoinEvent e) {
		EntityPlayer player = ((CraftPlayer) e.getPlayer()).getHandle();
		player.discoverRecipes(player.server.getCraftingManager().recipes.values().stream().flatMap(r -> r.values().stream()).collect(Collectors.toSet()));
	}

	public void loadMachineRecipes() {
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
	}

	@Nullable
	public ItemStack getMachineOutput(String type, ItemStack input) {
		return RECIPES.get(type).entrySet().stream().filter(e -> ItemData.isSimilar(e.getKey(), input)).map(e -> e.getValue()).findFirst().orElse(null);
	}

}

package shadows.stonerecipes.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftShapelessRecipe;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import shadows.stonerecipes.StoneRecipes;

/**
 * Loads custom recipes that are allowed to use our special item names.
 */
public class RecipeLoader {

	StoneRecipes plugin;
	PluginFile recipes;
	int id = 0;

	public HashMap<ArrayList<ItemStack>, ItemStack> shapeless;

	public RecipeLoader(StoneRecipes plugin) {
		this.plugin = plugin;
		recipes = new PluginFile(plugin, "recipes.yml");
		shapeless = new HashMap<>();
	}

	public void loadRecipes() {
		for (String recipe : recipes.getKeys(false)) {
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
			for (String ingredient : recipes.getStringList("SHAPELESS_" + recipe)) {
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
			for (String row : recipes.getStringList(recipe)) {
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

}

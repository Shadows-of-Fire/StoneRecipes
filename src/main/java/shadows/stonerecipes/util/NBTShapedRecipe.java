package shadows.stonerecipes.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftShapedRecipe;
import org.bukkit.craftbukkit.v1_16_R2.util.CraftNamespacedKey;
import org.bukkit.inventory.RecipeChoice;

import net.minecraft.server.v1_16_R2.InventoryCrafting;
import net.minecraft.server.v1_16_R2.ItemStack;
import net.minecraft.server.v1_16_R2.MinecraftKey;
import net.minecraft.server.v1_16_R2.MinecraftServer;
import net.minecraft.server.v1_16_R2.NonNullList;
import net.minecraft.server.v1_16_R2.RecipeItemStack;
import net.minecraft.server.v1_16_R2.ShapedRecipes;
import net.minecraft.server.v1_16_R2.World;

public class NBTShapedRecipe extends ShapedRecipes {

	protected List<Predicate<ItemStack>> stackPredicates;

	public NBTShapedRecipe(MinecraftKey name, String group, int width, int height, NonNullList<RecipeItemStack> input, ItemStack output) {
		super(name, group, width, height, input, output);
	}

	@SuppressWarnings("deprecation")
	public static void createNBTRecipe(CraftShapedRecipe original) {
		String[] shape = original.getShape();
		Map<Character, RecipeChoice> ingred = original.getChoiceMap();
		int width = shape[0].length();
		NonNullList<RecipeItemStack> data = NonNullList.a(shape.length * width, RecipeItemStack.a);

		for (int i = 0; i < shape.length; ++i) {
			String row = shape[i];

			for (int j = 0; j < row.length(); ++j) {
				data.set(i * width + j, original.toNMS(ingred.get(row.charAt(j)), false));
			}
		}

		NBTShapedRecipe recipe = new NBTShapedRecipe(CraftNamespacedKey.toMinecraft(original.getKey()), original.getGroup(), width, shape.length, data, CraftItemStack.asNMSCopy(original.getResult()));
		recipe.stackPredicates = new ArrayList<>(ingred.size());
		for (RecipeItemStack ingredient : data) {
			recipe.stackPredicates.add(new SRItemIngredient(ingredient));
		}
		MinecraftServer.getServer().getCraftingManager().addRecipe(recipe);
	}

	@Override
	public boolean a(InventoryCrafting inv, World world) {
		for (int i = 0; i <= inv.g() - this.i(); ++i) {
			for (int j = 0; j <= inv.f() - this.j(); ++j) {
				if (this.matchesSubsection(inv, i, j, true)) { return true; }

				if (this.matchesSubsection(inv, i, j, false)) { return true; }
			}
		}

		return false;
	}

	private boolean matchesSubsection(InventoryCrafting inventorycrafting, int i, int j, boolean flag) {
		for (int k = 0; k < inventorycrafting.g(); ++k) {
			for (int l = 0; l < inventorycrafting.f(); ++l) {
				int i1 = k - i;
				int j1 = l - j;
				Predicate<ItemStack> recipeitemstack = RecipeItemStack.a;
				if (i1 >= 0 && j1 >= 0 && i1 < this.i() && j1 < this.j()) {
					if (flag) {
						recipeitemstack = this.stackPredicates.get(this.i() - i1 - 1 + j1 * this.i());
					} else {
						recipeitemstack = this.stackPredicates.get(i1 + j1 * this.i());
					}
				}

				if (!recipeitemstack.test(inventorycrafting.getItem(k + l * inventorycrafting.g()))) { return false; }
			}
		}

		return true;
	}
}
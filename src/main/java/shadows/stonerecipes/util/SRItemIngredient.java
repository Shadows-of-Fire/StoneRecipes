package shadows.stonerecipes.util;

import java.util.function.Predicate;

import net.minecraft.server.v1_16_R2.ItemStack;
import net.minecraft.server.v1_16_R2.RecipeItemStack;

public class SRItemIngredient implements Predicate<ItemStack> {

	protected final RecipeItemStack ingredient;

	public SRItemIngredient(RecipeItemStack ingredient) {
		this.ingredient = ingredient;
	}

	@Override
	public boolean test(ItemStack e) {
		if (!ingredient.exact) return ingredient.test(e);
		ItemStack test = ingredient.choices[0];
		String id = test.getTag().getCompound("PublicBukkitValues").getString("stonerecipes:item_id");
		return e.hasTag() && id.equals(e.getTag().getCompound("PublicBukkitValues").getString("stonerecipes:item_id"));
	}

}

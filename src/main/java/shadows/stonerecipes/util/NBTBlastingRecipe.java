package shadows.stonerecipes.util;

import java.util.function.Predicate;

import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftBlastingRecipe;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R2.util.CraftNamespacedKey;

import net.minecraft.server.v1_16_R2.IInventory;
import net.minecraft.server.v1_16_R2.ItemStack;
import net.minecraft.server.v1_16_R2.MinecraftKey;
import net.minecraft.server.v1_16_R2.MinecraftServer;
import net.minecraft.server.v1_16_R2.RecipeBlasting;
import net.minecraft.server.v1_16_R2.RecipeItemStack;
import net.minecraft.server.v1_16_R2.World;

public class NBTBlastingRecipe extends RecipeBlasting {

	Predicate<ItemStack> input;

	public NBTBlastingRecipe(MinecraftKey minecraftkey, String s, RecipeItemStack recipeitemstack, ItemStack itemstack, float f, int i) {
		super(minecraftkey, s, recipeitemstack, itemstack, f, i);
	}

	@SuppressWarnings("deprecation")
	public static void createNBTRecipe(CraftBlastingRecipe original) {
		RecipeItemStack input = original.toNMS(original.getInputChoice(), true);
		NBTBlastingRecipe recipe = new NBTBlastingRecipe(CraftNamespacedKey.toMinecraft(original.getKey()), original.getGroup(), input, CraftItemStack.asNMSCopy(original.getResult()), original.getExperience(), original.getCookingTime());
		recipe.input = new SRItemIngredient(input);
		MinecraftServer.getServer().getCraftingManager().addRecipe(recipe);
	}

	@Override
	public boolean a(IInventory inv, World world) {
		return this.input.test(inv.getItem(0));
	}

}

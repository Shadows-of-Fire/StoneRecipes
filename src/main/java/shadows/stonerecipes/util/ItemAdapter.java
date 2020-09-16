package shadows.stonerecipes.util;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.v1_16_R2.IRegistry;
import net.minecraft.server.v1_16_R2.Item;
import net.minecraft.server.v1_16_R2.ItemStack;
import net.minecraft.server.v1_16_R2.MinecraftKey;
import net.minecraft.server.v1_16_R2.MojangsonParser;
import net.minecraft.server.v1_16_R2.NBTTagCompound;

public class ItemAdapter implements JsonDeserializer<ItemStack>, JsonSerializer<ItemStack> {

	public static final ItemAdapter INSTANCE = new ItemAdapter();

	@Override
	public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
		JsonObject obj = json.getAsJsonObject();
		MinecraftKey id = new MinecraftKey(obj.get("item").getAsString());
		Item item = IRegistry.ITEM.get(id);
		int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
		NBTTagCompound tag = obj.has("nbt") ? ctx.deserialize(obj.get("nbt"), NBTTagCompound.class) : null;
		ItemStack stack = new ItemStack(item, count);
		stack.setTag(tag);
		return stack;
	}

	@Override
	public JsonElement serialize(ItemStack stack, Type typeOfSrc, JsonSerializationContext ctx) {
		JsonObject obj = new JsonObject();
		obj.add("item", ctx.serialize(IRegistry.ITEM.getKey(stack.getItem()).toString()));
		obj.add("count", ctx.serialize(stack.getCount()));
		if (stack.hasTag()) obj.add("nbt", ctx.serialize(stack.getTag()));
		return obj;
	}

	public static class NBTAdapter implements JsonDeserializer<NBTTagCompound>, JsonSerializer<NBTTagCompound> {

		public static final NBTAdapter INSTANCE = new NBTAdapter();

		@Override
		public JsonElement serialize(NBTTagCompound src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.toString());
		}

		@Override
		public NBTTagCompound deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				return MojangsonParser.parse(json.getAsString());
			} catch (CommandSyntaxException e) {
				throw new JsonParseException(e);
			}
		}

	}

}
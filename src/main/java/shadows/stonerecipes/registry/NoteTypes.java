package shadows.stonerecipes.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.listener.DataHandler.MapWrapper;
import shadows.stonerecipes.listener.DataHandler.Maps;
import shadows.stonerecipes.listener.RecipeLoader;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.tileentity.OreVeinTile;
import shadows.stonerecipes.tileentity.machine.AutoBreaker;
import shadows.stonerecipes.tileentity.machine.AutoCrafter;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.tileentity.machine.CoalGenerator;
import shadows.stonerecipes.tileentity.machine.DualTypedMachine;
import shadows.stonerecipes.tileentity.machine.IndustrialTypedMachine;
import shadows.stonerecipes.tileentity.machine.ItemTeleporter;
import shadows.stonerecipes.tileentity.machine.NuclearReactor;
import shadows.stonerecipes.tileentity.machine.PowerGenerator;
import shadows.stonerecipes.tileentity.machine.TypedMachine;
import shadows.stonerecipes.util.WorldPos;

public class NoteTypes {

	public static final Map<String, NoteTileType<?>> REGISTRY = new HashMap<>();

	public static final NoteTileType<Charger> CHARGER = register("armor_charger", Maps.CHARGERS, Charger::new);
	public static final NoteTileType<PowerGenerator> GENERATOR = register("generator", Maps.GENERATORS, CoalGenerator::new);
	public static final MultiNoteTileType<TypedMachine> TYPED_MACHINE = register(new MultiNoteTileType<>("typed_machine", RecipeLoader.RECIPES.keySet(), Maps.TYPED_MACHINES, TypedMachine::new));
	public static final MultiNoteTileType<DualTypedMachine> DUAL_TYPED_MACHINE = register(new MultiNoteTileType<>("dual_typed_machine", RecipeLoader.DUAL_RECIPES.keySet(), Maps.DUAL_TYPED_MACHINES, DualTypedMachine::new));
	public static final MultiNoteTileType<OreVeinTile> ORE_VEIN = register(new MultiNoteTileType<>("ore_vein", StoneRecipes.INSTANCE.getOreVeins(), Maps.VEINS, OreVeinTile::new));
	public static final NoteTileType<NuclearReactor> REACTOR = register("nuclear_reactor", Maps.REACTORS, NuclearReactor::new);
	public static final PlayerTeleType PLAYER_TELEPORTER = register(new PlayerTeleType());
	public static final NoteTileType<ItemTeleporter> ITEM_TELEPORTER = register("item_teleporter", Maps.ITEM_TELEPORTERS, ItemTeleporter::new);
	public static final NoteTileType<AutoCrafter> AUTO_CRAFTER = register("auto_crafter", Maps.AUTOCRAFTERS, AutoCrafter::new);
	public static final NoteTileType<IndustrialTypedMachine> INDUSTRIAL_TYPED_MACHINE = register(new MultiNoteTileType<>("industrial_typed_machine", RecipeLoader.RECIPES.keySet().stream().map(s -> "industrial_" + s).collect(Collectors.toSet()), Maps.INDUSTRIAL_TYPED_MACHINES, IndustrialTypedMachine::new));
	public static final NoteTileType<AutoBreaker> BREAKER = register("auto_breaker", Maps.BREAKERS, AutoBreaker::new);

	private static <T extends NoteTileEntity> NoteTileType<T> register(String id, MapWrapper<T> map, Function<WorldPos, T> factory) {
		return register(new NoteTileType<>(id, map, factory));
	}

	private static <T extends NoteTileType<?>> T register(T t) {
		REGISTRY.put(t.getId(), t);
		return t;
	}

	public static NoteTileType<?> getTypeFor(String itemId) {
		for (NoteTileType<?> t : REGISTRY.values()) {
			if (t.accepts(itemId)) return t;
		}
		return null;
	}
}

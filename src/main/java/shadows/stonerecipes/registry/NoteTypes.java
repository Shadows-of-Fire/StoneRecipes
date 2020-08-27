package shadows.stonerecipes.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.item.CustomItem;
import shadows.stonerecipes.listener.RecipeLoader;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.tileentity.OreVeinTile;
import shadows.stonerecipes.tileentity.machine.AutoBreaker;
import shadows.stonerecipes.tileentity.machine.AutoCrafter;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.tileentity.machine.CoalGenerator;
import shadows.stonerecipes.tileentity.machine.DualTypedMachine;
import shadows.stonerecipes.tileentity.machine.GeoGenerator;
import shadows.stonerecipes.tileentity.machine.IndustrialTypedMachine;
import shadows.stonerecipes.tileentity.machine.ItemTeleporter;
import shadows.stonerecipes.tileentity.machine.NuclearReactor;
import shadows.stonerecipes.tileentity.machine.OxygenCompressor;
import shadows.stonerecipes.tileentity.machine.PowerGenerator;
import shadows.stonerecipes.tileentity.machine.QuantumStorageHive;
import shadows.stonerecipes.tileentity.machine.TypedMachine;
import shadows.stonerecipes.util.WorldPos;

public class NoteTypes {

	public static final Map<String, NoteTileType<?>> REGISTRY = new HashMap<>();

	public static final NoteTileType<Charger> CHARGER = register("armor_charger", Charger::new);
	public static final NoteTileType<PowerGenerator> GENERATOR = register("generator", CoalGenerator::new);
	public static final MultiNoteTileType<TypedMachine> TYPED_MACHINE = register(new MultiNoteTileType<>("typed_machine", RecipeLoader.RECIPES.keySet(), TypedMachine::new));
	public static final MultiNoteTileType<DualTypedMachine> DUAL_TYPED_MACHINE = register(new MultiNoteTileType<>("dual_typed_machine", RecipeLoader.DUAL_RECIPES.keySet(), DualTypedMachine::new));
	public static final MultiNoteTileType<OreVeinTile> ORE_VEIN = register(new MultiNoteTileType<>("ore_vein", StoneRecipes.INSTANCE.getOreVeins(), OreVeinTile::new));
	public static final NoteTileType<NuclearReactor> REACTOR = register("nuclear_reactor", NuclearReactor::new);
	public static final PlayerTeleType PLAYER_TELEPORTER = register(new PlayerTeleType());
	public static final NoteTileType<ItemTeleporter> ITEM_TELEPORTER = register("item_teleporter", ItemTeleporter::new);
	public static final NoteTileType<AutoCrafter> AUTO_CRAFTER = register("auto_crafter", AutoCrafter::new);
	public static final NoteTileType<IndustrialTypedMachine> INDUSTRIAL_TYPED_MACHINE = register(new MultiNoteTileType<>("industrial_typed_machine", RecipeLoader.RECIPES.keySet().stream().map(s -> "industrial_" + s).collect(Collectors.toSet()), IndustrialTypedMachine::new));
	public static final NoteTileType<AutoBreaker> BREAKER = register("auto_breaker", AutoBreaker::new);
	public static final NoteTileType<OxygenCompressor> OXYGEN_COMPRESSOR = register("oxygen_compressor", OxygenCompressor::new);
	public static final NoteTileType<QuantumStorageHive> STORAGE_HIVE = register("quantum_storage_hive", QuantumStorageHive::new);
	public static final NoteTileType<GeoGenerator> GEO_GENERATOR = register("geo_generator", GeoGenerator::new);

	private static <T extends NoteTileEntity> NoteTileType<T> register(String id, Function<WorldPos, T> factory) {
		return register(new NoteTileType<>(id, factory));
	}

	private static <T extends NoteTileType<?>> T register(T t) {
		REGISTRY.put(t.getId(), t);
		return t;
	}

	public static NoteTileType<?> getTypeFor(CustomItem item) {
		for (NoteTileType<?> t : REGISTRY.values()) {
			if (t.accepts(item.getName())) return t;
		}
		return null;
	}

	public static NoteTileType<?> getTypeById(String id) {
		for (NoteTileType<?> t : REGISTRY.values()) {
			if (t.getId().equals(id)) return t;
		}
		return null;
	}
}

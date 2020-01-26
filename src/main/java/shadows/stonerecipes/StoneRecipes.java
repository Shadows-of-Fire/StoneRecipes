package shadows.stonerecipes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.Blocks;
import net.minecraft.server.v1_14_R1.Item;
import net.minecraft.server.v1_14_R1.ItemTool;
import net.minecraft.server.v1_14_R1.Items;
import net.minecraft.server.v1_14_R1.Material;
import net.minecraft.server.v1_14_R1.SoundEffectType;
import shadows.stonerecipes.listener.ChargerHandler;
import shadows.stonerecipes.listener.CraftingHandler;
import shadows.stonerecipes.listener.CustomBlockHandler;
import shadows.stonerecipes.listener.DataHandler;
import shadows.stonerecipes.listener.GunHandler;
import shadows.stonerecipes.listener.MachineHandler;
import shadows.stonerecipes.listener.NanosuitHandler;
import shadows.stonerecipes.listener.OreVeinHandler;
import shadows.stonerecipes.listener.ReactorHandler;
import shadows.stonerecipes.listener.TeleportHandler;
import shadows.stonerecipes.tileentity.machine.PoweredMachine;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.RecipeLoader;
import shadows.stonerecipes.util.ReflectionHelper;
import shadows.stonerecipes.util.TickHandler;

/**
 * Main plugin class for StoneRecipes.
 */
public class StoneRecipes extends JavaPlugin {

	public static final boolean DEBUG = false;

	public static StoneRecipes INSTANCE;

	/*
	 * This Plugin requires a custom built spigot jar that has a class edit for NoteBlock#updateState to return the base data with no modifications.
	 * The relevant class, BlockNote.class, is within the plugin jar.  It must go inside the spigot jar's net/minecraft/server/v1_14_R1 folder
	 */
	static {
		ReflectionHelper.setPrivateValue(Block.class, Blocks.NOTE_BLOCK, Material.BANNER, "material");
		ReflectionHelper.setPrivateValue(Block.class, Blocks.NOTE_BLOCK, SoundEffectType.d, "stepSound");
		ReflectionHelper.setPrivateValue(Block.class, Blocks.NOTE_BLOCK, 3F, "strength");
		ReflectionHelper.setPrivateValue(Block.class, Blocks.NOTE_BLOCK, 3F, "durability");
		ReflectionHelper.setPrivateValue(Item.class, Items.DIAMOND_HOE, 64, "maxStackSize");
		for (Item i : new Item[] { Items.DIAMOND_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE }) {
			Set<Block> blocks = new HashSet<>(ReflectionHelper.getPrivateValue(ItemTool.class, (ItemTool) i, "a"));
			blocks.add(Blocks.NOTE_BLOCK);
			ReflectionHelper.setPrivateValue(ItemTool.class, (ItemTool) i, blocks, "a");
		}
		ReflectionHelper.setPrivateValue(Block.class, Blocks.BLUE_ICE, Material.BANNER, "material");
	}

	private ItemData itemData;
	private MachineHandler machineHandler;
	private ChargerHandler armorHandler;
	private TeleportHandler teleportHandler;
	private NanosuitHandler powerArmorHandler;
	private ReactorHandler reactorHandler;
	private RecipeLoader recipeLoader;
	private GunHandler gunHandler;
	private DataHandler dataHandler;
	private OreVeinHandler oreHandler;
	private BukkitTask tickTask;

	public static int jetLevel, jetTime, jetCost;

	@Override
	public void onEnable() {
		INSTANCE = this;
		saveDefaultConfig();
		dataHandler = new DataHandler();
		itemData = new ItemData(this);
		itemData.loadData();
		machineHandler = new MachineHandler(this);
		machineHandler.loadOutputs();
		powerArmorHandler = new NanosuitHandler();
		armorHandler = new ChargerHandler(this);
		teleportHandler = new TeleportHandler(this);
		reactorHandler = new ReactorHandler(this);
		gunHandler = new GunHandler(this);
		gunHandler.loadGuns();
		recipeLoader = new RecipeLoader(this);
		recipeLoader.loadRecipes();
		recipeLoader.loadFurnaceRecipes();
		recipeLoader.loadBlastRecipes();
		getServer().getPluginManager().registerEvents(new CraftingHandler(this), this);
		getServer().getPluginManager().registerEvents(oreHandler = new OreVeinHandler(this), this);
		getServer().getPluginManager().registerEvents(gunHandler, this);
		getServer().getPluginManager().registerEvents(armorHandler, this);
		getServer().getPluginManager().registerEvents(powerArmorHandler, this);
		getServer().getPluginManager().registerEvents(machineHandler, this);
		getServer().getPluginManager().registerEvents(teleportHandler, this);
		getServer().getPluginManager().registerEvents(reactorHandler, this);
		getServer().getPluginManager().registerEvents(dataHandler, this);
		getServer().getPluginManager().registerEvents(new CustomBlockHandler(), this);
		getCommand("giveitem").setExecutor(new GiveCommand(this));
		dataHandler.load();
		for (World w : Bukkit.getWorlds()) {
			for (Chunk c : w.getLoadedChunks()) {
				dataHandler.onChunkLoad(c);
			}
		}
		tickTask = BukkitLambda.runTimer(TickHandler::tick, 1);
		jetLevel = getConfig().getInt("jetpack.level");
		jetCost = getConfig().getInt("jetpack.cost");
		jetTime = getConfig().getInt("jetpack.time");
	}

	@Override
	public void onDisable() {
		for (World w : Bukkit.getWorlds()) {
			dataHandler.onWorldUnload(w);
		}
		dataHandler.save();
		for (Player p : getServer().getOnlinePlayers()) {
			p.closeInventory();
			gunHandler.unscope(p);
		}
		tickTask.cancel();
	}

	public MachineHandler getMachines() {
		return machineHandler;
	}

	public ItemData getItems() {
		return itemData;
	}

	public TeleportHandler getTeleporters() {
		return teleportHandler;
	}

	public static void debug(String msg, Object... args) {
		if (DEBUG) System.out.println(String.format(msg, args));
	}

	public ChargerHandler getChargers() {
		return armorHandler;
	}

	public ReactorHandler getReactors() {
		return reactorHandler;
	}

	public GunHandler getGuns() {
		return gunHandler;
	}

	public OreVeinHandler getOres() {
		return oreHandler;
	}

	public Collection<PoweredMachine> getAllPoweredMachines() {
		List<PoweredMachine> machines = new ArrayList<>();
		machines.addAll(machineHandler.getMachines());
		machines.addAll(teleportHandler.getTeleporters());
		return machines;
	}

}

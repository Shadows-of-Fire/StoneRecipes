package shadows.stonerecipes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import net.minecraft.server.v1_16_R2.ItemTool;
import net.minecraft.server.v1_16_R2.Items;
import net.minecraft.server.v1_16_R2.SoundEffectType;
import shadows.stonerecipes.item.ItemData;
import shadows.stonerecipes.listener.CustomBlockHandler;
import shadows.stonerecipes.listener.CustomMachineHandler;
import shadows.stonerecipes.listener.DataHandler;
import shadows.stonerecipes.listener.DrillHandler;
import shadows.stonerecipes.listener.GunHandler;
import shadows.stonerecipes.listener.MassStorageHandler;
import shadows.stonerecipes.listener.MoonHandler;
import shadows.stonerecipes.listener.NanosuitHandler;
import shadows.stonerecipes.listener.ReactorHandler;
import shadows.stonerecipes.listener.RecipeLoader;
import shadows.stonerecipes.listener.TeleportHandler;
import shadows.stonerecipes.util.BackupTask;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.PluginFile;
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
		ReflectionHelper.setPrivateValue(Block.class, Blocks.NOTE_BLOCK, 2F, "strength");
		ReflectionHelper.setPrivateValue(Block.class, Blocks.NOTE_BLOCK, 2F, "durability");
		ReflectionHelper.setPrivateValue(Item.class, Items.DIAMOND_HOE, 64, "maxStackSize");
		for (Item i : new Item[] { Items.DIAMOND_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE }) {
			Set<Block> blocks = new HashSet<>(ReflectionHelper.getPrivateValue(ItemTool.class, (ItemTool) i, "a"));
			blocks.add(Blocks.NOTE_BLOCK);
			ReflectionHelper.setPrivateValue(ItemTool.class, (ItemTool) i, blocks, "a");
		}
		ReflectionHelper.setPrivateValue(Block.class, Blocks.BLUE_ICE, Material.BANNER, "material");
	}

	private ItemData itemData;
	private CustomMachineHandler machines;
	private ReactorHandler reactorHandler;
	private RecipeLoader recipeLoader;
	private GunHandler gunHandler;
	private DataHandler dataHandler;
	private MassStorageHandler storageHandler;
	private BukkitTask tickTask;
	private List<String> oreVeins = new ArrayList<>();

	public static int jetLevel, jetTime, jetCost;

	public static int moonFeatherFall, moonJumpBoost;

	public static String moonWorldName;

	@Override
	public void onEnable() {
		INSTANCE = this;
		saveDefaultConfig();
		PluginFile veins = new PluginFile(this, "ore_veins.yml");
		for (String generator : veins.getKeys(false)) {
			oreVeins.add(generator);
		}
		itemData = new ItemData(this);
		recipeLoader = new RecipeLoader(this);
		dataHandler = new DataHandler();
		machines = new CustomMachineHandler();
		reactorHandler = new ReactorHandler(this);
		gunHandler = new GunHandler(this);
		gunHandler.loadGuns();
		storageHandler = new MassStorageHandler();
		registerEvents(recipeLoader, gunHandler, new NanosuitHandler(), new TeleportHandler(), reactorHandler, dataHandler, new CustomBlockHandler(), machines, new DrillHandler(), new MoonHandler(), storageHandler);
		getCommand("giveitem").setExecutor(new GiveCommand(this));
		for (World w : Bukkit.getWorlds()) {
			for (Chunk c : w.getLoadedChunks()) {
				dataHandler.onChunkLoad(c);
			}
		}
		tickTask = BukkitLambda.runTimer(TickHandler::tick, 1);
		jetLevel = getConfig().getInt("jetpack.level");
		jetCost = getConfig().getInt("jetpack.cost");
		jetTime = getConfig().getInt("jetpack.time");
		moonFeatherFall = getConfig().getInt("moon.feather-falling-power");
		moonJumpBoost = getConfig().getInt("moon.jump-boost-power");
		moonWorldName = getConfig().getString("moon.world-name");
		getCommand("srbackup").setExecutor(new BackupTask());
		BukkitLambda.runTimerAsync(new BackupTask(), 1200, 60 * 60 * 20);
	}

	private void registerEvents(Listener... listeners) {
		for (Listener l : listeners)
			getServer().getPluginManager().registerEvents(l, this);
	}

	@Override
	public void onDisable() {
		for (World w : Bukkit.getWorlds()) {
			dataHandler.onWorldUnload(w);
		}
		for (Player p : getServer().getOnlinePlayers()) {
			p.closeInventory();
			gunHandler.unscope(p);
		}
		tickTask.cancel();
	}

	public ItemData getItems() {
		return itemData;
	}

	public static void debug(String msg, Object... args) {
		if (DEBUG) System.out.println(String.format(msg, args));
	}

	public ReactorHandler getReactors() {
		return reactorHandler;
	}

	public GunHandler getGuns() {
		return gunHandler;
	}

	public CustomMachineHandler getMachineHandler() {
		return machines;
	}

	public List<String> getOreVeins() {
		return oreVeins;
	}

	public RecipeLoader getRecipes() {
		return recipeLoader;
	}

	public MassStorageHandler getMassStorage() {
		return storageHandler;
	}

}

package shadows.stonerecipes.listener;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.common.collect.ImmutableSet;

import joptsimple.internal.Strings;
import net.minecraft.server.v1_14_R1.AxisAlignedBB;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.Blocks;
import net.minecraft.server.v1_14_R1.ChatComponentText;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.IBlockData;
import net.minecraft.server.v1_14_R1.Items;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.PacketPlayOutSetSlot;
import net.minecraft.server.v1_14_R1.WorldServer;
import shadows.stoneblock.listeners.IslandProtection;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.tileentity.NoteTileEntity;
import shadows.stonerecipes.tileentity.machine.Charger;
import shadows.stonerecipes.util.BukkitLambda;
import shadows.stonerecipes.util.CustomBlock;
import shadows.stonerecipes.util.FlameParticleTask;
import shadows.stonerecipes.util.ItemData;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.ReflectionHelper;

public class CustomBlockHandler implements Listener {

	static final Set<Material> HOEABLE = ImmutableSet.of(Material.GRASS_BLOCK, Material.GRASS_PATH, Material.DIRT, Material.COARSE_DIRT);

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
		if (HOEABLE.contains(e.getClickedBlock().getType()) && e.getItem() != null && e.getItem().getType() == Material.DIAMOND_HOE) e.setUseItemInHand(Result.DENY);
		if (!IslandProtection.canAccess(e.getPlayer(), e.getClickedBlock().getLocation())) return;
		if (e.getPlayer().isSneaking() && e.getItem() != null) {
			processItem(e);
		} else {
			if (!processBlock(e) && !hasGui(e.getClickedBlock())) processItem(e);
		}
	}

	private boolean processBlock(PlayerInteractEvent e) {
		if (e.getClickedBlock().getType() == Material.NOTE_BLOCK) {
			NoteBlockClickedEvent ev = new NoteBlockClickedEvent(e.getClickedBlock(), e.getPlayer());
			StoneRecipes.INSTANCE.getServer().getPluginManager().callEvent(ev);
			if (ev.success) e.setCancelled(true);
			return ev.success;
		}
		return false;
	}

	private void processItem(PlayerInteractEvent e) {
		if (e.getItem() == null || !e.getItem().hasItemMeta()) return;
		Block block = e.getClickedBlock().getRelative(e.getBlockFace());
		String id = ItemData.getItemId(e.getItem());

		if ("dimensional_key".equals(id) && e.getClickedBlock().getType() == Material.BLUE_ICE) {
			e.getClickedBlock().setType(Material.AIR);
			e.getItem().setAmount(0);
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					e.getClickedBlock().getRelative(x, 0, z).setType(Material.AIR);
				}
			}
		}

		if ("jetpack_controller".equals(id)) {
			ItemStack helm = e.getPlayer().getEquipment().getHelmet();
			if ("jetpack".equals(ItemData.getItemId(helm)) && Charger.getPower(helm) >= StoneRecipes.jetCost) {
				if (e.getPlayer().getPotionEffect(PotionEffectType.LEVITATION) == null) {
					Charger.usePower(helm, StoneRecipes.jetCost);
					e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, StoneRecipes.jetTime, StoneRecipes.jetLevel - 1));
					new FlameParticleTask(e.getPlayer()).start();
					return;
				}
			}
		}

		if (e.getItem().getType() != Material.DIAMOND_HOE || !block.getType().equals(Material.AIR)) return;

		Location loc = block.getLocation();

		if (!((CraftWorld) block.getWorld()).getHandle().getEntities(null, new AxisAlignedBB(loc.getX(), loc.getY(), loc.getZ(), loc.getX() + 1, loc.getY() + 1, loc.getZ() + 1)).isEmpty()) return;

		if (Strings.isNullOrEmpty(id)) return;
		CustomBlock cBlock = StoneRecipes.INSTANCE.getItems().getBlock(id);
		if (cBlock != null) {
			MachineUtils.placeNoteBlock(block, cBlock);
			if (block.getType() == Material.NOTE_BLOCK) StoneRecipes.INSTANCE.getServer().getPluginManager().callEvent(new NoteBlockPlacedEvent(id, block, e.getItem()));
			if (e.getPlayer().getGameMode() == GameMode.SURVIVAL) e.getItem().setAmount(e.getItem().getAmount() - 1);
		}

	}

	private boolean hasGui(Block block) {
		IBlockData state = ((CraftBlock) block).getNMS();
		return state.b(((CraftBlock) block).getCraftWorld().getHandle(), ((CraftBlock) block).getPosition()) != null;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerDestroyMachine(BlockBreakEvent e) {
		if (e.isCancelled()) return;
		CustomBlock cBlock = new CustomBlock(e.getBlock().getType(), e.getBlock().getBlockData());
		ItemStack stack = StoneRecipes.INSTANCE.getItems().getItem(cBlock);
		if (stack.getType() != Material.AIR) {
			e.setDropItems(false);
			e.getBlock().getDrops().clear();
			BlockState state = e.getBlock().getState();
			e.getBlock().setType(Material.AIR);
			if (e.getPlayer().getGameMode() == GameMode.SURVIVAL) e.getBlock().getWorld().dropItem(state.getLocation().clone().add(0.5, 0.5, 0.5), stack);
			MachineUtils.playSound(e.getBlock(), cBlock);
			if (state.getType() == Material.NOTE_BLOCK) {
				NoteBlockRemovedEvent ev = new NoteBlockRemovedEvent(state, e.getPlayer());
				StoneRecipes.INSTANCE.getServer().getPluginManager().callEvent(ev);
				ItemStack tool = e.getPlayer().getInventory().getItemInMainHand();
				if (tool != null && tool.getType().getKey().getKey().contains("pickaxe")) {
					tool.setDurability((short) (tool.getDurability() + 1));
				}
			}
		}
		if (e.getBlock().getRelative(BlockFace.UP).getType() == Material.NOTE_BLOCK) {
			refreshChunk(e.getBlock().getLocation());
		}
		if (e.getBlock().getType() == Material.BLUE_ICE && e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
	}

	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		if (e.getBlock().getRelative(BlockFace.UP).getType() == Material.NOTE_BLOCK) {
			refreshChunk(e.getBlock().getLocation());
		}
	}

	@EventHandler
	public void onPlayerDestroyMachine(BlockExplodeEvent e) {
		StoneRecipes.debug("Detected block explode event, blocks following:");
		for (Block b : e.blockList()) {
			CustomBlock cBlock = new CustomBlock(b.getType(), b.getBlockData());
			ItemStack stack = StoneRecipes.INSTANCE.getItems().getItem(cBlock);
			if (stack.getType() != Material.AIR) {
				if (ThreadLocalRandom.current().nextFloat() <= e.getYield()) {
					b.getWorld().dropItem(b.getLocation(), StoneRecipes.INSTANCE.getItems().getItem(b));
				}
				if (cBlock.getBlock() == Material.NOTE_BLOCK) {
					NoteBlockRemovedEvent ev = new NoteBlockRemovedEvent(b.getState(), null);
					StoneRecipes.INSTANCE.getServer().getPluginManager().callEvent(ev);
				}
				b.setType(Material.AIR);
			}
		}
		e.blockList().removeIf(b -> b.getType() == Material.BLUE_ICE);
	}

	@EventHandler
	public void onPlayerDestroyMachine(EntityExplodeEvent e) {
		StoneRecipes.debug("Detected entity explode event, blocks following:");
		for (Block b : e.blockList()) {
			CustomBlock cBlock = new CustomBlock(b.getType(), b.getBlockData());
			ItemStack stack = StoneRecipes.INSTANCE.getItems().getItem(cBlock);
			if (stack.getType() != Material.AIR) {
				if (ThreadLocalRandom.current().nextFloat() <= e.getYield()) {
					b.getWorld().dropItem(b.getLocation(), StoneRecipes.INSTANCE.getItems().getItem(b));
				}
				if (cBlock.getBlock() == Material.NOTE_BLOCK) {
					NoteBlockRemovedEvent ev = new NoteBlockRemovedEvent(b.getState(), null);
					StoneRecipes.INSTANCE.getServer().getPluginManager().callEvent(ev);
				}
				b.setType(Material.AIR);
			}
		}
		e.blockList().removeIf(b -> b.getType() == Material.BLUE_ICE);
	}

	static net.minecraft.server.v1_14_R1.ItemStack invisHoe = new net.minecraft.server.v1_14_R1.ItemStack(Items.DIAMOND_HOE);
	static {
		invisHoe.setDamage(66);
		invisHoe.getTag().setBoolean("Unbreakable", true);
		invisHoe.a(new ChatComponentText(""));
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerStackHoes(InventoryClickEvent e) {
		if (e.isCancelled() || e.getSlot() < 0) return;
		if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.DIAMOND_HOE) {
			ItemStack hoe = e.getCurrentItem().clone();
			//normal clicks
			if (e.isLeftClick() && !e.isShiftClick()) {
				if (hoe.isSimilar(e.getCursor())) {
					ItemStack cursor = e.getCursor().clone();
					int amountSlot = hoe.getAmount();
					int amountCursor = cursor.getAmount();

					int total = amountSlot + amountCursor;
					if (total > 64) {
						amountSlot = 64;
						amountCursor = total - 64;
					} else {
						amountSlot = total;
						cursor.setType(Material.AIR);
					}
					e.setCancelled(true);
					cursor.setAmount(amountCursor);
					hoe.setAmount(amountSlot);

					e.setCurrentItem(hoe);
					e.getView().setCursor(cursor);
					return;
				}
			} else if (e.isRightClick() && !e.isShiftClick()) {
				if (hoe.isSimilar(e.getCursor())) {
					ItemStack cursor = e.getCursor().clone();
					if (hoe.getAmount() < 64) {
						hoe.setAmount(hoe.getAmount() + 1);
						if (cursor.getAmount() == 1) {
							cursor.setType(Material.AIR);
						} else {
							cursor.setAmount(cursor.getAmount() - 1);
						}
						e.setCancelled(true);
						e.setCurrentItem(hoe);
						e.getView().setCursor(cursor);
						e.setCursor(cursor);
						return;
					}
				}
			} else if (e.isShiftClick() && e.getView().getTopInventory().getType() == InventoryType.CHEST) {
				BukkitLambda.runLater(() -> ((Player) e.getWhoClicked()).updateInventory(), 0);
				return;
			}
		}

		if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR && e.getCursor().getType() == Material.DIAMOND_HOE) {
			EntityPlayer truePlayer = ((CraftPlayer) e.getWhoClicked()).getHandle();
			BukkitLambda.runLater(() -> {
				truePlayer.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, truePlayer.inventory.getCarried()));
			}, 0);
		}

		if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.IRON_HOE) {
			if (e.getInventory().getType() == InventoryType.PLAYER && e.getSlot() == 5) {
				ItemStack hoe = e.getCursor();
				if (ItemData.getItemId(hoe).equals("jetpack")) {
					ItemStack helm = e.getCurrentItem().clone();
					e.setCurrentItem(hoe);
					e.setCursor(helm);
					EntityPlayer truePlayer = ((CraftPlayer) e.getWhoClicked()).getHandle();
					BukkitLambda.runLater(() -> {
						truePlayer.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, truePlayer.inventory.getCarried()));
					}, 0);
					e.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onDamage(BlockDamageEvent e) {
		if (e.getBlock().getType() == Material.NOTE_BLOCK && !e.isCancelled() && !e.getItemInHand().getType().getKey().getKey().contains("pickaxe")) {
			e.setCancelled(true);
			EntityPlayer player = (EntityPlayer) ReflectionHelper.getPrivateValue(CraftEntity.class, (CraftEntity) e.getPlayer(), "entity");
			int idx = player.inventory.itemInHandIndex;
			net.minecraft.server.v1_14_R1.ItemStack stack;
			if (!player.getItemInMainHand().isEmpty()) stack = player.getItemInMainHand().cloneItemStack();
			else stack = invisHoe;
			if (!stack.hasTag()) stack.setTag(new NBTTagCompound());
			stack.getTag().setLong("a", System.currentTimeMillis());
			player.playerConnection.sendPacket(new PacketPlayOutSetSlot(-2, idx, stack));
		} else if (e.getBlock().getType() == Material.BLUE_ICE) {
			e.setCancelled(true);
			EntityPlayer player = (EntityPlayer) ReflectionHelper.getPrivateValue(CraftEntity.class, (CraftEntity) e.getPlayer(), "entity");
			int idx = player.inventory.itemInHandIndex;
			net.minecraft.server.v1_14_R1.ItemStack stack;
			if (!player.getItemInMainHand().isEmpty()) stack = player.getItemInMainHand().cloneItemStack();
			else stack = invisHoe;
			if (!stack.hasTag()) stack.setTag(new NBTTagCompound());
			stack.getTag().setLong("a", System.currentTimeMillis());
			player.playerConnection.sendPacket(new PacketPlayOutSetSlot(-2, idx, stack));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void notes(NotePlayEvent e) {
		e.setCancelled(true);
	}

	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		NoteTileEntity machine = MachineUtils.getMachine(e.getInventory(), NoteTileEntity.class);
		if (machine != null) {
			if (e.getRawSlots().stream().anyMatch(s -> e.getView().getInventory(s) == machine.getInv() && !machine.isClickableSlot(e.getView().convertSlot(s)))) {
				e.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	public void onPlayerClick(InventoryClickEvent e) {
		NoteTileEntity machine = MachineUtils.getMachine(e.getInventory(), NoteTileEntity.class);
		if (machine != null) {
			if (e.getClickedInventory() == e.getInventory() && !machine.isClickableSlot(e.getSlot())) {
				e.setCancelled(true);
				return;
			}
			if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				machine.handleShiftClick(e);
			} else if (e.getClickedInventory() == e.getInventory()) machine.onSlotClick(e);
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void anvilRepairBlock(PrepareAnvilEvent e) {
		AnvilInventory inv = e.getInventory();
		ItemStack input = inv.getItem(0);
		if (input != null) {
			net.minecraft.server.v1_14_R1.ItemStack nms = CraftItemStack.asNMSCopy(input);
			if (nms.hasTag() && nms.getTag().hasKey("CustomModelData")) {
				e.setResult(null);
				e.getViewers().forEach(p -> ((Player) p).updateInventory());
			}
		}
	}

	private static void refreshChunk(Location loc) {
		WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();
		BlockPosition pos = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		for (int i = 0; i < 5; i++)
			world.notify(pos.up(i), Blocks.AIR.getBlockData(), Blocks.ZOMBIE_HEAD.getBlockData(), 3);
	}

	/**
	 * Fired when a custom item places it's note block form into the world.  Should be acted upon to place custom machine tile entities or similar.
	 */
	public static class NoteBlockPlacedEvent extends BlockEvent {

		static HandlerList handlers = new HandlerList();

		protected final String itemId;
		protected final ItemStack placed;

		public NoteBlockPlacedEvent(String itemId, Block noteBlock, ItemStack placed) {
			super(noteBlock);
			this.itemId = itemId;
			this.placed = placed;
		}

		public String getItemId() {
			return itemId;
		}

		public ItemStack getPlaced() {
			return placed;
		}

		@Override
		public HandlerList getHandlers() {
			return handlers;
		}

		public static HandlerList getHandlerList() {
			return handlers;
		}

	}

	/**
	 * Fired when a custom note block is removed from the world.
	 */
	public static class NoteBlockRemovedEvent extends Event {

		static HandlerList handlers = new HandlerList();

		protected final BlockState state;
		protected final Player breaker;

		public NoteBlockRemovedEvent(BlockState state, Player breaker) {
			this.state = state;
			this.breaker = breaker;
		}

		@Override
		public HandlerList getHandlers() {
			return handlers;
		}

		public static HandlerList getHandlerList() {
			return handlers;
		}

		public BlockState getState() {
			return state;
		}

		@Nullable
		public Player getPlayer() {
			return breaker;
		}

	}

	/**
	 * Fired when a note block is clicked.  Used to open guis or similar.
	 */
	public static class NoteBlockClickedEvent extends BlockEvent {

		static HandlerList handlers = new HandlerList();

		protected final Player clicker;
		protected boolean success = false;

		public NoteBlockClickedEvent(Block noteBlock, Player clicker) {
			super(noteBlock);
			this.clicker = clicker;
		}

		public Player getClicker() {
			return clicker;
		}

		public void setSuccess() {
			this.success = true;
		}

		@Override
		public HandlerList getHandlers() {
			return handlers;
		}

		public static HandlerList getHandlerList() {
			return handlers;
		}

	}

}

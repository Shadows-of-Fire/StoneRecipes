package shadows.stonerecipes.tileentity.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.event.CraftEventFactory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.v1_15_R1.BlockCommand;
import net.minecraft.server.v1_15_R1.BlockJigsaw;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.BlockStructure;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.EnumGamemode;
import net.minecraft.server.v1_15_R1.EnumItemSlot;
import net.minecraft.server.v1_15_R1.IBlockData;
import net.minecraft.server.v1_15_R1.PlayerInteractManager;
import net.minecraft.server.v1_15_R1.TileEntity;
import net.minecraft.server.v1_15_R1.World;
import net.minecraft.server.v1_15_R1.WorldServer;
import shadows.stonerecipes.StoneRecipes;
import shadows.stonerecipes.registry.NoteTileType;
import shadows.stonerecipes.registry.NoteTypes;
import shadows.stonerecipes.util.FakePlayer;
import shadows.stonerecipes.util.MachineUtils;
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.WorldPos;

public class AutoBreaker extends PoweredMachine {

	protected BlockFace facing = BlockFace.NORTH;
	protected GameProfile playerId;
	protected FakePlayer fakePlayer;

	protected static final ItemStack NORTH = MachineUtils.hoeWithDura(422, ChatColor.RESET + "North");
	protected static final ItemStack EAST = MachineUtils.hoeWithDura(423, ChatColor.RESET + "East");
	protected static final ItemStack SOUTH = MachineUtils.hoeWithDura(424, ChatColor.RESET + "South");
	protected static final ItemStack WEST = MachineUtils.hoeWithDura(425, ChatColor.RESET + "West");

	@SuppressWarnings("deprecation")
	public AutoBreaker(WorldPos pos) {
		super("auto_breaker", "Auto Breaker", "config.yml", pos);
		this.start_progress = 426;
		this.updater = false;
		guiTex.setDurability((short) start_progress);
		ItemMeta meta = guiTex.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(" ");
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
		guiTex.setItemMeta(meta);
	}

	@Override
	public void setupContainer() {
		this.inventory.setItemInternal(Slots.GUI_TEX_SLOT, guiTex);
		this.inventory.setItemInternal(Slots.NORTH, NORTH);
		this.inventory.setItemInternal(Slots.EAST, EAST);
		this.inventory.setItemInternal(Slots.SOUTH, SOUTH);
		this.inventory.setItemInternal(Slots.WEST, WEST);
	}

	@Override
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void read(PluginFile file) {
		super.read(file);
		if (file.isList(pos + ".inv")) {
			List<ItemStack> content = (List<ItemStack>) file.getList(pos + ".inv");
			for (int i = 9 * 3; i < inventory.getSize(); i++) {
				if (content.get(i - 9 * 3) != null) {
					inventory.setItemInternal(i, content.get(i - 9 * 3));
				}
			}
		} else StoneRecipes.INSTANCE.getLogger().info("Failed to read inventory for a " + displayName + " at " + pos.translated());
		this.facing = BlockFace.valueOf(file.getString(pos + ".facing"));
		this.playerId = new GameProfile(UUID.fromString(file.getString(pos + ".uuid")), file.getString(pos + ".username"));
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".inv", Arrays.asList(inventory.getContents()).subList(9 * 3, inventory.getSize()));
		file.set(pos + ".facing", facing.toString());
		file.set(pos + ".uuid", playerId.getId().toString());
		file.set(pos + ".username", playerId.getName());
	}

	@Override
	public NoteTileType<?> getType() {
		return NoteTypes.BREAKER;
	}

	@Override
	protected void timerTick() {
		if (this.getPower() >= powerCost) {
			Block toBreak = this.location.getBlock().getRelative(facing);
			if (toBreak.getType() != Material.AIR) {
				BlockState state = toBreak.getState();
				World world = ((CraftWorld) this.location.getWorld()).getHandle();
				FakePlayer player = fakePlayer == null ? (fakePlayer = new FakePlayer((WorldServer) world, playerId)) : fakePlayer;
				breakBlock(world, new BlockPosition(toBreak.getX(), toBreak.getY(), toBreak.getZ()), player);
				if (!toBreak.getState().equals(state)) {
					List<Item> items = toBreak.getWorld().getNearbyEntities(toBreak.getLocation(), 1.5, 1.5, 1.5).stream().filter(t -> t instanceof Item).map(t -> (Item) t).collect(Collectors.toList());
					for (Item i : items) {
						ItemStack res = this.insertItem(i.getItemStack(), false);
						if (!isEmpty(res)) i.setItemStack(res);
						else i.remove();
					}
					this.usePower(powerCost);
				}
			}
		}
	}

	@Override
	public void onSlotClick(InventoryClickEvent e) {
		if (e.getSlot() == Slots.NORTH) {
			this.facing = BlockFace.NORTH;
			e.getWhoClicked().sendMessage("Facing updated to " + this.facing.toString().toLowerCase(Locale.ROOT) + ".");
			updateAndCancel(e);
		} else if (e.getSlot() == Slots.EAST) {
			this.facing = BlockFace.EAST;
			e.getWhoClicked().sendMessage("Facing updated to " + this.facing.toString().toLowerCase(Locale.ROOT) + ".");
			updateAndCancel(e);
		} else if (e.getSlot() == Slots.SOUTH) {
			this.facing = BlockFace.SOUTH;
			e.getWhoClicked().sendMessage("Facing updated to " + this.facing.toString().toLowerCase(Locale.ROOT) + ".");
			updateAndCancel(e);
		} else if (e.getSlot() == Slots.WEST) {
			this.facing = BlockFace.WEST;
			e.getWhoClicked().sendMessage("Facing updated to " + this.facing.toString().toLowerCase(Locale.ROOT) + ".");
			updateAndCancel(e);
		}
	}

	@Override
	public boolean isClickableSlot(int slot) {
		return slot >= 9 * 3 || slot == Slots.NORTH || slot == Slots.EAST || slot == Slots.SOUTH || slot == Slots.WEST;
	}

	static int[] slots = new int[27];
	static {
		for (int i = 0; i < 27; i++) {
			slots[i] = 9 * 3 + i;
		}
	}

	@Override
	protected int[] getOutputSlots() {
		return slots;
	}

	@Override
	protected int[] getInputSlots() {
		return slots;
	}

	@Override
	protected void dropItems() {
		Location dropLoc = location.clone().add(0.5, 0.5, 0.5);
		for (int i = 9 * 3; i < inventory.getSize(); i++) {
			if (inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR) {
				location.getWorld().dropItem(dropLoc, inventory.getItem(i));
			}
		}
	}

	@Override
	public void handleShiftClick(InventoryClickEvent e) {
		if (e.getSlot() == Slots.NORTH || e.getSlot() == Slots.EAST || e.getSlot() == Slots.SOUTH || e.getSlot() == Slots.WEST) {
			updateAndCancel(e);
			return;
		}
		Inventory inv = e.getClickedInventory();
		org.bukkit.inventory.ItemStack clicked = e.getCurrentItem();
		if (isEmpty(clicked)) return;
		else {
			if (inv == inventory) {
				vanillaInvInsert(e.getView().getBottomInventory(), clicked);
			} else {
				boolean hotbar = e.getSlot() >= 0 && e.getSlot() < 9;
				attemptMerge(inventory, clicked, 9 * 3, inventory.getSize());
				if (!isEmpty(clicked)) {
					if (hotbar) attemptMerge(e.getClickedInventory(), clicked, 9, 36);
					else attemptMerge(e.getClickedInventory(), clicked, 0, 9);
				}
			}
			updateAndCancel(e);
		}
	}

	@Override
	public void onPlaced(Player placer) {
		this.playerId = ((CraftPlayer) placer).getHandle().getProfile();
	}

	/**
	 * Copy of {@link PlayerInteractManager#breakBlock(BlockPosition)}
	 * @param blockposition The location of the block to break.
	 * @return If a block was successfully broken.
	 */
	public static boolean breakBlock(World world, BlockPosition blockposition, EntityPlayer player) {
		IBlockData iblockdata = world.getType(blockposition);
		org.bukkit.block.Block bblock = CraftBlock.at(world, blockposition);
		BlockBreakEvent event = new BlockBreakEvent(bblock, player.getBukkitEntity());
		event.setCancelled(iblockdata.getBlock().strength < 0);
		IBlockData nmsData = world.getType(blockposition);
		net.minecraft.server.v1_15_R1.Block nmsBlock = nmsData.getBlock();
		net.minecraft.server.v1_15_R1.ItemStack itemstack = player.getEquipment(EnumItemSlot.MAINHAND);
		if (nmsBlock != null && !event.isCancelled() && player.hasBlock(nmsBlock.getBlockData())) {
			event.setExpToDrop(nmsBlock.getExpDrop(nmsData, world, blockposition, itemstack));
		}

		world.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) { return false; }

		iblockdata = world.getType(blockposition);
		if (iblockdata.isAir()) {
			return false;
		} else {
			TileEntity tileentity = world.getTileEntity(blockposition);
			net.minecraft.server.v1_15_R1.Block block = iblockdata.getBlock();
			if ((block instanceof BlockCommand || block instanceof BlockStructure || block instanceof BlockJigsaw) && !player.isCreativeAndOp()) {
				world.notify(blockposition, iblockdata, iblockdata, 3);
				return false;
			} else if (player.a(world, blockposition, EnumGamemode.SURVIVAL)) {
				return false;
			} else {
				org.bukkit.block.BlockState state = bblock.getState();
				world.captureDrops = new ArrayList<>();
				block.a(world, blockposition, iblockdata, player);
				boolean flag = world.a(blockposition, false);
				if (flag) {
					block.postBreak(world, blockposition, iblockdata);
				}

				itemstack = player.getItemInMainHand();
				net.minecraft.server.v1_15_R1.ItemStack itemstack1 = itemstack.cloneItemStack();
				itemstack.a(world, iblockdata, blockposition, player);
				if (flag && event.isDropItems()) {
					block.a(world, player, blockposition, iblockdata, tileentity, itemstack1);
				}

				if (event.isDropItems()) {
					CraftEventFactory.handleBlockDropItemEvent(bblock, state, player, world.captureDrops);
				}

				world.captureDrops = null;

				return true;
			}
		}
	}

}

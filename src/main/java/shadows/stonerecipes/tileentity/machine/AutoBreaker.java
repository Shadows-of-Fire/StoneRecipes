package shadows.stonerecipes.tileentity.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_15_R1.event.CraftEventFactory;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.minecraft.server.v1_15_R1.BlockCommand;
import net.minecraft.server.v1_15_R1.BlockJigsaw;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.BlockStructure;
import net.minecraft.server.v1_15_R1.EntityHuman;
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
import shadows.stonerecipes.util.PluginFile;
import shadows.stonerecipes.util.Slots;
import shadows.stonerecipes.util.WorldPos;

public class AutoBreaker extends PoweredMachine {

	protected BlockFace facing = BlockFace.NORTH;

	@SuppressWarnings("deprecation")
	public AutoBreaker(WorldPos pos) {
		super("auto_breaker", "Auto Breaker", "config.yml", pos);
		this.start_progress = 70;
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
	}

	@Override
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void read(PluginFile file) {
		super.read(file);
		if (file.isList(pos + ".inv")) {
			List<org.bukkit.inventory.ItemStack> content = (List<org.bukkit.inventory.ItemStack>) file.getList(pos + ".inv");
			for (int i = 0; i < 54; i++) {
				if (content.get(i) != null) {
					inventory.setItemInternal(i, content.get(i));
				}
			}
		} else StoneRecipes.INSTANCE.getLogger().info("Failed to read inventory for a " + name + " at " + pos.translated());
	}

	@Override
	public void write(PluginFile file) {
		super.write(file);
		file.set(pos + ".inv", Arrays.asList(inventory.getContents()));
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
				breakBlock(new BlockPosition(toBreak.getX(), toBreak.getY(), toBreak.getZ()));
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

	@Override
	public void onSlotClick(InventoryClickEvent e) {

	}

	@Override
	public boolean isClickableSlot(int slot) {
		return slot < 3 || slot >= 9 && slot < 12 || slot >= 18 && slot < 21 || slot == Slots.AUTOCRAFTER_OUTPUT || slot >= 9 * 4 || slot == Slots.AUTOCRAFTER_REFRESH;
	}

	static int[] slots = new int[18];
	static {
		for (int i = 0; i < 18; i++) {
			slots[i] = 9 * 4 + i;
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
		for (int i = 0; i < inventory.getSize(); i++) {
			if (i != Slots.GUI_TEX_SLOT && i != Slots.AUTOCRAFTER_REFRESH && inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR) {
				location.getWorld().dropItem(dropLoc, inventory.getItem(i));
			}
		}
	}

	@Override
	public void handleShiftClick(InventoryClickEvent e) {
		Inventory inv = e.getClickedInventory();
		org.bukkit.inventory.ItemStack clicked = e.getCurrentItem();
		if (isEmpty(clicked)) return;
		else {
			if (inv == inventory) {
				vanillaInvInsert(e.getView().getBottomInventory(), clicked);
			} else {
				boolean hotbar = e.getSlot() >= 0 && e.getSlot() < 9;
				attemptMerge(inventory, clicked, 9 * 4, inventory.getSize());
				if (!isEmpty(clicked)) {
					if (hotbar) attemptMerge(e.getClickedInventory(), clicked, 9, 36);
					else attemptMerge(e.getClickedInventory(), clicked, 0, 9);
				}
			}
			updateAndCancel(e);
		}
	}

	/**
	 * Copy of {@link PlayerInteractManager#breakBlock(BlockPosition)}
	 * @param blockposition The location of the block to break.
	 * @return If a block was successfully broken.
	 */
	public boolean breakBlock(BlockPosition blockposition) {
		World world = ((CraftWorld) this.location.getWorld()).getHandle();
		FakePlayer player = new FakePlayer((WorldServer) world);
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
			} else if (player.a((World) world, (BlockPosition) blockposition, EnumGamemode.SURVIVAL)) {
				return false;
			} else {
				org.bukkit.block.BlockState state = bblock.getState();
				world.captureDrops = new ArrayList<>();
				block.a((World) world, (BlockPosition) blockposition, (IBlockData) iblockdata, (EntityHuman) player);
				boolean flag = world.a(blockposition, false);
				if (flag) {
					block.postBreak(world, blockposition, iblockdata);
				}

				itemstack = player.getItemInMainHand();
				net.minecraft.server.v1_15_R1.ItemStack itemstack1 = itemstack.cloneItemStack();
				boolean flag1 = player.hasBlock(iblockdata);
				itemstack.a(world, iblockdata, blockposition, player);
				if (flag && flag1 && event.isDropItems()) {
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

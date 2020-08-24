package shadows.stonerecipes.util;

import java.io.File;
import java.util.OptionalInt;

import javax.annotation.Nullable;

import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.v1_16_R2.AdvancementDataPlayer;
import net.minecraft.server.v1_16_R2.DamageSource;
import net.minecraft.server.v1_16_R2.DimensionManager;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.EnumGamemode;
import net.minecraft.server.v1_16_R2.IChatBaseComponent;
import net.minecraft.server.v1_16_R2.ITileInventory;
import net.minecraft.server.v1_16_R2.PacketPlayInSettings;
import net.minecraft.server.v1_16_R2.PlayerInteractManager;
import net.minecraft.server.v1_16_R2.Statistic;
import net.minecraft.server.v1_16_R2.Vec3D;
import net.minecraft.server.v1_16_R2.WorldServer;

public class FakePlayer extends EntityPlayer {

	public FakePlayer(WorldServer world, GameProfile id) {
		super(world.getMinecraftServer(), world, id, new PlayerInteractManager(world));
		this.playerInteractManager.setGameMode(EnumGamemode.SURVIVAL);
	}

	@Override
	public Vec3D getPositionVector() {
		return new Vec3D(0.0D, 0.0D, 0.0D);
	}

	@Override
	public void tick() {
	}

	@Override
	public void die(DamageSource damagesource) {
	}

	@Override
	public Entity a(DimensionManager dimensionmanager, TeleportCause cause) {
		return this;
	}

	@Override
	public OptionalInt openContainer(@Nullable ITileInventory itileinventory) {
		return OptionalInt.empty();
	}

	@Override
	public void a(PacketPlayInSettings packetplayinsettings) {
	}

	@Override
	public void sendMessage(IChatBaseComponent ichatbasecomponent) {
	}

	@Override
	public void a(IChatBaseComponent ichatbasecomponent, boolean flag) {
	}

	@Override
	public void a(Statistic<?> statistic, int i) {
	}

	@Override
	public void a(Statistic<?> statistic) {
	}

	@Override
	public boolean isInvulnerable(DamageSource damagesource) {
		return true;
	}

	@Override
	public boolean p(boolean flag) {
		return true;
	}

	AdvancementDataPlayer data;

	@Override
	public AdvancementDataPlayer getAdvancementData() {
		if (data == null) {
			File file = new File(this.server.getWorldServer(DimensionManager.OVERWORLD).getDataManager().getDirectory(), "advancements");
			File file1 = new File(file, "fake.json");
			data = new AdvancementDataPlayer(this.server, file1, this);
		}
		return data;
	}

}

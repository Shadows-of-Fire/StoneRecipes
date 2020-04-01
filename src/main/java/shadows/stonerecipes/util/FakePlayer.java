package shadows.stonerecipes.util;

import java.io.File;
import java.util.OptionalInt;

import javax.annotation.Nullable;

import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.v1_15_R1.AdvancementDataPlayer;
import net.minecraft.server.v1_15_R1.DamageSource;
import net.minecraft.server.v1_15_R1.DimensionManager;
import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.EnumGamemode;
import net.minecraft.server.v1_15_R1.IChatBaseComponent;
import net.minecraft.server.v1_15_R1.ITileInventory;
import net.minecraft.server.v1_15_R1.PacketPlayInSettings;
import net.minecraft.server.v1_15_R1.PlayerInteractManager;
import net.minecraft.server.v1_15_R1.Statistic;
import net.minecraft.server.v1_15_R1.Vec3D;
import net.minecraft.server.v1_15_R1.WorldServer;

public class FakePlayer extends EntityPlayer {

	public FakePlayer(WorldServer world, GameProfile id) {
		super(world.getMinecraftServer(), world, id, new PlayerInteractManager(world));
		this.playerInteractManager.setGameMode(EnumGamemode.SURVIVAL);
	}

	public Vec3D getPositionVector() {
		return new Vec3D(0.0D, 0.0D, 0.0D);
	}

	public void tick() {
	}

	public void die(DamageSource damagesource) {
	}

	public Entity a(DimensionManager dimensionmanager, TeleportCause cause) {
		return this;
	}

	public OptionalInt openContainer(@Nullable ITileInventory itileinventory) {
		return OptionalInt.empty();
	}

	public void a(PacketPlayInSettings packetplayinsettings) {
	}

	public void sendMessage(IChatBaseComponent ichatbasecomponent) {
	}

	public void a(IChatBaseComponent ichatbasecomponent, boolean flag) {
	}

	public void a(Statistic<?> statistic, int i) {
	}

	public void a(Statistic<?> statistic) {
	}

	public boolean isInvulnerable(DamageSource damagesource) {
		return true;
	}

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

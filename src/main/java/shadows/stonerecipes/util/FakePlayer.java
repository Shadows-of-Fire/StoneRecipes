package shadows.stonerecipes.util;

import java.io.File;
import java.util.OptionalInt;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.v1_16_R2.AdvancementDataPlayer;
import net.minecraft.server.v1_16_R2.DamageSource;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.EnumGamemode;
import net.minecraft.server.v1_16_R2.IChatBaseComponent;
import net.minecraft.server.v1_16_R2.ITileInventory;
import net.minecraft.server.v1_16_R2.PacketPlayInSettings;
import net.minecraft.server.v1_16_R2.PlayerInteractManager;
import net.minecraft.server.v1_16_R2.PlayerList;
import net.minecraft.server.v1_16_R2.SavedFile;
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
	public OptionalInt openContainer(@Nullable ITileInventory itileinventory) {
		return OptionalInt.empty();
	}

	@Override
	public void a(PacketPlayInSettings packetplayinsettings) {
	}

	@Override
	public void sendMessage(IChatBaseComponent ichatbasecomponent, UUID uuid) {
	}

	@Override
	public void sendMessage(IChatBaseComponent[] ichatbasecomponent) {
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

	AdvancementDataPlayer data;

	@Override
	public AdvancementDataPlayer getAdvancementData() {
		if (data == null) {
			data = genAdvancementData();
		}
		return data;
	}

	public AdvancementDataPlayer genAdvancementData() {
		PlayerList list = this.server.getPlayerList();
		AdvancementDataPlayer advancementdataplayer = this.getAdvancementData();
		if (advancementdataplayer == null) {
			File file = this.server.a(SavedFile.ADVANCEMENTS).toFile();
			File file1 = new File(file, "fake.json");
			advancementdataplayer = new AdvancementDataPlayer(this.server.getDataFixer(), list, this.server.getAdvancementData(), file1, this);
		}
		advancementdataplayer.a(this);
		return advancementdataplayer;
	}

}

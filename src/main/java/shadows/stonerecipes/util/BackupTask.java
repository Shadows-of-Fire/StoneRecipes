package shadows.stonerecipes.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.IOUtils;
import org.bukkit.craftbukkit.v1_16_R2.command.ServerCommandSender;

import shadows.stonerecipes.StoneRecipes;

public class BackupTask implements Runnable, CommandExecutor {

	@Override
	public void run() {
		backup();
	}

	public static synchronized void backup() {
		File dataFolder = new File(StoneRecipes.INSTANCE.getDataFolder(), "/data");
		if (!dataFolder.exists()) StoneRecipes.INSTANCE.getLogger().info("Failed to find data folder for backup!");
		else {
			File backupFolder = new File(StoneRecipes.INSTANCE.getDataFolder(), "/backups");
			if (!backupFolder.exists()) backupFolder.mkdir();
			File backup = new File(backupFolder, Instant.now().toString().replace(':', '_') + ".zip");
			try {
				backup.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			try (FileOutputStream fos = new FileOutputStream(backup)) {
				ZipOutputStream zos = new ZipOutputStream(fos);
				for (File f : dataFolder.listFiles()) {
					if (f.isDirectory()) {
						for (File f2 : f.listFiles()) {
							zos.putNextEntry(new ZipEntry(f2.getName()));
							FileInputStream fis = new FileInputStream(f2);
							byte[] bytes = IOUtils.toByteArray(fis);
							fis.close();
							zos.write(bytes, 0, bytes.length);
							zos.closeEntry();
						}
					} else {
						zos.putNextEntry(new ZipEntry(f.getName()));
						FileInputStream fis = new FileInputStream(f);
						byte[] bytes = IOUtils.toByteArray(fis);
						fis.close();
						zos.write(bytes, 0, bytes.length);
						zos.closeEntry();
					}
				}
				zos.close();
				StoneRecipes.INSTANCE.getLogger().info("Created a backup of ./StoneRecipes/data at ./StoneRecipes/backups/" + backup.getName());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof ServerCommandSender) backup();
		return true;
	}

}

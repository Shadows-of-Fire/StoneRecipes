package shadows.stonerecipes.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * An automatically loaded yaml configuration file.  Uses 2 space indents.
 */
public class PluginFile extends YamlConfiguration {

	private File file;
	private String defaults;
	private JavaPlugin plugin;

	/**
	 * Creates new PluginFile, without defaults
	 *
	 * @param plugin   - Your plugin
	 * @param fileName - Name of the file
	 */
	public PluginFile(JavaPlugin plugin, String fileName) {
		this(plugin, fileName, null);
	}

	/**
	 * Creates new PluginFile, with defaults
	 *
	 * @param plugin       - Your plugin
	 * @param fileName     - Name of the file
	 * @param defaultsName - Name of the defaults
	 */
	public PluginFile(JavaPlugin plugin, String fileName, String defaultsName) {
		this.plugin = plugin;
		this.defaults = defaultsName;
		this.file = new File(plugin.getDataFolder(), fileName);
		reload();
	}

	/**
	 * Reload configuration
	 */
	public void reload() {

		if (!file.exists()) {

			try {
				file.getParentFile().setWritable(true);
				file.getParentFile().mkdirs();
				file.createNewFile();

			} catch (IOException exception) {
				exception.printStackTrace();
				plugin.getLogger().severe("Error while creating file " + file.getName());
			}

		}

		try {
			load(file);

			if (defaults != null) {
				InputStreamReader reader = new InputStreamReader(plugin.getResource(defaults));
				FileConfiguration defaultsConfig = YamlConfiguration.loadConfiguration(reader);

				setDefaults(defaultsConfig);
				options().copyDefaults(true);

				reader.close();
				save();
			}

		} catch (Exception exception) {
			exception.printStackTrace();
			plugin.getLogger().severe("Error while loading file " + file.getName());
		}

	}

	/**
	 * Save configuration
	 */
	public void save() {
		try {
			options().indent(2);
			save(file);
		} catch (Exception exception) {
			exception.printStackTrace();
			plugin.getLogger().severe("Error while saving file " + file.getName());
		}

	}

}
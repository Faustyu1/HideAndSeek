package io.github.hideAndSeek;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.GameMode;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class HideAndSeek extends JavaPlugin {
    private static HideAndSeek instance;
    private FileConfiguration config;
    private Location seekersSpawn;
    private Location hidersSpawn;
    private Location endSpawn;
    private List<String> jury;
    private GameManager gameManager;
    private FileConfiguration languageConfig;
    private String currentLanguage;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();
        
        // Load language
        currentLanguage = config.getString("settings.language", "en_us");
        loadLanguageConfig();
        
        // Load jury from config
        loadJury();
        
        // Load spawn locations
        loadSpawnLocations();
        
        // Initialize game manager
        gameManager = new GameManager(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(new GameEvents(this), this);
        
        // Register commands
        getCommand("hideandseek").setExecutor(new GameCommands(this));
    }

    private void loadLanguageConfig() {
        // Create lang directory if it doesn't exist
        File langDir = new File(getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
            saveResource("lang/en_us.yml", false);
            saveResource("lang/ru_ru.yml", false);
        }

        // Load the language file
        File langFile = new File(getDataFolder(), "lang/" + currentLanguage + ".yml");
        if (!langFile.exists()) {
            getLogger().warning("Language file " + currentLanguage + " not found, falling back to en_us");
            currentLanguage = "en_us";
            langFile = new File(getDataFolder(), "lang/en_us.yml");
            if (!langFile.exists()) {
                saveResource("lang/en_us.yml", false);
            }
        }

        languageConfig = YamlConfiguration.loadConfiguration(langFile);

        // Load default values from embedded resource
        InputStream defaultStream = getResource("lang/" + currentLanguage + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream)
            );
            languageConfig.setDefaults(defaultConfig);
        }
    }

    public FileConfiguration getLanguageConfig() {
        if (languageConfig == null) {
            loadLanguageConfig();
        }
        return languageConfig;
    }

    public void setLanguage(String language) {
        if (!language.equals(currentLanguage)) {
            currentLanguage = language;
            config.set("settings.language", language);
            saveConfig();
            loadLanguageConfig();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        config = getConfig();
        loadLanguageConfig();
        loadJury();
        loadSpawnLocations();
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    private void loadJury() {
        jury = config.getStringList("jury");
    }

    private void loadSpawnLocations() {
        if (config.contains("locations.seekers.world")) {
            World seekersWorld = Bukkit.getWorld(config.getString("locations.seekers.world"));
            if (seekersWorld != null) {
                seekersSpawn = new Location(
                    seekersWorld,
                    config.getDouble("locations.seekers.x"),
                    config.getDouble("locations.seekers.y"),
                    config.getDouble("locations.seekers.z")
                );
                
                if (config.contains("locations.seekers.yaw")) {
                    seekersSpawn.setYaw((float) config.getDouble("locations.seekers.yaw"));
                    seekersSpawn.setPitch((float) config.getDouble("locations.seekers.pitch"));
                }
            }
        }

        if (config.contains("locations.hiders.world")) {
            World hidersWorld = Bukkit.getWorld(config.getString("locations.hiders.world"));
            if (hidersWorld != null) {
                hidersSpawn = new Location(
                    hidersWorld,
                    config.getDouble("locations.hiders.x"),
                    config.getDouble("locations.hiders.y"),
                    config.getDouble("locations.hiders.z")
                );
                
                if (config.contains("locations.hiders.yaw")) {
                    hidersSpawn.setYaw((float) config.getDouble("locations.hiders.yaw"));
                    hidersSpawn.setPitch((float) config.getDouble("locations.hiders.pitch"));
                }
            }
        }

        if (config.contains("locations.end.world")) {
            World endWorld = Bukkit.getWorld(config.getString("locations.end.world"));
            if (endWorld != null) {
                endSpawn = new Location(
                    endWorld,
                    config.getDouble("locations.end.x"),
                    config.getDouble("locations.end.y"),
                    config.getDouble("locations.end.z")
                );
                
                if (config.contains("locations.end.yaw")) {
                    endSpawn.setYaw((float) config.getDouble("locations.end.yaw"));
                    endSpawn.setPitch((float) config.getDouble("locations.end.pitch"));
                }
            }
        }
    }

    public void setSeekersSpawn(Location location) {
        this.seekersSpawn = location.clone();
        config.set("locations.seekers.world", location.getWorld().getName());
        config.set("locations.seekers.x", location.getX());
        config.set("locations.seekers.y", location.getY());
        config.set("locations.seekers.z", location.getZ());
        config.set("locations.seekers.yaw", location.getYaw());
        config.set("locations.seekers.pitch", location.getPitch());
        saveConfig();
    }

    public void setHidersSpawn(Location location) {
        this.hidersSpawn = location.clone();
        config.set("locations.hiders.world", location.getWorld().getName());
        config.set("locations.hiders.x", location.getX());
        config.set("locations.hiders.y", location.getY());
        config.set("locations.hiders.z", location.getZ());
        config.set("locations.hiders.yaw", location.getYaw());
        config.set("locations.hiders.pitch", location.getPitch());
        saveConfig();
    }

    public void setEndSpawn(Location location) {
        this.endSpawn = location.clone();
        config.set("locations.end.world", location.getWorld().getName());
        config.set("locations.end.x", location.getX());
        config.set("locations.end.y", location.getY());
        config.set("locations.end.z", location.getZ());
        config.set("locations.end.yaw", location.getYaw());
        config.set("locations.end.pitch", location.getPitch());
        saveConfig();
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.endGame();
        }
    }

    public static HideAndSeek getInstance() {
        return instance;
    }

    public List<String> getJury() {
        return jury;
    }

    public Location getSeekersSpawn() {
        return seekersSpawn != null ? seekersSpawn.clone() : null;
    }

    public Location getHidersSpawn() {
        return hidersSpawn != null ? hidersSpawn.clone() : null;
    }

    public Location getEndSpawn() {
        return endSpawn != null ? endSpawn.clone() : null;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}

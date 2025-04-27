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

public final class HideAndSeek extends JavaPlugin {
    private static HideAndSeek instance;
    private FileConfiguration config;
    private Location seekersSpawn;
    private Location hidersSpawn;
    private List<String> jury;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();
        
        // Load jury from config
        jury = config.getStringList("jury");
        
        // Load spawn locations
        loadSpawnLocations();
        
        // Initialize game manager
        gameManager = new GameManager(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(new GameEvents(this), this);
        
        // Register commands
        getCommand("hideandseek").setExecutor(new GameCommands(this));
    }

    private void loadSpawnLocations() {
        // Load seekers spawn
        if (config.contains("locations.seekers.world")) {
            World seekersWorld = Bukkit.getWorld(config.getString("locations.seekers.world"));
            if (seekersWorld != null) {
                seekersSpawn = new Location(
                    seekersWorld,
                    config.getDouble("locations.seekers.x"),
                    config.getDouble("locations.seekers.y"),
                    config.getDouble("locations.seekers.z")
                );
                
                // Загружаем углы поворота, если они есть
                if (config.contains("locations.seekers.yaw")) {
                    seekersSpawn.setYaw((float) config.getDouble("locations.seekers.yaw"));
                    seekersSpawn.setPitch((float) config.getDouble("locations.seekers.pitch"));
                }
            }
        }

        // Load hiders spawn
        if (config.contains("locations.hiders.world")) {
            World hidersWorld = Bukkit.getWorld(config.getString("locations.hiders.world"));
            if (hidersWorld != null) {
                hidersSpawn = new Location(
                    hidersWorld,
                    config.getDouble("locations.hiders.x"),
                    config.getDouble("locations.hiders.y"),
                    config.getDouble("locations.hiders.z")
                );
                
                // Загружаем углы поворота, если они есть
                if (config.contains("locations.hiders.yaw")) {
                    hidersSpawn.setYaw((float) config.getDouble("locations.hiders.yaw"));
                    hidersSpawn.setPitch((float) config.getDouble("locations.hiders.pitch"));
                }
            }
        }
    }

    public void setSeekersSpawn(Location location) {
        this.seekersSpawn = location.clone(); // Клонируем для безопасности
        // Save to config
        config.set("locations.seekers.world", location.getWorld().getName());
        config.set("locations.seekers.x", location.getX());
        config.set("locations.seekers.y", location.getY());
        config.set("locations.seekers.z", location.getZ());
        config.set("locations.seekers.yaw", location.getYaw());
        config.set("locations.seekers.pitch", location.getPitch());
        saveConfig();
    }

    public void setHidersSpawn(Location location) {
        this.hidersSpawn = location.clone(); // Клонируем для безопасности
        // Save to config
        config.set("locations.hiders.world", location.getWorld().getName());
        config.set("locations.hiders.x", location.getX());
        config.set("locations.hiders.y", location.getY());
        config.set("locations.hiders.z", location.getZ());
        config.set("locations.hiders.yaw", location.getYaw());
        config.set("locations.hiders.pitch", location.getPitch());
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

    public GameManager getGameManager() {
        return gameManager;
    }
}

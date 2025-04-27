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
        
        // Initialize game manager
        gameManager = new GameManager(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(new GameEvents(this), this);
        
        // Register commands
        getCommand("hideandseek").setExecutor(new GameCommands(this));
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
        return seekersSpawn;
    }

    public Location getHidersSpawn() {
        return hidersSpawn;
    }

    public void setSeekersSpawn(Location location) {
        this.seekersSpawn = location;
        // Save to config
        config.set("locations.seekers.world", location.getWorld().getName());
        config.set("locations.seekers.x", location.getX());
        config.set("locations.seekers.y", location.getY());
        config.set("locations.seekers.z", location.getZ());
        saveConfig();
    }

    public void setHidersSpawn(Location location) {
        this.hidersSpawn = location;
        // Save to config
        config.set("locations.hiders.world", location.getWorld().getName());
        config.set("locations.hiders.x", location.getX());
        config.set("locations.hiders.y", location.getY());
        config.set("locations.hiders.z", location.getZ());
        saveConfig();
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}

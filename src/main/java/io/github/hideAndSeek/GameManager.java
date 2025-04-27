package io.github.hideAndSeek;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;

public class GameManager {
    private final HideAndSeek plugin;
    private boolean isGameRunning = false;
    private List<Player> players = new ArrayList<>();
    private List<Player> seekers = new ArrayList<>();
    private List<Player> foundPlayers = new ArrayList<>();
    private int timeLeft;
    private GameState gameState = GameState.WAITING;
    private Team hidersTeam;
    private Team seekersTeam;
    private boolean statsDisplayed = false;
    private FileConfiguration lang;

    public GameManager(HideAndSeek plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageConfig();
        setupTeams();
    }

    private void setupTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        if (scoreboard.getTeam("hiders") != null) {
            scoreboard.getTeam("hiders").unregister();
        }
        if (scoreboard.getTeam("seekers") != null) {
            scoreboard.getTeam("seekers").unregister();
        }
        
        hidersTeam = scoreboard.registerNewTeam("hiders");
        seekersTeam = scoreboard.registerNewTeam("seekers");
        
        hidersTeam.setColor(ChatColor.GREEN);
        seekersTeam.setColor(ChatColor.RED);
        
        hidersTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        seekersTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    private ItemStack createSeekerStick() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        meta.setDisplayName(ChatColor.RED + lang.getString("messages.items.seeker_stick"));
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + lang.getString("messages.items.seeker_stick_lore"));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        stick.setItemMeta(meta);
        return stick;
    }

    private void setupSeeker(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setGlowing(false);
        player.getInventory().clear();
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
        
        // Выдаем палку искателя
        ItemStack seekerStick = createSeekerStick();
        player.getInventory().setItem(0, seekerStick);
    }

    public void startGame() {
        if (isGameRunning) {
            return;
        }

        if (plugin.getSeekersSpawn() == null || plugin.getHidersSpawn() == null) {
            Bukkit.broadcastMessage(ChatColor.RED + lang.getString("messages.errors.spawn_not_set"));
            return;
        }

        isGameRunning = true;
        gameState = GameState.STARTING;
        
        new BukkitRunnable() {
            int countdown = 10;
            
            @Override
            public void run() {
                if (countdown > 0) {
                    String message = lang.getString("messages.game.start").replace("%seconds%", String.valueOf(countdown));
                    Bukkit.broadcastMessage(ChatColor.YELLOW + message);
                    
                    if (countdown <= 5) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                    
                    countdown--;
                } else {
                    cancel();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    }
                    startHidingPhase();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startHidingPhase() {
        gameState = GameState.HIDING;
        timeLeft = plugin.getConfig().getInt("settings.hide-time");
        foundPlayers.clear();

        // Setup players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getJury().contains(player.getName())) {
                seekers.add(player);
                setupSeeker(player);
                player.teleport(plugin.getSeekersSpawn());
                seekersTeam.addEntry(player.getName());
            } else {
                players.add(player);
                setupPlayer(player);
                player.teleport(plugin.getHidersSpawn());
                hidersTeam.addEntry(player.getName());
            }
        }

        // Start timer
        new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    cancel();
                    startSeekingPhase();
                    return;
                }

                updateActionBar();
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startSeekingPhase() {
        gameState = GameState.SEEKING;
        timeLeft = plugin.getConfig().getInt("settings.seek-time");
        statsDisplayed = false;

        Bukkit.broadcastMessage(ChatColor.RED + lang.getString("messages.game.hunting_phase"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                ChatColor.RED + lang.getString("messages.game.hunting_title"),
                ChatColor.YELLOW + lang.getString("messages.game.hunting_subtitle"),
                10, 70, 20
            );
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        for (Player seeker : seekers) {
            seeker.teleport(plugin.getHidersSpawn());
            seeker.setGlowing(true);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isGameRunning) {
                    cancel();
                    return;
                }

                if (timeLeft <= 0 || players.isEmpty()) {
                    cancel();
                    if (!statsDisplayed) {
                        displayGameStats();
                        statsDisplayed = true;
                        endGame();
                    }
                    return;
                }

                int glowTime = plugin.getConfig().getInt("settings.glow-time");
                
                if (timeLeft == glowTime + 30) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + lang.getString("messages.game.glow_warning"));
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    }
                }

                if (timeLeft == glowTime) {
                    Bukkit.broadcastMessage(ChatColor.RED + lang.getString("messages.game.glow_activated"));
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
                        player.sendTitle(
                            ChatColor.RED + lang.getString("messages.game.glow_title"),
                            ChatColor.YELLOW + lang.getString("messages.game.glow_subtitle"),
                            10, 40, 10
                        );
                    }
                    for (Player player : players) {
                        player.setGlowing(true);
                    }
                }

                updateActionBar();
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void markPlayerAsFound(Player player, Player seeker) {
        if (!isGameRunning || !players.contains(player) || foundPlayers.contains(player)) {
            return;
        }

        foundPlayers.add(player);
        players.remove(player);
        
        player.setGameMode(GameMode.SPECTATOR);
        player.setGlowing(false);
        player.getInventory().clear();
        hidersTeam.removeEntry(player.getName());
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        }
        
        String foundMessage = lang.getString("messages.player.found")
            .replace("%seeker%", ChatColor.RED + seeker.getName())
            .replace("%player%", ChatColor.GREEN + player.getName());
        Bukkit.broadcastMessage(ChatColor.YELLOW + foundMessage);
        
        if (players.isEmpty() && !statsDisplayed) {
            Bukkit.broadcastMessage(ChatColor.GREEN + lang.getString("messages.game.all_players_found"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
            displayGameStats();
            statsDisplayed = true;
            endGame();
        }
    }

    private void setupPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setGlowing(false);
        player.getInventory().clear();
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255));
    }

    private void updateActionBar() {
        String timeMessage = formatTime(timeLeft);
        String message = lang.getString("messages.game.time_remaining")
            .replace("%time%", timeMessage)
            .replace("%players%", String.valueOf(players.size()));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public void endGame() {
        if (!isGameRunning) {
            return;
        }

        isGameRunning = false;
        gameState = GameState.WAITING;
        statsDisplayed = false;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.setGlowing(false);
            player.getInventory().clear();
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(PotionEffectType.SATURATION);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.SPEED);
            
            hidersTeam.removeEntry(player.getName());
            seekersTeam.removeEntry(player.getName());
            
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team defaultTeam = scoreboard.getTeam("default");
            if (defaultTeam == null) {
                defaultTeam = scoreboard.registerNewTeam("default");
                defaultTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            }
            defaultTeam.addEntry(player.getName());
            
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f);
        }

        players.clear();
        seekers.clear();
        foundPlayers.clear();
        
        Bukkit.broadcastMessage(ChatColor.GREEN + lang.getString("messages.game.game_over"));
    }

    public boolean isGameRunning() {
        return isGameRunning;
    }

    public boolean isSeekingPhase() {
        return gameState == GameState.SEEKING;
    }

    public boolean isSeeker(Player player) {
        return seekers.contains(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
        seekers.remove(player);
        foundPlayers.remove(player);
    }

    private void displayGameStats() {
        if (!isGameRunning || statsDisplayed) {
            return;
        }

        int totalPlayers = players.size() + foundPlayers.size();
        int foundCount = foundPlayers.size();
        
        Bukkit.broadcastMessage(ChatColor.GOLD + lang.getString("messages.game.stats_header"));
        
        String foundMessage = lang.getString("messages.game.stats_found")
            .replace("%found%", String.valueOf(foundCount))
            .replace("%total%", String.valueOf(totalPlayers));
        Bukkit.broadcastMessage(ChatColor.YELLOW + foundMessage);
        
        if (!players.isEmpty()) {
            StringBuilder survivors = new StringBuilder();
            for (Player player : players) {
                if (survivors.length() > 0) {
                    survivors.append(", ");
                }
                survivors.append(ChatColor.GREEN + player.getName());
            }
            String survivorsMessage = lang.getString("messages.game.stats_survivors")
                .replace("%players%", survivors.toString());
            Bukkit.broadcastMessage(ChatColor.YELLOW + survivorsMessage);
        }
        
        Bukkit.broadcastMessage(ChatColor.GOLD + lang.getString("messages.game.stats_footer"));
    }

    enum GameState {
        WAITING,
        STARTING,
        HIDING,
        SEEKING
    }
} 
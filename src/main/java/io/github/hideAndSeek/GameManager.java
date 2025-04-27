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

    public GameManager(HideAndSeek plugin) {
        this.plugin = plugin;
        setupTeams();
    }

    private void setupTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // Удаляем старые команды если они существуют
        if (scoreboard.getTeam("hiders") != null) {
            scoreboard.getTeam("hiders").unregister();
        }
        if (scoreboard.getTeam("seekers") != null) {
            scoreboard.getTeam("seekers").unregister();
        }
        
        // Создаем новые команды
        hidersTeam = scoreboard.registerNewTeam("hiders");
        seekersTeam = scoreboard.registerNewTeam("seekers");
        
        // Настраиваем цвета и видимость
        hidersTeam.setColor(ChatColor.GREEN);
        seekersTeam.setColor(ChatColor.RED);
        
        // Скрываем ники
        hidersTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        seekersTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    private ItemStack createSeekerStick() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Палка искателя");
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Используйте для поиска игроков");
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
            Bukkit.broadcastMessage(ChatColor.RED + "Точки спавна не установлены!");
            return;
        }

        isGameRunning = true;
        gameState = GameState.STARTING;
        
        // Start countdown
        new BukkitRunnable() {
            int countdown = 10;
            
            @Override
            public void run() {
                if (countdown > 0) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Игра начнется через " + countdown + " секунд!");
                    
                    // Звук обратного отсчета
                    if (countdown <= 5) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                    
                    countdown--;
                } else {
                    cancel();
                    // Звук начала игры
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

        // Announce hunting phase
        Bukkit.broadcastMessage(ChatColor.RED + "§l⚔ Жюри вышли на охоту! ⚔");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                ChatColor.RED + "⚔ Охота началась! ⚔",
                ChatColor.YELLOW + "Жюри вышли на поиски игроков",
                10, 70, 20
            );
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        // Release seekers
        for (Player seeker : seekers) {
            seeker.teleport(plugin.getHidersSpawn());
            seeker.setGlowing(true);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    cancel();
                    if (!statsDisplayed) {
                        displayGameStats();
                        statsDisplayed = true;
                    }
                    endGame();
                    return;
                }
                
                if (players.isEmpty()) {
                    cancel();
                    if (!statsDisplayed) {
                        displayGameStats();
                        statsDisplayed = true;
                    }
                    endGame();
                    return;
                }

                int glowTime = plugin.getConfig().getInt("settings.glow-time");
                
                // Предупреждение за минуту до подсветки
                if (timeLeft == glowTime + 30) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "⚠ До включения подсветки игроков осталась 1 минута!");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    }
                }

                // Включение подсветки
                if (timeLeft == glowTime) {
                    Bukkit.broadcastMessage(ChatColor.RED + "⚠ Внимание! " + ChatColor.YELLOW + "Все игроки подсвечены!");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
                        player.sendTitle(
                            ChatColor.RED + "⚠ Подсветка активирована!",
                            ChatColor.YELLOW + "Все игроки видны",
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
        if (!players.contains(player) || foundPlayers.contains(player)) {
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
        
        // Новое сообщение с информацией о том, кто кого нашел
        Bukkit.broadcastMessage(ChatColor.RED + seeker.getName() + ChatColor.YELLOW + " нашел " + 
                              ChatColor.GREEN + player.getName() + ChatColor.YELLOW + "!");
        
        if (players.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GREEN + "Все игроки найдены! Игра окончена!");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
            if (!statsDisplayed) {
                displayGameStats();
                statsDisplayed = true;
            }
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
        String playersMessage = ChatColor.GREEN + "Игроков осталось: " + ChatColor.WHITE + players.size();
        String message = ChatColor.YELLOW + "Время: " + ChatColor.WHITE + timeMessage + " " + playersMessage;
        
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
            
            // Удаляем игроков из команд и восстанавливаем видимость ников
            hidersTeam.removeEntry(player.getName());
            seekersTeam.removeEntry(player.getName());
            
            // Добавляем всех в дефолтную команду, чтобы ники снова стали видимыми
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
        
        Bukkit.broadcastMessage(ChatColor.GREEN + "Игра завершена!");
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
        int totalPlayers = players.size() + foundPlayers.size();
        int foundCount = foundPlayers.size();
        
        Bukkit.broadcastMessage(ChatColor.GOLD + "═══════ Результаты игры ═══════");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Найдено игроков: " + ChatColor.WHITE + foundCount + 
                              ChatColor.YELLOW + " из " + ChatColor.WHITE + totalPlayers);
        
        if (!players.isEmpty()) {
            StringBuilder survivors = new StringBuilder();
            for (Player player : players) {
                if (survivors.length() > 0) {
                    survivors.append(", ");
                }
                survivors.append(ChatColor.GREEN + player.getName());
            }
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Выжившие: " + survivors.toString());
        }
        
        Bukkit.broadcastMessage(ChatColor.GOLD + "═══════════════════════════");
    }

    enum GameState {
        WAITING,
        STARTING,
        HIDING,
        SEEKING
    }
} 
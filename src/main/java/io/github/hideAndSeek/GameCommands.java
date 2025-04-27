package io.github.hideAndSeek;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class GameCommands implements CommandExecutor {
    private final HideAndSeek plugin;
    private final FileConfiguration lang;

    public GameCommands(HideAndSeek plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + lang.getString("messages.errors.player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("hideandseek.admin")) {
            player.sendMessage(ChatColor.RED + lang.getString("messages.errors.no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (plugin.getGameManager().isGameRunning()) {
                    player.sendMessage(ChatColor.RED + lang.getString("messages.errors.game_in_progress"));
                    return true;
                }
                plugin.getGameManager().startGame();
                return true;

            case "setseek":
                if (plugin.getGameManager().isGameRunning()) {
                    player.sendMessage(ChatColor.RED + lang.getString("messages.errors.cant_change_spawn"));
                    return true;
                }
                plugin.setSeekersSpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + lang.getString("messages.commands.spawn_set_seekers"));
                return true;

            case "sethiders":
                if (plugin.getGameManager().isGameRunning()) {
                    player.sendMessage(ChatColor.RED + lang.getString("messages.errors.cant_change_spawn"));
                    return true;
                }
                plugin.setHidersSpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + lang.getString("messages.commands.spawn_set_hiders"));
                return true;

            case "language":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.YELLOW + "Current language: " + plugin.getCurrentLanguage());
                    player.sendMessage(ChatColor.YELLOW + "Available languages: en_us, ru_ru");
                    player.sendMessage(ChatColor.YELLOW + "Usage: /hideandseek language <en_us|ru_ru>");
                    return true;
                }
                String newLang = args[1].toLowerCase();
                if (!newLang.equals("en_us") && !newLang.equals("ru_ru")) {
                    player.sendMessage(ChatColor.RED + "Invalid language! Available: en_us, ru_ru");
                    return true;
                }
                plugin.setLanguage(newLang);
                player.sendMessage(ChatColor.GREEN + "Language changed to: " + newLang);
                return true;

            default:
                sendUsage(player);
                return true;
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.YELLOW + lang.getString("messages.commands.usage"));
        player.sendMessage(ChatColor.YELLOW + "/hideandseek start " + ChatColor.WHITE + "- " + 
                         lang.getString("messages.commands.start"));
        player.sendMessage(ChatColor.YELLOW + "/hideandseek setseek " + ChatColor.WHITE + "- " + 
                         lang.getString("messages.commands.set_seekers"));
        player.sendMessage(ChatColor.YELLOW + "/hideandseek sethiders " + ChatColor.WHITE + "- " + 
                         lang.getString("messages.commands.set_hiders"));
        player.sendMessage(ChatColor.YELLOW + "/hideandseek language <en_us|ru_ru> " + ChatColor.WHITE + 
                         "- Change language");
    }
} 
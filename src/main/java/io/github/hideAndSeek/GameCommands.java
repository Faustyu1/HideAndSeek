package io.github.hideAndSeek;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class GameCommands implements CommandExecutor {
    private final HideAndSeek plugin;

    public GameCommands(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда может быть использована только игроком!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("hideandseek.admin")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды!");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (plugin.getGameManager().isGameRunning()) {
                    player.sendMessage(ChatColor.RED + "Игра уже запущена!");
                    return true;
                }
                plugin.getGameManager().startGame();
                return true;

            case "setseek":
                if (plugin.getGameManager().isGameRunning()) {
                    player.sendMessage(ChatColor.RED + "Нельзя изменять точки спавна во время игры!");
                    return true;
                }
                plugin.setSeekersSpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Точка спавна для жюри установлена!");
                return true;

            case "sethiders":
                if (plugin.getGameManager().isGameRunning()) {
                    player.sendMessage(ChatColor.RED + "Нельзя изменять точки спавна во время игры!");
                    return true;
                }
                plugin.setHidersSpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Точка спавна для игроков установлена!");
                return true;

            default:
                sendUsage(player);
                return true;
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Использование команд:");
        player.sendMessage(ChatColor.YELLOW + "/hideandseek start " + ChatColor.WHITE + "- Начать игру");
        player.sendMessage(ChatColor.YELLOW + "/hideandseek setseek " + ChatColor.WHITE + "- Установить точку спавна для жюри");
        player.sendMessage(ChatColor.YELLOW + "/hideandseek sethiders " + ChatColor.WHITE + "- Установить точку спавна для игроков");
    }
} 
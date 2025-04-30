package io.github.hideAndSeek;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class GameEvents implements Listener {
    private final HideAndSeek plugin;
    private final FileConfiguration lang;

    public GameEvents(HideAndSeek plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageConfig();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getGameManager().isGameRunning() || !plugin.getGameManager().isSeekingPhase()) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.getGameManager().isSeeker(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.STICK) {
            return;
        }

        if (item.getItemMeta() != null && item.getItemMeta().getDisplayName().equals(
            ChatColor.RED + lang.getString("messages.items.seeker_stick"))) {
            event.setCancelled(true);
            
            // Проверяем, есть ли игроки в радиусе
            boolean found = false;
            for (Player target : player.getWorld().getPlayers()) {
                if (target != player && !plugin.getGameManager().isSeeker(target) && 
                    target.getLocation().distance(player.getLocation()) <= 5) {
                    plugin.getGameManager().markPlayerAsFound(target, player);
                    found = true;
                }
            }
            
            if (!found) {
                player.sendMessage(ChatColor.RED + lang.getString("messages.player.not_found"));
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }

        Player seeker = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isGameRunning() || !gameManager.isSeekingPhase()) {
            return;
        }

        if (!gameManager.isSeeker(seeker)) {
            return;
        }

        if (gameManager.isSeeker(target)) {
            return;
        }

        gameManager.markPlayerAsFound(target, seeker);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        if (event.getItemDrop().getItemStack().getItemMeta() != null &&
            event.getItemDrop().getItemStack().getItemMeta().getDisplayName().equals(
                ChatColor.RED + lang.getString("messages.items.seeker_stick"))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + lang.getString("messages.items.cant_drop_stick"));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (plugin.getGameManager().isGameRunning()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        if (plugin.getGameManager().isGameRunning()) {
            event.setCancelled(true);
            Player damager = (Player) event.getDamager();
            damager.sendMessage(ChatColor.RED + lang.getString("messages.errors.pvp_disabled"));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getGameManager().isGameRunning() || !plugin.getGameManager().isSeekingPhase()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (!plugin.getGameManager().isSeeker(player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() == Material.STICK && 
            clickedItem.getItemMeta() != null && clickedItem.getItemMeta().getDisplayName().equals(
                ChatColor.RED + lang.getString("messages.items.seeker_stick"))) {
            event.setCancelled(true);
        }
    }
}
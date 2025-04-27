package io.github.hideAndSeek;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.ChatColor;

public class GameEvents implements Listener {
    private final HideAndSeek plugin;

    public GameEvents(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
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
            event.getItemDrop().getItemStack().getItemMeta().getDisplayName().equals(ChatColor.RED + "Палка искателя")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Вы не можете выбросить палку искателя!");
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
            damager.sendMessage(ChatColor.RED + "PvP отключен во время игры!");
        }
    }
}
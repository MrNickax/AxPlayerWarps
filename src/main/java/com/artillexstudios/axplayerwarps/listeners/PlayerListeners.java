package com.artillexstudios.axplayerwarps.listeners;

import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListeners implements Listener {

    public PlayerListeners() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Users.create(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Users.create(event.getPlayer());
        Warp.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Users.getPlayers().remove(event.getPlayer());
    }
}

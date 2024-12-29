package org.lch.cCoinCraft.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.lch.cCoinCraft.database.PlayerDAO;

import java.util.UUID;

import static org.bukkit.Bukkit.getLogger;

public class PlayerJoinListener implements Listener {

    private final PlayerDAO playerDAO;

    public PlayerJoinListener(PlayerDAO playerDAO) {
        this.playerDAO = playerDAO;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String nickname = event.getPlayer().getName();
        getLogger().info("PlayerJoinEvent fired! " + event.getPlayer().getName());

        // DAO의 메소드만 호출
        playerDAO.insertOrUpdatePlayer(uuid, nickname);
    }
}
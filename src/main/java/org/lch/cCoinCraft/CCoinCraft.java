package org.lch.cCoinCraft;

import org.bukkit.plugin.java.JavaPlugin;
import org.lch.cCoinCraft.database.DatabaseManager;
import org.lch.cCoinCraft.database.PlayerDAO;
import org.lch.cCoinCraft.database.QueryQueue;
import org.lch.cCoinCraft.listeners.PlayerJoinListener;

import java.io.File;

public class CCoinCraft extends JavaPlugin {

    private DatabaseManager databaseManager;
    private QueryQueue queryQueue;
    private PlayerDAO playerDAO;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File dbFile = new File(getDataFolder(), "database.db");

        databaseManager = new DatabaseManager(dbFile);
        databaseManager.initDatabase();

        queryQueue = new QueryQueue();

        // DAO 생성 (DB 매니저, 큐를 주입)
        playerDAO = new PlayerDAO(databaseManager, queryQueue);

        // 리스너 등록
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(playerDAO), this);

        getLogger().info("CCoinCraft 플러그인 활성화");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeDatabase();
        }
        if (queryQueue != null) {
            queryQueue.stopQueue();
        }
        getLogger().info("CCoinCraft 플러그인 비활성화");
    }

    public PlayerDAO getPlayerDAO() {
        return playerDAO;
    }
}
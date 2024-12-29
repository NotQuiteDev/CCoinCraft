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
        // 플러그인 폴더 생성
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // DB 파일 지정
        File dbFile = new File(getDataFolder(), "database.db");

        // DatabaseManager 생성 (B 방식)
        databaseManager = new DatabaseManager(dbFile);

        // 테이블 생성 (initDatabase)
        databaseManager.initDatabase();

        // QueryQueue 초기화
        queryQueue = new QueryQueue();

        // DAO 생성
        playerDAO = new PlayerDAO(databaseManager, queryQueue);

        // 리스너 등록
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(playerDAO), this);

        getLogger().info("CCoinCraft enabled (B-mode DB Connection).");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeDatabase();
        }
        if (queryQueue != null) {
            queryQueue.stopQueue();
        }
        getLogger().info("CCoinCraft disabled.");
    }
}
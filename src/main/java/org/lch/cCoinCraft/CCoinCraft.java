package org.lch.cCoinCraft;

import org.bukkit.plugin.java.JavaPlugin;
import org.lch.cCoinCraft.database.DatabaseManager;
import org.lch.cCoinCraft.database.PlayerDAO;
import org.lch.cCoinCraft.database.QueryQueue;
import org.lch.cCoinCraft.listeners.BlockBreakListener;
import org.lch.cCoinCraft.listeners.PlayerJoinListener;
import org.lch.cCoinCraft.service.OreRewardService;

import java.io.File;

public class CCoinCraft extends JavaPlugin {

    private DatabaseManager databaseManager;
    private QueryQueue queryQueue;
    private PlayerDAO playerDAO;
    private OreRewardService oreRewardService;

    @Override
    public void onEnable() {
        // 플러그인 폴더 생성
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // config.yml이 없으면 resources/config.yml를 복사해옴
        saveDefaultConfig();
        // config 불러오기
        // (보통은 saveDefaultConfig() 후 자동으로 불러와지므로 생략 가능하지만,
        //  혹시 갱신된 내용이 있다면 reloadConfig() 호출)
        reloadConfig();


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
        oreRewardService = new OreRewardService(this, playerDAO);

        // 리스너 등록
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(playerDAO), this);
        // BlockBreakListener 등록
        getServer().getPluginManager().registerEvents(new BlockBreakListener(oreRewardService), this);

        getLogger().info("CCoinCraft 플러그인 onEnable 완료");
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
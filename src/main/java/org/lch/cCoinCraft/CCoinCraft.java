// src/main/java/org/lch/cCoinCraft/CCoinCraft.java
package org.lch.cCoinCraft;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.lch.cCoinCraft.commands.CccCommand;
import org.lch.cCoinCraft.commands.CccCommandTabCompleter;
import org.lch.cCoinCraft.database.BtcHistoryDAO;
import org.lch.cCoinCraft.database.DatabaseManager;
import org.lch.cCoinCraft.database.HistoryDAO;
import org.lch.cCoinCraft.database.PlayerDAO;
import org.lch.cCoinCraft.database.QueryQueue;
import org.lch.cCoinCraft.listeners.BlockBreakListener;
import org.lch.cCoinCraft.listeners.PlayerJoinListener;
import org.lch.cCoinCraft.service.BtcTransactionService;
import org.lch.cCoinCraft.service.CoinGeckoPriceFetcher;
import org.lch.cCoinCraft.service.OreRewardService;
import org.lch.cCoinCraft.gui.CccGui;
import org.lch.cCoinCraft.gui.CccGuiListener;

import java.io.File;

public class CCoinCraft extends JavaPlugin {

    // 기존 필드들...
    private DatabaseManager databaseManager;
    private QueryQueue queryQueue;
    private PlayerDAO playerDAO;
    private OreRewardService oreRewardService;
    private BtcHistoryDAO btcHistoryDAO;
    private HistoryDAO historyDAO;
    private CoinGeckoPriceFetcher priceFetcher;
    private static Economy economy = null;
    private BtcTransactionService transactionService;
    private static CccGuiListener guiListener; // Added to access listener methods

    // GUI 관련 필드
    private CccGui cccGui;

    @Override
    public void onEnable() {

        // Vault 연동
        if (!setupEconomy()) {
            getLogger().severe("Vault 연동에 실패했습니다. Vault 플러그인 및 Economy 플러그인을 확인하세요.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 플러그인 폴더 생성
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // config.yml 복사 및 로드
        saveDefaultConfig();
        reloadConfig();

        // DB 파일 지정
        File dbFile = new File(getDataFolder(), "database.db");

        // DatabaseManager 생성
        databaseManager = new DatabaseManager(dbFile);

        // 테이블 생성 (initDatabase)
        databaseManager.initDatabase();

        // QueryQueue 초기화
        queryQueue = new QueryQueue();

        // DAO 생성
        btcHistoryDAO = new BtcHistoryDAO(databaseManager, queryQueue);
        historyDAO = new HistoryDAO(databaseManager, queryQueue);
        playerDAO = new PlayerDAO(databaseManager, queryQueue, historyDAO);

        // CoinGeckoPriceFetcher 생성 및 시작
        priceFetcher = new CoinGeckoPriceFetcher(this);
        priceFetcher.startPriceUpdates();

        // OreRewardService 생성 시, priceFetcher도 주입
        oreRewardService = new OreRewardService(this, playerDAO, btcHistoryDAO, priceFetcher);

        // BtcTransactionService 생성 시, historyDAO도 주입
        transactionService = new BtcTransactionService(playerDAO, historyDAO, priceFetcher, this);

        // GUI 클래스 초기화
        cccGui = new CccGui(priceFetcher);

        // 리스너 등록
        guiListener = new CccGuiListener(priceFetcher, cccGui);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(playerDAO), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(oreRewardService), this);

        // 커맨드 등록
        CccCommand cccCommand = new CccCommand(playerDAO, transactionService, priceFetcher, cccGui);
        getCommand("ccc").setExecutor(cccCommand);

        // TabCompleter 등록
        this.getCommand("ccc").setTabCompleter(new CccCommandTabCompleter());

        getLogger().info("CCoinCraft 플러그인 onEnable 완료");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static Economy getEconomy() {
        return economy;
    }

    /**
     * Retrieves the CccGuiListener instance.
     *
     * @return The CccGuiListener instance.
     */
    public static CccGuiListener getGuiListener() {
        return guiListener;
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
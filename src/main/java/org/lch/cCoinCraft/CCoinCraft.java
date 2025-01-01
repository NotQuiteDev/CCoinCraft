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

import java.io.File;

public class CCoinCraft extends JavaPlugin {

    private DatabaseManager databaseManager;
    private QueryQueue queryQueue;
    private PlayerDAO playerDAO;
    private OreRewardService oreRewardService;
    private BtcHistoryDAO btcHistoryDAO;
    private HistoryDAO historyDAO;
    private CoinGeckoPriceFetcher priceFetcher; // CoinGeckoPriceFetcher 필드 추가
    private static Economy economy = null;

    @Override
    public void onEnable() {

        // Vault 연동
        if (!setupEconomy()) {
            getLogger().severe("Vault 연동에 실패했습니다. Vault 플러그인 및 Economy 플러그인을 확인하세요.");
            // 서버를 중단시키거나, 기능 제한 가능
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

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

        // DatabaseManager 생성
        databaseManager = new DatabaseManager(dbFile);

        // 테이블 생성 (initDatabase)
        databaseManager.initDatabase();

        // QueryQueue 초기화
        queryQueue = new QueryQueue();

        // DAO 생성
        btcHistoryDAO = new BtcHistoryDAO(databaseManager, queryQueue);
        historyDAO = new HistoryDAO(databaseManager, queryQueue);
        playerDAO = new PlayerDAO(databaseManager, queryQueue);

        // CoinGeckoPriceFetcher 생성 및 시작
        priceFetcher = new CoinGeckoPriceFetcher(this);
        priceFetcher.startPriceUpdates();

        // OreRewardService 생성 시, priceFetcher도 주입
        oreRewardService = new OreRewardService(this, playerDAO, btcHistoryDAO, priceFetcher);

        // BtcTransactionService 생성 시, historyDAO도 주입
        BtcTransactionService transactionService = new BtcTransactionService(playerDAO, btcHistoryDAO, historyDAO, priceFetcher,this);

        // 리스너 등록
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(playerDAO), this);
        // BlockBreakListener 등록
        getServer().getPluginManager().registerEvents(new BlockBreakListener(oreRewardService), this);

        // 커맨드 등록
        getCommand("ccc").setExecutor(new CccCommand(playerDAO, transactionService, priceFetcher)); // 수정된 부분

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
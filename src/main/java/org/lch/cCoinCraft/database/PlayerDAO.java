package org.lch.cCoinCraft.database;

import org.bukkit.util.Consumer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.bukkit.Bukkit.getLogger;

public class PlayerDAO {

    private final DatabaseManager databaseManager;
    private final QueryQueue queryQueue;

    // 지원하는 코인 목록
    private static final List<String> SUPPORTED_COINS = Arrays.asList("BTC", "ETH", "DOGE", "USDT");

    public PlayerDAO(DatabaseManager databaseManager, QueryQueue queryQueue) {
        this.databaseManager = databaseManager;
        this.queryQueue = queryQueue;
    }

    /**
     * uuid/nickname 기준으로 '없으면 INSERT, 있으면 닉네임만 UPDATE'
     */
    public void insertOrUpdatePlayer(UUID uuid, String nickname) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            getLogger().info("insertOrUpdatePlayer called for " + nickname);
            try {
                String selectSql = "SELECT nickname FROM players WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            // 닉네임이 바뀌었는지 확인
                            String dbNickname = rs.getString("nickname");
                            if (!dbNickname.equals(nickname)) {
                                String updateSql = "UPDATE players SET nickname = ? WHERE uuid = ?";
                                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                                    updatePs.setString(1, nickname);
                                    updatePs.setString(2, uuid.toString());
                                    updatePs.executeUpdate();
                                }
                            }
                        } else {
                            // 없는 플레이어면 INSERT
                            String insertSql = "INSERT INTO players (uuid, nickname) VALUES (?, ?)";
                            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                                insertPs.setString(1, uuid.toString());
                                insertPs.setString(2, nickname);
                                insertPs.executeUpdate();
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * 특정 코인의 잔고를 업데이트 (추가 또는 차감)
     *
     * @param uuid       플레이어 UUID
     * @param coinType   코인 종류 (e.g., "BTC", "ETH")
     * @param amountDelta 추가/차감할 양 (양수: 추가, 음수: 차감)
     */
    public void updateCoinBalance(UUID uuid, String coinType, double amountDelta) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            try {
                // coinType에 따라 컬럼명 결정
                String columnName = getColumnName(coinType);
                if (columnName == null) {
                    // 지원하지 않는 코인
                    getLogger().warning("Unsupported coin type: " + coinType);
                    return;
                }

                String sql = "UPDATE players SET " + columnName + " = " + columnName + " + ? WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setDouble(1, amountDelta);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * 특정 코인의 잔고를 조회
     *
     * @param uuid     플레이어 UUID
     * @param coinType 코인 종류 (e.g., "BTC", "ETH")
     * @param callback 잔고를 전달할 콜백 함수
     */
    public void getCoinBalance(UUID uuid, String coinType, Consumer<Double> callback) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            double balance = 0.0;
            String columnName = getColumnName(coinType);
            if (columnName == null) {
                callback.accept(-1.0);
                return;
            }

            String sql = "SELECT " + columnName + " FROM players WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        balance = rs.getDouble(columnName);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // 호출자에게 결과 전달
            callback.accept(balance);
        }));
    }

    /**
     * 특정 플레이어의 모든 코인 잔고를 조회
     *
     * @param uuid     플레이어 UUID
     * @param callback 잔고를 전달할 콜백 함수 (코인 심볼과 잔액의 맵)
     */
    public void getAllCoinBalances(UUID uuid, Consumer<Map<String, Double>> callback) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            Map<String, Double> coinBalances = new HashMap<>();

            try {
                // 지원하는 모든 코인의 잔액을 한 번에 조회
                StringBuilder sqlBuilder = new StringBuilder("SELECT ");
                for (int i = 0; i < SUPPORTED_COINS.size(); i++) {
                    String columnName = getColumnName(SUPPORTED_COINS.get(i));
                    if (columnName != null) {
                        sqlBuilder.append(columnName);
                        if (i < SUPPORTED_COINS.size() - 1) {
                            sqlBuilder.append(", ");
                        }
                    }
                }
                sqlBuilder.append(" FROM players WHERE uuid = ?");

                String sql = sqlBuilder.toString();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            for (String coin : SUPPORTED_COINS) {
                                String columnName = getColumnName(coin);
                                if (columnName != null) {
                                    double balance = rs.getDouble(columnName);
                                    coinBalances.put(coin.toUpperCase(), balance);
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // 호출자에게 결과 전달
            callback.accept(coinBalances);
        }));
    }

    /**
     * 코인 타입에 따라 데이터베이스 컬럼명을 반환
     *
     * @param coinType 코인 종류 (e.g., "BTC", "ETH")
     * @return 데이터베이스 컬럼명, 지원하지 않으면 null
     */
    private String getColumnName(String coinType) {
        switch (coinType.toUpperCase()) {
            case "BTC":
                return "btc_balance";
            case "ETH":
                return "eth_balance";
            case "DOGE":
                return "doge_balance";
            case "USDT":
                return "usdt_balance";
            // 필요하면 추가
            default:
                return null; // 지원 안 함
        }
    }
}
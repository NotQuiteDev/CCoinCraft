package org.lch.cCoinCraft.database;

import org.bukkit.util.Consumer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.bukkit.Bukkit.getLogger;

public class PlayerDAO {

    private final DatabaseManager databaseManager;
    private final QueryQueue queryQueue;

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

    // 예시로 광물 채굴 보상 (btc_balance 추가) 같은 메소드
    public void addBtcBalance(UUID uuid, double addAmount) {
        queryQueue.addTask(new QueryTask(databaseManager, (conn) -> {
            try {
                String updateSql = "UPDATE players SET btc_balance = btc_balance + ? WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setDouble(1, addAmount);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }

    public void updateCoinBalance(UUID uuid, String coinType, double amountDelta) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            try {
                // coinType에 따라 컬럼명 결정
                // ex) "BTC" -> "btc_balance"
                //     "ETH" -> "eth_balance"
                //     "DOGE" -> "doge_balance"
                String columnName = getColumnName(coinType);
                if (columnName == null) {
                    // 지원하지 않는 코인
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

    private String getColumnName(String coinType) {
        switch (coinType.toUpperCase()) {
            case "BTC": return "btc_balance";
            case "ETH": return "eth_balance";
            case "DOGE": return "doge_balance";
            case "USDT": return "usdt_balance";
            // 필요하면 추가
            default: return null; // 지원 안 함
        }
    }
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
    public double getBtcBalance(UUID uuid) {
        double balance = 0.0;
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT btc_balance FROM players WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        balance = rs.getDouble("btc_balance");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balance;
    }

}
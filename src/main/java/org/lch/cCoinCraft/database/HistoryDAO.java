package org.lch.cCoinCraft.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class HistoryDAO {

    private final DatabaseManager databaseManager;
    private final QueryQueue queryQueue;

    public HistoryDAO(DatabaseManager databaseManager, QueryQueue queryQueue) {
        this.databaseManager = databaseManager;
        this.queryQueue = queryQueue;
    }

    /**
     * 거래내역 INSERT
     * @param uuid      플레이어 UUID
     * @param nickname  닉네임
     * @param coinType  "BTC", "ETH" 등
     * @param amount    거래 수량 (양수면 구매, 음수면 판매 or action 구분)
     * @param action    "BUY" or "SELL"
     */
    public void insertTransaction(UUID uuid, String nickname, String coinType, double amount, String action) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            String sql = "INSERT INTO transaction_history (uuid, nickname, coin_type, amount, action) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, nickname);
                ps.setString(3, coinType);
                ps.setDouble(4, amount);
                ps.setString(5, action);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }
}
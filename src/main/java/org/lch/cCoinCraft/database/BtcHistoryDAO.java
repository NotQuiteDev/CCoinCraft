package org.lch.cCoinCraft.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BtcHistoryDAO {

    private final DatabaseManager databaseManager;
    private final QueryQueue queryQueue;

    public BtcHistoryDAO(DatabaseManager databaseManager, QueryQueue queryQueue) {
        this.databaseManager = databaseManager;
        this.queryQueue = queryQueue;
    }

    /**
     * BTC 거래 내역 INSERT
     * @param uuid      플레이어 UUID
     * @param nickname  닉네임
     * @param amount_btc    거래 수량 (양수면 구매, 음수면 판매)
     * @param reason    "BUY_BTC" or "SELL_BTC"
     */
    public void insertHistory(String uuid, String nickname, double amount_btc, String reason) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            String sql = "INSERT INTO btc_history (uuid, nickname, amount_btc, reason) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setString(2, nickname);
                ps.setDouble(3, amount_btc);
                ps.setString(4, reason);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }
}
package org.lch.cCoinCraft.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

public class BtcHistoryDAO {

    private final DatabaseManager databaseManager;
    private final QueryQueue queryQueue;

    public BtcHistoryDAO(DatabaseManager databaseManager, QueryQueue queryQueue) {
        this.databaseManager = databaseManager;
        this.queryQueue = queryQueue;
    }

    /**
     * BTC를 얻었을 때 기록을 남기는 메소드
     * @param uuid      플레이어 UUID
     * @param nickname  플레이어 현재 닉네임
     * @param amountBtc 얻은 BTC 양
     * @param reason    예: "DIAMOND_ORE", "quest_reward" 등
     */
    public void insertHistory(String uuid, String nickname, double amountBtc, String reason) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            String sql = "INSERT INTO btc_history (uuid, nickname, amount_btc, reason) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setString(2, nickname);
                ps.setDouble(3, amountBtc);
                ps.setString(4, reason);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }
}
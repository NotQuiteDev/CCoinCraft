package org.lch.cCoinCraft.database;

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
}
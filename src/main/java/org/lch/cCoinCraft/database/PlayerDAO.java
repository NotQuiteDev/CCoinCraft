package org.lch.cCoinCraft.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * 플레이어 정보를 DB에서 INSERT/SELECT/UPDATE 하는 로직만 전담하는 클래스
 */
public class PlayerDAO {

    private final DatabaseManager databaseManager;
    private final QueryQueue queryQueue;

    public PlayerDAO(DatabaseManager databaseManager, QueryQueue queryQueue) {
        this.databaseManager = databaseManager;
        this.queryQueue = queryQueue;
    }

    /**
     * uuid/nickname 기준으로 '없으면 INSERT, 있으면 닉네임만 UPDATE'
     * (플레이어가 접속했을 때 호출 가능)
     */
    public void insertOrUpdatePlayer(UUID uuid, String nickname) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            try {
                // 1) 해당 uuid가 이미 있는지 확인
                String selectSql = "SELECT nickname FROM players WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            // 존재하면 닉네임이 바뀌었는지 확인
                            String dbNickname = rs.getString("nickname");
                            if (!dbNickname.equals(nickname)) {
                                // 닉네임만 업데이트
                                String updateSql = "UPDATE players SET nickname = ? WHERE uuid = ?";
                                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                                    updatePs.setString(1, nickname);
                                    updatePs.setString(2, uuid.toString());
                                    updatePs.executeUpdate();
                                }
                            }
                        } else {
                            // 존재하지 않으면 INSERT
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
     * 예: 플레이어 코인 잔고 업데이트 (btc_balance 증가 등)
     * 이와 같이 다양한 DB 로직을 하나의 DAO에 모아놓을 수 있음
     */
    public void updatePlayerBalance(UUID uuid, double addBtc) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            try {
                String updateSql = "UPDATE players SET btc_balance = btc_balance + ? WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setDouble(1, addBtc);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * 예: 플레이어 정보 조회 (동기/비동기 처리 방안은 상황에 따라)
     * 여기서는 간단히 설명만 함
     */
    public void getPlayerData(UUID uuid) {
        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            try {
                String selectSql = "SELECT * FROM players WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            // rs에서 데이터 가져오기
                            String nickname = rs.getString("nickname");
                            double btc = rs.getDouble("btc_balance");
                            // ... 필요에 맞게 처리
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }
}
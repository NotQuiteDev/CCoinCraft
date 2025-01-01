package org.lch.cCoinCraft.database;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
    private final HistoryDAO historyDAO; // HistoryDAO 필드 추가

    // 지원하는 코인 목록
    private static final List<String> SUPPORTED_COINS = Arrays.asList("BTC", "ETH", "DOGE", "USDT");

    public PlayerDAO(DatabaseManager databaseManager, QueryQueue queryQueue, HistoryDAO historyDAO) {
        this.databaseManager = databaseManager;
        this.queryQueue = queryQueue;
        this.historyDAO = historyDAO; // 생성자에 HistoryDAO 주입
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
     * 새로운 어드민 명령어 메서드: 플레이어에게 코인 추가
     *
     * @param uuid     플레이어 UUID
     * @param coinType 코인 종류 (e.g., "BTC", "ETH")
     * @param amount   추가할 양
     */
    public void giveCoin(UUID uuid, String coinType, double amount) {
        if (amount <= 0) {
            getLogger().warning("Invalid amount to give: " + amount);
            return;
        }

        // 기존 updateCoinBalance 메소드 사용하여 코인 추가
        updateCoinBalance(uuid, coinType, amount);

        // 거래 내역 기록: GIVE
        historyDAO.insertTransaction(uuid, getPlayerNickname(uuid), coinType, amount, "GIVE");
    }

    /**
     * 새로운 어드민 명령어 메서드: 플레이어의 코인 잔액 설정
     *
     * @param uuid     플레이어 UUID
     * @param coinType 코인 종류 (e.g., "BTC", "ETH")
     * @param amount   설정할 양
     */
    public void setCoinBalance(UUID uuid, String coinType, double amount) {
        if (amount < 0) {
            getLogger().warning("Amount cannot be negative: " + amount);
            return;
        }

        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            try {
                String columnName = getColumnName(coinType);
                if (columnName == null) {
                    getLogger().warning("Unsupported coin type: " + coinType);
                    return;
                }

                String sql = "INSERT INTO players (uuid, " + columnName + ") VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE " + columnName + " = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setDouble(2, amount);
                    ps.setDouble(3, amount);
                    ps.executeUpdate();
                }

                // 거래 내역 기록: SET
                historyDAO.insertTransaction(uuid, getPlayerNickname(uuid), coinType, amount, "SET");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * 새로운 어드민 명령어 메서드: 플레이어의 코인 차감
     *
     * @param uuid     플레이어 UUID
     * @param coinType 코인 종류 (e.g., "BTC", "ETH")
     * @param amount   차감할 양
     */
    public void takeCoin(UUID uuid, String coinType, double amount) {
        if (amount <= 0) {
            getLogger().warning("Invalid amount to take: " + amount);
            return;
        }

        queryQueue.addTask(new QueryTask(databaseManager, (Connection conn) -> {
            try {
                String columnName = getColumnName(coinType);
                if (columnName == null) {
                    getLogger().warning("Unsupported coin type: " + coinType);
                    return;
                }

                // 현재 잔고 확인
                String selectSql = "SELECT " + columnName + " FROM players WHERE uuid = ?";
                double currentBalance = 0.0;
                try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                    psSelect.setString(1, uuid.toString());
                    try (ResultSet rs = psSelect.executeQuery()) {
                        if (rs.next()) {
                            currentBalance = rs.getDouble(columnName);
                        }
                    }
                }

                if (currentBalance < amount) {
                    getLogger().warning("Insufficient " + coinType + " for player " + uuid + ". Needed: " + amount + ", Available: " + currentBalance);
                    return;
                }

                // 코인 차감
                String updateSql = "UPDATE players SET " + columnName + " = " + columnName + " - ? WHERE uuid = ?";
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setDouble(1, amount);
                    psUpdate.setString(2, uuid.toString());
                    psUpdate.executeUpdate();
                }

                // 거래 내역 기록: TAKE
                historyDAO.insertTransaction(uuid, getPlayerNickname(uuid), coinType, -amount, "TAKE");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * 플레이어의 닉네임을 가져오는 헬퍼 메서드
     * 실제 구현에서는 플레이어 객체나 다른 방법으로 닉네임을 가져와야 합니다.
     *
     * @param uuid 플레이어 UUID
     * @return 플레이어 닉네임
     */
    private String getPlayerNickname(UUID uuid) {
        // 예시: 서버에서 플레이어 닉네임을 가져오는 로직
        // 실제 구현에 맞게 수정 필요
        Player player = Bukkit.getPlayer(uuid);
        return (player != null) ? player.getName() : "Unknown";
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

    // 기타 기존 메소드들...
}
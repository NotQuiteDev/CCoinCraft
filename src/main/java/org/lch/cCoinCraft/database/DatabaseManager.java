package org.lch.cCoinCraft.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 매번 Connection 열고 닫는 구조 (B 방식).
 */
public class DatabaseManager {

    private final String dbUrl;

    public DatabaseManager(File dbFile) {
        // DB 파일 절대경로를 기반으로 JDBC URL 구성
        this.dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    /**
     * 테이블 생성 등 초기 작업.
     * 서버 onEnable() 시점에 한 번만 호출.
     */
    public void initDatabase() {
        // 여기서만 임시로 Connection 열어서 테이블 존재 확인/생성
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            String createTableSql =
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "    uuid TEXT PRIMARY KEY," +
                            "    nickname TEXT NOT NULL," +
                            "    btc_balance REAL DEFAULT 0.0," +
                            "    eth_balance REAL DEFAULT 0.0," +
                            "    doge_balance REAL DEFAULT 0.0," +
                            "    usdt_balance REAL DEFAULT 0.0" +
                            ");";

            stmt.executeUpdate(createTableSql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 매번 새로운 Connection을 반환.
     * QueryTask 안에서 try-with-resources로 쓰고 즉시 닫힘.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * (B 방식에서는 굳이 유지할 Connection이 없으므로 특별히 닫을 작업 없음)
     */
    public void closeDatabase() {
        // 필요하다면 log 정도 남길 수 있음
        System.out.println("[DatabaseManager] Nothing to close (B-Mode).");
    }
}
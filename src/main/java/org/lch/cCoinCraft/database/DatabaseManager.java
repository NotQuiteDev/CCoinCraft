package org.lch.cCoinCraft.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private Connection connection;
    private final File dbFile;

    public DatabaseManager(File dbFile) {
        this.dbFile = dbFile;
    }

    public void initDatabase() {
        try {
            // dbFile 경로를 기반으로 JDBC URL 생성
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);

            if (connection == null) {
                throw new SQLException("SQLite 데이터베이스 연결에 실패했습니다.");
            }

            createPlayersTable();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createPlayersTable() {
        String sql =
                "CREATE TABLE IF NOT EXISTS players (" +
                        "    uuid TEXT PRIMARY KEY," +
                        "    nickname TEXT NOT NULL," +
                        "    btc_balance REAL DEFAULT 0.0," +
                        "    eth_balance REAL DEFAULT 0.0," +
                        "    doge_balance REAL DEFAULT 0.0," +
                        "    usdt_balance REAL DEFAULT 0.0" +
                        ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeDatabase() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
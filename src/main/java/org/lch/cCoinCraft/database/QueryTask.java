package org.lch.cCoinCraft.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

import static org.bukkit.Bukkit.getLogger;

/**
 * 하나의 DB 작업(INSERT/UPDATE/SELECT)을 정의하는 클래스.
 * B 방식: "매번 새 Connection" 구조.
 */
public class QueryTask implements Runnable {

    private final DatabaseManager databaseManager;
    private final Consumer<Connection> queryAction;

    public QueryTask(DatabaseManager databaseManager, Consumer<Connection> queryAction) {
        this.databaseManager = databaseManager;
        this.queryAction = queryAction;
    }

    @Override
    public void run() {
        try (Connection conn = databaseManager.getConnection()) {
            // 실제 쿼리 실행 로직
            queryAction.accept(conn);
        } catch (SQLException e) {
            getLogger().warning("DB Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
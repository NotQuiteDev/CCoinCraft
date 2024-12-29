package org.lch.cCoinCraft.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

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
            queryAction.accept(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
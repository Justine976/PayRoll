package database;

import config.DatabaseConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import util.AppConstants;

public final class SQLiteConnectionManager implements ConnectionPool {
    private final Deque<Connection> available = new ArrayDeque<>();
    private final String jdbcUrl;
    private final int maxPoolSize;
    private int totalConnections;

    private SQLiteConnectionManager(String jdbcUrl, int maxPoolSize) {
        this.jdbcUrl = jdbcUrl;
        this.maxPoolSize = maxPoolSize;
    }

    private static class Holder {
        private static final SQLiteConnectionManager INSTANCE =
                new SQLiteConnectionManager(DatabaseConfig.jdbcUrl(), AppConstants.MAX_POOL_SIZE);
    }

    public static SQLiteConnectionManager getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public synchronized Connection borrowConnection() throws SQLException {
        ensureDatabaseFileExists();

        while (!available.isEmpty()) {
            Connection existing = available.pop();
            if (existing != null && !existing.isClosed()) {
                return existing;
            }
            totalConnections = Math.max(0, totalConnections - 1);
        }

        if (totalConnections < maxPoolSize) {
            totalConnections++;
            return DriverManager.getConnection(jdbcUrl);
        }

        return DriverManager.getConnection(jdbcUrl);
    }

    @Override
    public synchronized void returnConnection(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            if (connection.isClosed()) {
                totalConnections = Math.max(0, totalConnections - 1);
                return;
            }
        } catch (SQLException e) {
            closeQuietly(connection);
            totalConnections = Math.max(0, totalConnections - 1);
            return;
        }

        if (available.size() >= maxPoolSize) {
            closeQuietly(connection);
            totalConnections = Math.max(0, totalConnections - 1);
        } else {
            available.push(connection);
        }
    }

    @Override
    public synchronized void close() {
        while (!available.isEmpty()) {
            closeQuietly(available.pop());
        }
        totalConnections = 0;
    }

    private void ensureDatabaseFileExists() throws SQLException {
        try {
            Path path = DatabaseConfig.databasePath();
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }
        } catch (Exception e) {
            throw new SQLException("Unable to create SQLite database file", e);
        }
    }

    private void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}

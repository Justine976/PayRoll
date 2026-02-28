package database;

import config.DatabaseConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.AppConstants;

public final class SQLiteConnectionManager implements ConnectionPool {
    private static final Logger LOGGER = Logger.getLogger(SQLiteConnectionManager.class.getName());
    private static final Object SCHEMA_LOCK = new Object();

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

    public static Path getDbPath() {
        return DatabaseConfig.databasePath();
    }

    public static Object schemaLock() {
        return SCHEMA_LOCK;
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
            return createConnection();
        }

        return createConnection();
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
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
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

    private Connection createConnection() throws SQLException {
        SQLException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Connection connection = DriverManager.getConnection(jdbcUrl);
                initializeConnection(connection);
                LOGGER.fine("SQLite connection opened on attempt " + attempt + " to " + getDbPath());
                return connection;
            } catch (SQLException ex) {
                last = ex;
                LOGGER.log(Level.WARNING, "SQLite connection attempt " + attempt + " failed.", ex);
                if (attempt < 3) {
                    try {
                        Thread.sleep(120L * attempt);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new SQLException("Unable to create SQLite connection after retries.", last);
    }

    private void initializeConnection(Connection connection) throws SQLException {
        applyPragma(connection, "PRAGMA busy_timeout = 5000", false);
        applyPragma(connection, "PRAGMA journal_mode = WAL", true);
        applyPragma(connection, "PRAGMA synchronous = NORMAL", true);
    }

    private void applyPragma(Connection connection, String sql, boolean optional) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException ex) {
            if (!optional) {
                throw ex;
            }
        }
    }
}

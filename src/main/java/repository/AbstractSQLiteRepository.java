package repository;

import database.SQLiteConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import model.BaseEntity;

public abstract class AbstractSQLiteRepository<T extends BaseEntity, ID> implements CrudRepository<T, ID> {
    private final SQLiteConnectionManager connectionManager;

    protected AbstractSQLiteRepository() {
        this.connectionManager = SQLiteConnectionManager.getInstance();
    }

    protected Connection getConnection() throws SQLException {
        return connectionManager.borrowConnection();
    }

    protected PreparedStatement prepare(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    protected void releaseConnection(Connection connection) {
        connectionManager.returnConnection(connection);
    }
}

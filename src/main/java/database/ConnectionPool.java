package database;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool {
    Connection borrowConnection() throws SQLException;

    void returnConnection(Connection connection);

    void close();
}

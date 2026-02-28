package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.User;

public class UserRepository extends AbstractSQLiteRepository<User, Long> {

    public void ensureSchema() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY,
                    full_name TEXT NOT NULL,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    created_at TEXT,
                    updated_at TEXT
                )
                """;

        Connection connection = getConnection();
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.executeUpdate();
        } finally {
            releaseConnection(connection);
        }
    }

    public boolean hasAnyUser() throws SQLException {
        String sql = "SELECT 1 FROM users LIMIT 1";
        Connection connection = getConnection();
        try (PreparedStatement statement = prepare(connection, sql);
                ResultSet rs = statement.executeQuery()) {
            return rs.next();
        } finally {
            releaseConnection(connection);
        }
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        Connection connection = getConnection();
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } finally {
            releaseConnection(connection);
        }
    }

    @Override
    public User save(User entity) throws SQLException {
        String sql = "INSERT INTO users(full_name, username, password_hash, created_at, updated_at) VALUES(?, ?, ?, ?, ?)";
        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, entity.getFullName());
            statement.setString(2, entity.getUsername());
            statement.setString(3, entity.getPasswordHash());
            statement.setString(4, asText(entity.getCreatedAt()));
            statement.setString(5, asText(entity.getUpdatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }
            return entity;
        } finally {
            releaseConnection(connection);
        }
    }

    @Override
    public Optional<User> findById(Long id) throws SQLException {
        String sql = "SELECT id, full_name, username, password_hash, created_at, updated_at FROM users WHERE id = ? LIMIT 1";
        Connection connection = getConnection();
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } finally {
            releaseConnection(connection);
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, full_name, username, password_hash, created_at, updated_at FROM users WHERE username = ? LIMIT 1";
        Connection connection = getConnection();
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } finally {
            releaseConnection(connection);
        }
    }

    @Override
    public List<User> findAll(int limit, int offset) throws SQLException {
        String sql = "SELECT id, full_name, username, password_hash, created_at, updated_at FROM users ORDER BY id LIMIT ? OFFSET ?";
        Connection connection = getConnection();
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setInt(1, limit);
            statement.setInt(2, offset);
            List<User> users = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    users.add(map(rs));
                }
            }
            return users;
        } finally {
            releaseConnection(connection);
        }
    }

    @Override
    public boolean update(User entity) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, username = ?, password_hash = ?, updated_at = ? WHERE id = ?";
        Connection connection = getConnection();
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setString(1, entity.getFullName());
            statement.setString(2, entity.getUsername());
            statement.setString(3, entity.getPasswordHash());
            statement.setString(4, asText(entity.getUpdatedAt()));
            statement.setLong(5, entity.getId());
            return statement.executeUpdate() > 0;
        } finally {
            releaseConnection(connection);
        }
    }

    @Override
    public boolean deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        Connection connection = getConnection();
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } finally {
            releaseConnection(connection);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setFullName(rs.getString("full_name"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setCreatedAt(asDateTime(rs.getString("created_at")));
        user.setUpdatedAt(asDateTime(rs.getString("updated_at")));
        return user;
    }

    private String asText(LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime).toString();
    }

    private LocalDateTime asDateTime(String text) {
        return text == null ? null : Timestamp.valueOf(text).toLocalDateTime();
    }
}

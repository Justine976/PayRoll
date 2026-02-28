package repository;

import database.SQLiteConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Employee;

public class EmployeeRepository {
    private static final String INSERT_SQL = "INSERT INTO employees(full_name, position, monthly_salary, created_at, updated_at) VALUES(?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE employees SET full_name = ?, position = ?, monthly_salary = ?, updated_at = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM employees WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT id, full_name, position, monthly_salary, created_at, updated_at FROM employees ORDER BY id DESC";
    private static final String SEARCH_SQL = "SELECT id, full_name, position, monthly_salary, created_at, updated_at FROM employees WHERE lower(full_name) LIKE ? OR lower(position) LIKE ? ORDER BY id DESC";
    private static final String EXISTS_DUPLICATE_SQL = "SELECT 1 FROM employees WHERE full_name = ? AND position = ? LIMIT 1";
    private static final String EXISTS_DUPLICATE_EXCEPT_SQL = "SELECT 1 FROM employees WHERE full_name = ? AND position = ? AND id <> ? LIMIT 1";
    private static final String EXISTS_ID_SQL = "SELECT 1 FROM employees WHERE id = ? LIMIT 1";

    public void ensureSchema() throws SQLException {
        synchronized (SQLiteConnectionManager.schemaLock()) {
            String tableSql = """
                    CREATE TABLE IF NOT EXISTS employees (
                        id INTEGER PRIMARY KEY,
                        full_name TEXT NOT NULL,
                        position TEXT NOT NULL,
                        monthly_salary REAL NOT NULL,
                        created_at TEXT,
                        updated_at TEXT
                    )
                    """;
            String nameIndex = "CREATE INDEX IF NOT EXISTS idx_employees_full_name ON employees(full_name)";
            String posIndex = "CREATE INDEX IF NOT EXISTS idx_employees_position ON employees(position)";

            Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
            try (PreparedStatement table = connection.prepareStatement(tableSql)) {
                table.executeUpdate();
            }

            try (PreparedStatement idx1 = connection.prepareStatement(nameIndex)) {
                idx1.executeUpdate();
            }

            try (PreparedStatement idx2 = connection.prepareStatement(posIndex)) {
                idx2.executeUpdate();
            } finally {
                SQLiteConnectionManager.getInstance().returnConnection(connection);
            }
        }
    }

    public Employee save(Employee employee) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, employee.getFullName());
            statement.setString(2, employee.getPosition());
            statement.setDouble(3, employee.getMonthlySalary());
            statement.setString(4, asText(employee.getCreatedAt()));
            statement.setString(5, asText(employee.getUpdatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return employee.withId(keys.getLong(1));
                }
            }
            return employee;
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public boolean update(Employee employee) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, employee.getFullName());
            statement.setString(2, employee.getPosition());
            statement.setDouble(3, employee.getMonthlySalary());
            statement.setString(4, asText(employee.getUpdatedAt()));
            statement.setLong(5, employee.getId());
            return statement.executeUpdate() > 0;
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public boolean deleteById(long id) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public int deleteBatch(List<Long> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        boolean autoCommit = connection.getAutoCommit();
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            connection.setAutoCommit(false);
            for (Long id : ids) {
                statement.setLong(1, id);
                statement.addBatch();
            }
            int[] results = statement.executeBatch();
            connection.commit();
            int affected = 0;
            for (int result : results) {
                if (result > 0) {
                    affected++;
                }
            }
            return affected;
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(autoCommit);
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public List<Employee> findAll() throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
                ResultSet rs = statement.executeQuery()) {
            return mapRows(rs);
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public List<Employee> search(String keyword) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(SEARCH_SQL)) {
            String token = "%" + keyword.toLowerCase() + "%";
            statement.setString(1, token);
            statement.setString(2, token);
            try (ResultSet rs = statement.executeQuery()) {
                return mapRows(rs);
            }
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public List<Employee> findFiltered(String keyword, String position, String sortKey) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id, full_name, position, monthly_salary, created_at, updated_at FROM employees WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (lower(full_name) LIKE ? OR lower(position) LIKE ?)");
            String token = "%" + keyword.toLowerCase() + "%";
            params.add(token);
            params.add(token);
        }

        if (position != null && !position.isBlank()) {
            sql.append(" AND position = ?");
            params.add(position);
        }

        sql.append(" ORDER BY ").append(resolveSort(sortKey));

        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                return mapRows(rs);
            }
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public List<String> findDistinctPositions() throws SQLException {
        String sql = "SELECT DISTINCT position FROM employees WHERE position IS NOT NULL AND trim(position) <> '' ORDER BY position";
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            List<String> values = new ArrayList<>();
            while (rs.next()) {
                values.add(rs.getString("position"));
            }
            return values;
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }


    public boolean existsById(long id) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(EXISTS_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public boolean existsByNameAndPosition(String fullName, String position, Long excludeId) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        String sql = excludeId == null ? EXISTS_DUPLICATE_SQL : EXISTS_DUPLICATE_EXCEPT_SQL;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fullName);
            statement.setString(2, position);
            if (excludeId != null) {
                statement.setLong(3, excludeId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    private List<Employee> mapRows(ResultSet rs) throws SQLException {
        List<Employee> data = new ArrayList<>();
        while (rs.next()) {
            data.add(new Employee(
                    rs.getLong("id"),
                    rs.getString("full_name"),
                    rs.getString("position"),
                    rs.getDouble("monthly_salary"),
                    asDateTime(rs.getString("created_at")),
                    asDateTime(rs.getString("updated_at"))
            ));
        }
        return data;
    }

    private String asText(LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime).toString();
    }

    private LocalDateTime asDateTime(String text) {
        return text == null ? null : Timestamp.valueOf(text).toLocalDateTime();
    }

    private String resolveSort(String sortKey) {
        if (sortKey == null) {
            return "id DESC";
        }
        return switch (sortKey) {
            case "NAME_ASC" -> "full_name ASC, id DESC";
            case "NAME_DESC" -> "full_name DESC, id DESC";
            case "SALARY_ASC" -> "monthly_salary ASC, id DESC";
            case "SALARY_DESC" -> "monthly_salary DESC, id DESC";
            default -> "id DESC";
        };
    }
}

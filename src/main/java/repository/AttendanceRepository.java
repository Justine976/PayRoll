package repository;

import database.SQLiteConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import model.AttendanceRecord;

public class AttendanceRepository {
    private static final String INSERT_SQL = "INSERT INTO attendance(employee_id, date, status, created_at, updated_at) VALUES(?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE attendance SET employee_id = ?, date = ?, status = ?, updated_at = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM attendance WHERE id = ?";
    private static final String FIND_BY_EMPLOYEE = "SELECT id, employee_id, date, status, created_at, updated_at FROM attendance WHERE employee_id = ? ORDER BY date DESC";
    private static final String FIND_BY_MONTH = "SELECT id, employee_id, date, status, created_at, updated_at FROM attendance WHERE substr(date, 1, 7) = ? ORDER BY date DESC, employee_id";
    private static final String FIND_BY_EMPLOYEE_MONTH = "SELECT id, employee_id, date, status, created_at, updated_at FROM attendance WHERE employee_id = ? AND substr(date, 1, 7) = ? ORDER BY date DESC";
    private static final String EXISTS_DUPLICATE = "SELECT 1 FROM attendance WHERE employee_id = ? AND date = ? AND id <> ? LIMIT 1";
    private static final String AGG_MONTH_SQL = """
            SELECT
                SUM(CASE WHEN status='PRESENT' THEN 1 ELSE 0 END) AS present_count,
                SUM(CASE WHEN status='ABSENT' THEN 1 ELSE 0 END) AS absent_count,
                SUM(CASE WHEN status='LATE' THEN 1 ELSE 0 END) AS late_count,
                SUM(CASE WHEN status='HALF_DAY' THEN 1 ELSE 0 END) AS half_count
            FROM attendance
            WHERE employee_id = ? AND substr(date, 1, 7) = ?
            """;

    public record MonthlyStatusTotals(int present, int absent, int late, int halfDay) {
    }

    public void ensureSchema() throws SQLException {
        String tableSql = """
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY,
                    employee_id INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT,
                    updated_at TEXT,
                    UNIQUE(employee_id, date)
                )
                """;
        String idxEmployee = "CREATE INDEX IF NOT EXISTS idx_attendance_employee_id ON attendance(employee_id)";

        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement table = connection.prepareStatement(tableSql);
                PreparedStatement idx = connection.prepareStatement(idxEmployee)) {
            table.executeUpdate();
            idx.executeUpdate();
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public AttendanceRecord save(AttendanceRecord record) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindSave(statement, record);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return record.withId(keys.getLong(1));
                }
            }
            return record;
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public boolean update(AttendanceRecord record) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setLong(1, record.getEmployeeId());
            statement.setString(2, record.getDate().toString());
            statement.setString(3, record.getStatus().name());
            statement.setString(4, asText(record.getUpdatedAt()));
            statement.setLong(5, record.getId());
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
        boolean auto = connection.getAutoCommit();
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            connection.setAutoCommit(false);
            for (Long id : ids) {
                statement.setLong(1, id);
                statement.addBatch();
            }
            int[] results = statement.executeBatch();
            connection.commit();
            int count = 0;
            for (int r : results) {
                if (r > 0) {
                    count++;
                }
            }
            return count;
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(auto);
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public List<AttendanceRecord> findByEmployee(long employeeId) throws SQLException {
        return query(FIND_BY_EMPLOYEE, st -> st.setLong(1, employeeId));
    }

    public List<AttendanceRecord> findByMonth(YearMonth month) throws SQLException {
        return query(FIND_BY_MONTH, st -> st.setString(1, month.toString()));
    }

    public List<AttendanceRecord> findByEmployeeAndMonth(long employeeId, YearMonth month) throws SQLException {
        return query(FIND_BY_EMPLOYEE_MONTH, st -> {
            st.setLong(1, employeeId);
            st.setString(2, month.toString());
        });
    }

    public boolean existsDuplicate(long employeeId, LocalDate date, long excludeId) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(EXISTS_DUPLICATE)) {
            statement.setLong(1, employeeId);
            statement.setString(2, date.toString());
            statement.setLong(3, excludeId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    public MonthlyStatusTotals aggregateByEmployeeAndMonth(long employeeId, YearMonth month) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(AGG_MONTH_SQL)) {
            statement.setLong(1, employeeId);
            statement.setString(2, month.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new MonthlyStatusTotals(
                            rs.getInt("present_count"),
                            rs.getInt("absent_count"),
                            rs.getInt("late_count"),
                            rs.getInt("half_count"));
                }
            }
            return new MonthlyStatusTotals(0, 0, 0, 0);
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private List<AttendanceRecord> query(String sql, StatementBinder binder) throws SQLException {
        Connection connection = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rs = statement.executeQuery()) {
                return mapRows(rs);
            }
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(connection);
        }
    }

    private List<AttendanceRecord> mapRows(ResultSet rs) throws SQLException {
        List<AttendanceRecord> records = new ArrayList<>();
        while (rs.next()) {
            records.add(new AttendanceRecord(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    LocalDate.parse(rs.getString("date")),
                    AttendanceRecord.Status.valueOf(rs.getString("status")),
                    asDateTime(rs.getString("created_at")),
                    asDateTime(rs.getString("updated_at"))));
        }
        return records;
    }

    private void bindSave(PreparedStatement statement, AttendanceRecord record) throws SQLException {
        statement.setLong(1, record.getEmployeeId());
        statement.setString(2, record.getDate().toString());
        statement.setString(3, record.getStatus().name());
        statement.setString(4, asText(record.getCreatedAt()));
        statement.setString(5, asText(record.getUpdatedAt()));
    }

    private String asText(LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime).toString();
    }

    private LocalDateTime asDateTime(String text) {
        return text == null ? null : Timestamp.valueOf(text).toLocalDateTime();
    }
}

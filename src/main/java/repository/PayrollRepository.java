package repository;

import database.SQLiteConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import model.PayrollRecord;

public class PayrollRepository {
    private static final String INSERT_SQL = "INSERT INTO payroll(employee_id, month, base_salary, effective_work_days, required_work_days, computed_salary, status, processed_at, created_at, updated_at) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_STATUS_SQL = "UPDATE payroll SET status = ?, processed_at = ?, updated_at = ? WHERE id = ?";
    private static final String FIND_BY_MONTH_SQL = "SELECT * FROM payroll WHERE month = ? ORDER BY employee_id";
    private static final String FIND_BY_EMP_MONTH_SQL = "SELECT * FROM payroll WHERE employee_id = ? AND month = ? LIMIT 1";
    private static final String DELETE_SQL = "DELETE FROM payroll WHERE id = ?";
    private static final String COUNT_MONTH_SQL = "SELECT COUNT(*) AS c FROM payroll WHERE month = ?";
    private static final String RECENT_SQL = "SELECT processed_at FROM payroll WHERE processed_at IS NOT NULL ORDER BY processed_at DESC LIMIT 1";

    public void ensureSchema() throws SQLException {
        synchronized (SQLiteConnectionManager.schemaLock()) {
            String tableSql = """
                    CREATE TABLE IF NOT EXISTS payroll (
                      id INTEGER PRIMARY KEY,
                      employee_id INTEGER NOT NULL,
                      month TEXT NOT NULL,
                      base_salary REAL NOT NULL,
                      effective_work_days REAL NOT NULL,
                      required_work_days REAL NOT NULL,
                      computed_salary REAL NOT NULL,
                      status TEXT NOT NULL,
                      processed_at TEXT,
                      created_at TEXT,
                      updated_at TEXT,
                      UNIQUE(employee_id, month)
                    )
                    """;
            String idx = "CREATE INDEX IF NOT EXISTS idx_payroll_employee_id ON payroll(employee_id)";

            Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
            try (PreparedStatement t = c.prepareStatement(tableSql); PreparedStatement i = c.prepareStatement(idx)) {
                t.executeUpdate();
                i.executeUpdate();
            } finally {
                SQLiteConnectionManager.getInstance().returnConnection(c);
            }
        }
    }

    public PayrollRecord save(PayrollRecord record) throws SQLException {
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(INSERT_SQL, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindSave(s, record);
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (keys.next()) return record.withId(keys.getLong(1));
            }
            return record;
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(c);
        }
    }

    public int saveBatch(List<PayrollRecord> records) throws SQLException {
        if (records == null || records.isEmpty()) return 0;
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        boolean auto = c.getAutoCommit();
        try (PreparedStatement s = c.prepareStatement(INSERT_SQL)) {
            c.setAutoCommit(false);
            for (PayrollRecord r : records) {
                bindSave(s, r);
                s.addBatch();
            }
            int[] rs = s.executeBatch();
            c.commit();
            int count = 0;
            for (int r : rs) if (r > 0) count++;
            return count;
        } catch (SQLException ex) {
            c.rollback();
            throw ex;
        } finally {
            c.setAutoCommit(auto);
            SQLiteConnectionManager.getInstance().returnConnection(c);
        }
    }

    public boolean updateStatus(long id, PayrollRecord.Status status) throws SQLException {
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(UPDATE_STATUS_SQL)) {
            LocalDateTime now = LocalDateTime.now();
            s.setString(1, status.name());
            s.setString(2, asText(now));
            s.setString(3, asText(now));
            s.setLong(4, id);
            return s.executeUpdate() > 0;
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(c);
        }
    }

    public List<PayrollRecord> findByMonth(YearMonth month) throws SQLException {
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(FIND_BY_MONTH_SQL)) {
            s.setString(1, month.toString());
            try (ResultSet rs = s.executeQuery()) { return mapRows(rs); }
        } finally { SQLiteConnectionManager.getInstance().returnConnection(c); }
    }

    public PayrollRecord findByEmployeeAndMonth(long employeeId, YearMonth month) throws SQLException {
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(FIND_BY_EMP_MONTH_SQL)) {
            s.setLong(1, employeeId);
            s.setString(2, month.toString());
            try (ResultSet rs = s.executeQuery()) {
                List<PayrollRecord> list = mapRows(rs);
                return list.isEmpty() ? null : list.get(0);
            }
        } finally { SQLiteConnectionManager.getInstance().returnConnection(c); }
    }

    public boolean deleteById(long id) throws SQLException {
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(DELETE_SQL)) {
            s.setLong(1, id);
            return s.executeUpdate() > 0;
        } finally { SQLiteConnectionManager.getInstance().returnConnection(c); }
    }

    public int deleteBatch(List<Long> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) return 0;
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        boolean auto = c.getAutoCommit();
        try (PreparedStatement s = c.prepareStatement(DELETE_SQL)) {
            c.setAutoCommit(false);
            for (Long id : ids) { s.setLong(1, id); s.addBatch(); }
            int[] rs = s.executeBatch();
            c.commit();
            int count = 0;
            for (int r : rs) if (r > 0) count++;
            return count;
        } catch (SQLException ex) {
            c.rollback();
            throw ex;
        } finally {
            c.setAutoCommit(auto);
            SQLiteConnectionManager.getInstance().returnConnection(c);
        }
    }

    public int countByMonth(YearMonth month) throws SQLException {
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(COUNT_MONTH_SQL)) {
            s.setString(1, month.toString());
            try (ResultSet rs = s.executeQuery()) { return rs.next() ? rs.getInt("c") : 0; }
        } finally { SQLiteConnectionManager.getInstance().returnConnection(c); }
    }

    public String recentProcessedAt() throws SQLException {
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(RECENT_SQL); ResultSet rs = s.executeQuery()) {
            return rs.next() ? rs.getString("processed_at") : "No payroll processed";
        } finally { SQLiteConnectionManager.getInstance().returnConnection(c); }
    }

    private List<PayrollRecord> mapRows(ResultSet rs) throws SQLException {
        List<PayrollRecord> out = new ArrayList<>();
        while (rs.next()) {
            out.add(new PayrollRecord(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    YearMonth.parse(rs.getString("month")),
                    rs.getDouble("base_salary"),
                    rs.getDouble("effective_work_days"),
                    rs.getDouble("required_work_days"),
                    rs.getDouble("computed_salary"),
                    PayrollRecord.Status.valueOf(rs.getString("status")),
                    asDateTime(rs.getString("processed_at")),
                    asDateTime(rs.getString("created_at")),
                    asDateTime(rs.getString("updated_at"))
            ));
        }
        return out;
    }

    private void bindSave(PreparedStatement s, PayrollRecord r) throws SQLException {
        s.setLong(1, r.getEmployeeId());
        s.setString(2, r.getMonth().toString());
        s.setDouble(3, r.getBaseSalary());
        s.setDouble(4, r.getEffectiveWorkDays());
        s.setDouble(5, r.getRequiredWorkDays());
        s.setDouble(6, r.getComputedSalary());
        s.setString(7, r.getStatus().name());
        s.setString(8, asText(r.getProcessedAt()));
        s.setString(9, asText(r.getCreatedAt()));
        s.setString(10, asText(r.getUpdatedAt()));
    }

    private String asText(LocalDateTime dt) { return dt == null ? null : Timestamp.valueOf(dt).toString(); }
    private LocalDateTime asDateTime(String t) { return t == null ? null : Timestamp.valueOf(t).toLocalDateTime(); }
}

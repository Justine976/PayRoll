package repository;

import config.ThemeManager;
import database.SQLiteConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import model.AppSettings;

public class SettingsRepository {
    public void ensureSchema() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS settings (
                  id INTEGER PRIMARY KEY,
                  company_name TEXT,
                  required_work_days REAL,
                  theme TEXT,
                  table_config TEXT,
                  created_at TEXT,
                  updated_at TEXT
                )
                """;
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.executeUpdate();
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(c);
        }
    }

    public boolean exists() throws SQLException {
        String sql = "SELECT 1 FROM settings LIMIT 1";
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(sql); ResultSet rs = s.executeQuery()) {
            return rs.next();
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(c);
        }
    }

    public AppSettings load() throws SQLException {
        String sql = "SELECT id, company_name, required_work_days, theme FROM settings LIMIT 1";
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(sql); ResultSet rs = s.executeQuery()) {
            if (rs.next()) {
                ThemeManager.Theme theme = ThemeManager.Theme.valueOf(rs.getString("theme"));
                return new AppSettings(rs.getLong("id"), rs.getString("company_name"), rs.getDouble("required_work_days"), theme);
            }
            return null;
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(c);
        }
    }

    public AppSettings save(AppSettings settings) throws SQLException {
        if (exists()) {
            String update = "UPDATE settings SET company_name = ?, required_work_days = ?, theme = ?, updated_at = datetime('now') WHERE id = ?";
            Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
            try (PreparedStatement s = c.prepareStatement(update)) {
                s.setString(1, settings.getCompanyName());
                s.setDouble(2, settings.getRequiredWorkDays());
                s.setString(3, settings.getTheme().name());
                s.setLong(4, settings.getId() > 0 ? settings.getId() : 1L);
                s.executeUpdate();
                return new AppSettings(settings.getId() > 0 ? settings.getId() : 1L, settings.getCompanyName(), settings.getRequiredWorkDays(), settings.getTheme());
            } finally {
                SQLiteConnectionManager.getInstance().returnConnection(c);
            }
        }

        String insert = "INSERT INTO settings(company_name, required_work_days, theme, created_at, updated_at) VALUES(?, ?, ?, datetime('now'), datetime('now'))";
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, settings.getCompanyName());
            s.setDouble(2, settings.getRequiredWorkDays());
            s.setString(3, settings.getTheme().name());
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (keys.next()) {
                    return settings.withId(keys.getLong(1));
                }
            }
            return settings;
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(c);
        }
    }

    public String loadTableConfig() throws SQLException {
        String sql = "SELECT table_config FROM settings LIMIT 1";
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(sql); ResultSet rs = s.executeQuery()) {
            return rs.next() ? rs.getString("table_config") : null;
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(c);
        }
    }

    public void saveTableConfig(String config) throws SQLException {
        if (!exists()) {
            save(new AppSettings(0L, "My Company", 22, ThemeManager.Theme.LIGHT));
        }
        String sql = "UPDATE settings SET table_config = ?, updated_at = datetime('now') WHERE id = 1";
        Connection c = SQLiteConnectionManager.getInstance().borrowConnection();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, config);
            s.executeUpdate();
        } finally {
            SQLiteConnectionManager.getInstance().returnConnection(c);
        }
    }
}

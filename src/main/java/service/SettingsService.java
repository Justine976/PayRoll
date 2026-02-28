package service;

import config.ThemeManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import model.AppSettings;
import repository.SettingsRepository;

public class SettingsService {
    private SettingsRepository settingsRepository;
    private AppSettings cache;
    private final List<Consumer<AppSettings>> listeners = new CopyOnWriteArrayList<>();

    private SettingsRepository repository() {
        if (settingsRepository == null) settingsRepository = new SettingsRepository();
        return settingsRepository;
    }

    public synchronized void initialize() {
        try {
            repository().ensureSchema();
            if (!repository().exists()) {
                cache = repository().save(new AppSettings(0L, "My Company", 22, ThemeManager.Theme.LIGHT));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to initialize settings.");
        }
    }

    public synchronized AppSettings getSettings() {
        if (cache != null) return cache;
        initialize();
        try {
            AppSettings loaded = repository().load();
            if (loaded == null) {
                loaded = repository().save(new AppSettings(0L, "My Company", 22, ThemeManager.Theme.LIGHT));
            }
            cache = loaded;
            return cache;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load settings.");
        }
    }

    public synchronized AppSettings save(String companyName, double requiredWorkDays, ThemeManager.Theme theme) {
        AppSettings settings = new AppSettings(getSettings().getId(), companyName == null ? "" : companyName.trim(), requiredWorkDays, theme);
        settings.validate();
        try {
            cache = repository().save(settings);
            listeners.forEach(listener -> listener.accept(cache));
            return cache;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save settings.");
        }
    }

    public double requiredWorkDays() {
        return getSettings().getRequiredWorkDays();
    }

    public void addChangeListener(Consumer<AppSettings> listener) {
        listeners.add(listener);
    }

    public String loadTableConfig() {
        try {
            return repository().loadTableConfig();
        } catch (SQLException ex) {
            return null;
        }
    }

    public void saveTableConfig(String config) {
        try {
            repository().saveTableConfig(config);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to persist table configuration.");
        }
    }
}

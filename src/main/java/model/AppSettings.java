package model;

import config.ThemeManager;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AppSettings {
    private final ReadOnlyLongWrapper id = new ReadOnlyLongWrapper();
    private final StringProperty companyName = new SimpleStringProperty();
    private final DoubleProperty requiredWorkDays = new SimpleDoubleProperty();
    private final ObjectProperty<ThemeManager.Theme> theme = new SimpleObjectProperty<>();

    public AppSettings(long id, String companyName, double requiredWorkDays, ThemeManager.Theme theme) {
        this.id.set(id);
        this.companyName.set(companyName);
        this.requiredWorkDays.set(requiredWorkDays);
        this.theme.set(theme);
    }

    public long getId() { return id.get(); }
    public ReadOnlyLongProperty idProperty() { return id.getReadOnlyProperty(); }
    public String getCompanyName() { return companyName.get(); }
    public StringProperty companyNameProperty() { return companyName; }
    public double getRequiredWorkDays() { return requiredWorkDays.get(); }
    public DoubleProperty requiredWorkDaysProperty() { return requiredWorkDays; }
    public ThemeManager.Theme getTheme() { return theme.get(); }
    public ObjectProperty<ThemeManager.Theme> themeProperty() { return theme; }

    public AppSettings withId(long newId) {
        if (getId() > 0) throw new IllegalStateException("Settings ID is immutable once set.");
        return new AppSettings(newId, getCompanyName(), getRequiredWorkDays(), getTheme());
    }

    public void validate() {
        if (getCompanyName() == null || getCompanyName().isBlank()) {
            throw new IllegalArgumentException("Company name is required.");
        }
        if (getRequiredWorkDays() <= 0) {
            throw new IllegalArgumentException("Required work days must be greater than 0.");
        }
        if (getTheme() == null) {
            throw new IllegalArgumentException("Theme is required.");
        }
    }
}

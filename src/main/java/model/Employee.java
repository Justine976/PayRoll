package model;

import java.time.LocalDateTime;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Employee {
    private final ReadOnlyLongWrapper id = new ReadOnlyLongWrapper();
    private final StringProperty fullName = new SimpleStringProperty();
    private final StringProperty position = new SimpleStringProperty();
    private final DoubleProperty monthlySalary = new SimpleDoubleProperty();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updatedAt = new SimpleObjectProperty<>();

    public Employee(long id, String fullName, String position, double monthlySalary,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id.set(id);
        this.fullName.set(fullName);
        this.position.set(position);
        this.monthlySalary.set(monthlySalary);
        this.createdAt.set(createdAt);
        this.updatedAt.set(updatedAt);
    }

    public Employee(String fullName, String position, double monthlySalary) {
        this(0L, fullName, position, monthlySalary, null, null);
    }

    public long getId() {
        return id.get();
    }

    public ReadOnlyLongProperty idProperty() {
        return id.getReadOnlyProperty();
    }

    public String getFullName() {
        return fullName.get();
    }

    public StringProperty fullNameProperty() {
        return fullName;
    }

    public String getPosition() {
        return position.get();
    }

    public StringProperty positionProperty() {
        return position;
    }

    public double getMonthlySalary() {
        return monthlySalary.get();
    }

    public DoubleProperty monthlySalaryProperty() {
        return monthlySalary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }

    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt.get();
    }

    public ObjectProperty<LocalDateTime> updatedAtProperty() {
        return updatedAt;
    }

    public Employee withId(long newId) {
        if (getId() > 0) {
            throw new IllegalStateException("Employee ID is immutable once set.");
        }
        return new Employee(newId, getFullName(), getPosition(), getMonthlySalary(), getCreatedAt(), getUpdatedAt());
    }

    public Employee withTimestamps(LocalDateTime created, LocalDateTime updated) {
        return new Employee(getId(), getFullName(), getPosition(), getMonthlySalary(), created, updated);
    }
}

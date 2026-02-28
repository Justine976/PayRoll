package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.SimpleObjectProperty;

public class AttendanceRecord {
    public enum Status {
        PRESENT,
        ABSENT,
        LATE,
        HALF_DAY
    }

    private final ReadOnlyLongWrapper id = new ReadOnlyLongWrapper();
    private final ReadOnlyLongWrapper employeeId = new ReadOnlyLongWrapper();
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updatedAt = new SimpleObjectProperty<>();

    public AttendanceRecord(long id, long employeeId, LocalDate date, Status status,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id.set(id);
        this.employeeId.set(employeeId);
        this.date.set(date);
        this.status.set(status);
        this.createdAt.set(createdAt);
        this.updatedAt.set(updatedAt);
    }

    public AttendanceRecord(long employeeId, LocalDate date, Status status) {
        this(0L, employeeId, date, status, null, null);
    }

    public long getId() {
        return id.get();
    }

    public ReadOnlyLongProperty idProperty() {
        return id.getReadOnlyProperty();
    }

    public long getEmployeeId() {
        return employeeId.get();
    }

    public ReadOnlyLongProperty employeeIdProperty() {
        return employeeId.getReadOnlyProperty();
    }

    public LocalDate getDate() {
        return date.get();
    }

    public ObjectProperty<LocalDate> dateProperty() {
        return date;
    }

    public Status getStatus() {
        return status.get();
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
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

    public AttendanceRecord withId(long newId) {
        if (getId() > 0) {
            throw new IllegalStateException("Attendance ID is immutable once set.");
        }
        return new AttendanceRecord(newId, getEmployeeId(), getDate(), getStatus(), getCreatedAt(), getUpdatedAt());
    }

    public AttendanceRecord withTimestamps(LocalDateTime created, LocalDateTime updated) {
        return new AttendanceRecord(getId(), getEmployeeId(), getDate(), getStatus(), created, updated);
    }
}

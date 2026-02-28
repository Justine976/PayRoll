package model;

import java.time.LocalDateTime;
import java.time.YearMonth;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

public class PayrollRecord {
    public enum Status {
        PAID,
        UNPAID
    }

    private final ReadOnlyLongWrapper id = new ReadOnlyLongWrapper();
    private final ReadOnlyLongWrapper employeeId = new ReadOnlyLongWrapper();
    private final ObjectProperty<YearMonth> month = new SimpleObjectProperty<>();
    private final DoubleProperty baseSalary = new SimpleDoubleProperty();
    private final DoubleProperty effectiveWorkDays = new SimpleDoubleProperty();
    private final DoubleProperty requiredWorkDays = new SimpleDoubleProperty();
    private final DoubleProperty computedSalary = new SimpleDoubleProperty();
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> processedAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updatedAt = new SimpleObjectProperty<>();

    public PayrollRecord(long id, long employeeId, YearMonth month, double baseSalary,
            double effectiveWorkDays, double requiredWorkDays, double computedSalary,
            Status status, LocalDateTime processedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id.set(id);
        this.employeeId.set(employeeId);
        this.month.set(month);
        this.baseSalary.set(baseSalary);
        this.effectiveWorkDays.set(effectiveWorkDays);
        this.requiredWorkDays.set(requiredWorkDays);
        this.computedSalary.set(computedSalary);
        this.status.set(status);
        this.processedAt.set(processedAt);
        this.createdAt.set(createdAt);
        this.updatedAt.set(updatedAt);
    }

    public PayrollRecord(long employeeId, YearMonth month, double baseSalary,
            double effectiveWorkDays, double requiredWorkDays, double computedSalary, Status status) {
        this(0L, employeeId, month, baseSalary, effectiveWorkDays, requiredWorkDays, computedSalary, status, null, null, null);
    }

    public long getId() { return id.get(); }
    public ReadOnlyLongProperty idProperty() { return id.getReadOnlyProperty(); }
    public long getEmployeeId() { return employeeId.get(); }
    public ReadOnlyLongProperty employeeIdProperty() { return employeeId.getReadOnlyProperty(); }
    public YearMonth getMonth() { return month.get(); }
    public ObjectProperty<YearMonth> monthProperty() { return month; }
    public double getBaseSalary() { return baseSalary.get(); }
    public DoubleProperty baseSalaryProperty() { return baseSalary; }
    public double getEffectiveWorkDays() { return effectiveWorkDays.get(); }
    public DoubleProperty effectiveWorkDaysProperty() { return effectiveWorkDays; }
    public double getRequiredWorkDays() { return requiredWorkDays.get(); }
    public DoubleProperty requiredWorkDaysProperty() { return requiredWorkDays; }
    public double getComputedSalary() { return computedSalary.get(); }
    public DoubleProperty computedSalaryProperty() { return computedSalary; }
    public Status getStatus() { return status.get(); }
    public ObjectProperty<Status> statusProperty() { return status; }
    public LocalDateTime getProcessedAt() { return processedAt.get(); }
    public ObjectProperty<LocalDateTime> processedAtProperty() { return processedAt; }
    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt.get(); }
    public ObjectProperty<LocalDateTime> updatedAtProperty() { return updatedAt; }

    public PayrollRecord withId(long newId) {
        if (getId() > 0) throw new IllegalStateException("Payroll ID is immutable once set.");
        return new PayrollRecord(newId, getEmployeeId(), getMonth(), getBaseSalary(), getEffectiveWorkDays(),
                getRequiredWorkDays(), getComputedSalary(), getStatus(), getProcessedAt(), getCreatedAt(), getUpdatedAt());
    }

    public PayrollRecord withTimestamps(LocalDateTime processedAtValue, LocalDateTime created, LocalDateTime updated) {
        return new PayrollRecord(getId(), getEmployeeId(), getMonth(), getBaseSalary(), getEffectiveWorkDays(),
                getRequiredWorkDays(), getComputedSalary(), getStatus(), processedAtValue, created, updated);
    }
}

package service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import model.AttendanceRecord;
import repository.AttendanceRepository;
import repository.EmployeeRepository;

public class AttendanceService {
    private AttendanceRepository attendanceRepository;
    private EmployeeRepository employeeRepository;
    private final WorkDayCalculator calculator = new WorkDayCalculator();

    private AttendanceRepository attendanceRepository() {
        if (attendanceRepository == null) {
            attendanceRepository = new AttendanceRepository();
        }
        return attendanceRepository;
    }

    private EmployeeRepository employeeRepository() {
        if (employeeRepository == null) {
            employeeRepository = new EmployeeRepository();
        }
        return employeeRepository;
    }

    public void initialize() {
        try {
            attendanceRepository().ensureSchema();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to initialize attendance storage.");
        }
    }

    public AttendanceRecord create(Long employeeId, LocalDate date, AttendanceRecord.Status status) {
        long normalizedEmployeeId = validEmployee(employeeId);
        LocalDate normalizedDate = requiredDate(date);
        AttendanceRecord.Status normalizedStatus = requiredStatus(status);

        try {
            if (attendanceRepository().existsDuplicate(normalizedEmployeeId, normalizedDate, 0L)) {
                throw new IllegalArgumentException("Attendance already exists for this employee and date.");
            }

            LocalDateTime now = LocalDateTime.now();
            AttendanceRecord record = new AttendanceRecord(normalizedEmployeeId, normalizedDate, normalizedStatus)
                    .withTimestamps(now, now);
            return attendanceRepository().save(record);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save attendance record.");
        }
    }

    public AttendanceRecord update(AttendanceRecord existing, Long employeeId, LocalDate date, AttendanceRecord.Status status) {
        if (existing == null || existing.getId() <= 0) {
            throw new IllegalArgumentException("Select a valid attendance record first.");
        }

        long normalizedEmployeeId = validEmployee(employeeId);
        LocalDate normalizedDate = requiredDate(date);
        AttendanceRecord.Status normalizedStatus = requiredStatus(status);

        try {
            if (attendanceRepository().existsDuplicate(normalizedEmployeeId, normalizedDate, existing.getId())) {
                throw new IllegalArgumentException("Attendance already exists for this employee and date.");
            }

            AttendanceRecord updated = new AttendanceRecord(
                    existing.getId(),
                    normalizedEmployeeId,
                    normalizedDate,
                    normalizedStatus,
                    existing.getCreatedAt(),
                    LocalDateTime.now());
            boolean success = attendanceRepository().update(updated);
            if (!success) {
                throw new IllegalStateException("Attendance record was not updated.");
            }
            return updated;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update attendance record.");
        }
    }

    public void delete(long id) {
        try {
            attendanceRepository().deleteById(id);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete attendance record.");
        }
    }

    public int deleteBatch(List<Long> ids) {
        try {
            return attendanceRepository().deleteBatch(ids);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete selected attendance records.");
        }
    }

    public List<AttendanceRecord> findByMonth(YearMonth month) {
        try {
            return attendanceRepository().findByMonth(month);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load attendance records.");
        }
    }

    public List<AttendanceRecord> findByEmployee(long employeeId) {
        try {
            return attendanceRepository().findByEmployee(employeeId);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load attendance records.");
        }
    }

    public List<AttendanceRecord> findByEmployeeAndMonth(long employeeId, YearMonth month) {
        try {
            return attendanceRepository().findByEmployeeAndMonth(employeeId, month);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load attendance records.");
        }
    }

    public WorkDayCalculator.WorkDaySummary computeMonthlySummary(long employeeId, YearMonth month, int requiredWorkDays) {
        try {
            AttendanceRepository.MonthlyStatusTotals totals = attendanceRepository().aggregateByEmployeeAndMonth(employeeId, month);
            return calculator.calculate(totals, requiredWorkDays);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to compute monthly work-day summary.");
        }
    }

    private long validEmployee(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("Employee is required.");
        }
        try {
            if (!employeeRepository().existsById(employeeId)) {
                throw new IllegalArgumentException("Selected employee does not exist.");
            }
            return employeeId;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to validate employee.");
        }
    }

    private LocalDate requiredDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        return date;
    }

    private AttendanceRecord.Status requiredStatus(AttendanceRecord.Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status is required.");
        }
        return status;
    }
}

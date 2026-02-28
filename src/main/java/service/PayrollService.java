package service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import model.Employee;
import model.PayrollRecord;
import repository.PayrollRepository;

public class PayrollService {
    private PayrollRepository payrollRepository;
    private EmployeeService employeeService;
    private AttendanceService attendanceService;
    private final PayrollCalculator payrollCalculator = new PayrollCalculator();
    private SettingsService settingsService;

    private PayrollRepository payrollRepository() {
        if (payrollRepository == null) payrollRepository = new PayrollRepository();
        return payrollRepository;
    }

    private EmployeeService employeeService() {
        if (employeeService == null) employeeService = new EmployeeService();
        return employeeService;
    }

    private AttendanceService attendanceService() {
        if (attendanceService == null) attendanceService = new AttendanceService();
        return attendanceService;
    }

    private SettingsService settingsService() {
        if (settingsService == null) settingsService = new SettingsService();
        return settingsService;
    }

    public void initialize() {
        try { payrollRepository().ensureSchema(); }
        catch (SQLException ex) { throw new IllegalStateException("Unable to initialize payroll storage."); }
    }

    public List<PayrollRecord> findByMonth(YearMonth month) {
        try { return payrollRepository().findByMonth(month); }
        catch (SQLException ex) { throw new IllegalStateException("Unable to load payroll records."); }
    }

    public PayrollRecord generateForEmployee(long employeeId, YearMonth month) {
        if (month == null) throw new IllegalArgumentException("Month is required.");
        try {
            if (payrollRepository().findByEmployeeAndMonth(employeeId, month) != null) {
                throw new IllegalArgumentException("Payroll already generated for this employee and month.");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to validate payroll duplication.");
        }

        Employee employee = employeeService().findAll().stream().filter(e -> e.getId() == employeeId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));

        WorkDayCalculator.WorkDaySummary summary = attendanceService().computeMonthlySummary(employeeId, month, settingsService().requiredWorkDays() > 0 ? (int)Math.round(settingsService().requiredWorkDays()) : 22);
        double computed = payrollCalculator.compute(employee.getMonthlySalary(), summary.effectiveWorkDays(), summary.requiredWorkDays());

        PayrollRecord record = new PayrollRecord(employeeId, month, employee.getMonthlySalary(),
                summary.effectiveWorkDays(), summary.requiredWorkDays(), computed, PayrollRecord.Status.UNPAID)
                .withTimestamps(null, LocalDateTime.now(), LocalDateTime.now());
        try {
            return payrollRepository().save(record);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to generate payroll.");
        }
    }

    public int processAll(YearMonth month) {
        if (month == null) throw new IllegalArgumentException("Month is required.");
        List<Employee> employees = employeeService().findAll();
        List<PayrollRecord> newRecords = new ArrayList<>();

        for (Employee employee : employees) {
            try {
                if (payrollRepository().findByEmployeeAndMonth(employee.getId(), month) != null) {
                    continue;
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Unable to verify existing payroll records.");
            }

            WorkDayCalculator.WorkDaySummary summary = attendanceService().computeMonthlySummary(employee.getId(), month, settingsService().requiredWorkDays() > 0 ? (int)Math.round(settingsService().requiredWorkDays()) : 22);
            double computed = payrollCalculator.compute(employee.getMonthlySalary(), summary.effectiveWorkDays(), summary.requiredWorkDays());
            PayrollRecord record = new PayrollRecord(employee.getId(), month, employee.getMonthlySalary(),
                    summary.effectiveWorkDays(), summary.requiredWorkDays(), computed, PayrollRecord.Status.UNPAID)
                    .withTimestamps(null, LocalDateTime.now(), LocalDateTime.now());
            newRecords.add(record);
        }

        try {
            return payrollRepository().saveBatch(newRecords);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to batch process payroll.");
        }
    }

    public void markAsPaid(List<Long> payrollIds) {
        if (payrollIds == null || payrollIds.isEmpty()) throw new IllegalArgumentException("Select payroll records first.");
        for (Long id : payrollIds) {
            try {
                payrollRepository().updateStatus(id, PayrollRecord.Status.PAID);
            } catch (SQLException ex) {
                throw new IllegalStateException("Unable to update payroll status.");
            }
        }
    }

    public int deleteBatch(List<Long> payrollIds) {
        try { return payrollRepository().deleteBatch(payrollIds); }
        catch (SQLException ex) { throw new IllegalStateException("Unable to delete payroll records."); }
    }

    public int countByMonth(YearMonth month) {
        try { return payrollRepository().countByMonth(month); }
        catch (SQLException ex) { throw new IllegalStateException("Unable to read payroll metrics."); }
    }

    public String recentActivity() {
        try { return payrollRepository().recentProcessedAt(); }
        catch (SQLException ex) { return "Unavailable"; }
    }
}

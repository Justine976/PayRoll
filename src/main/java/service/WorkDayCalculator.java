package service;

import repository.AttendanceRepository;

public class WorkDayCalculator {
    public record WorkDaySummary(
            int totalPresentDays,
            int totalAbsentDays,
            int totalLateDays,
            int totalHalfDays,
            double effectiveWorkDays,
            int requiredWorkDays) {
    }

    public WorkDaySummary calculate(AttendanceRepository.MonthlyStatusTotals totals, int requiredWorkDays) {
        double effective = totals.present() + totals.late() + (totals.halfDay() * 0.5d);
        return new WorkDaySummary(
                totals.present(),
                totals.absent(),
                totals.late(),
                totals.halfDay(),
                effective,
                requiredWorkDays);
    }
}

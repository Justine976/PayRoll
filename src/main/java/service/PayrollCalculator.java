package service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PayrollCalculator {
    public double compute(double monthlySalary, double effectiveWorkDays, double requiredWorkDays) {
        if (effectiveWorkDays <= 0 || requiredWorkDays <= 0 || monthlySalary <= 0) {
            return 0.0d;
        }
        double dailyRate = monthlySalary / requiredWorkDays;
        double raw = dailyRate * effectiveWorkDays;
        double capped = Math.min(raw, monthlySalary);
        return BigDecimal.valueOf(capped).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

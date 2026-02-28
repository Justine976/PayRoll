package service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import model.Employee;
import repository.EmployeeRepository;
import util.ValidationUtil;

public class EmployeeService {
    private EmployeeRepository employeeRepository;

    private EmployeeRepository repository() {
        if (employeeRepository == null) {
            employeeRepository = new EmployeeRepository();
        }
        return employeeRepository;
    }

    public void initialize() {
        try {
            repository().ensureSchema();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to initialize employee storage.");
        }
    }

    public List<Employee> findAll() {
        try {
            return repository().findAll();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load employees.");
        }
    }

    public List<Employee> search(String keyword) {
        String normalized = normalize(keyword);
        try {
            if (normalized.isBlank()) {
                return repository().findAll();
            }
            return repository().search(normalized);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to search employees.");
        }
    }

    public List<Employee> filterAndSort(String keyword, String position, String sortKey) {
        String normalizedKeyword = normalize(keyword);
        String normalizedPosition = normalize(position);
        try {
            return repository().findFiltered(normalizedKeyword,
                    normalizedPosition.equalsIgnoreCase("ALL") ? "" : normalizedPosition,
                    sortKey);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to filter employees.");
        }
    }

    public List<String> listPositions() {
        try {
            return repository().findDistinctPositions();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load position filters.");
        }
    }

    public Employee create(String fullName, String position, String salaryText) {
        String name = required(fullName, "Name is required.");
        String role = required(position, "Position is required.");
        double salary = parseSalary(salaryText);

        try {
            if (repository().existsByNameAndPosition(name, role, null)) {
                throw new IllegalArgumentException("Duplicate employee (same name and position) is not allowed.");
            }

            LocalDateTime now = LocalDateTime.now();
            Employee employee = new Employee(name, role, salary).withTimestamps(now, now);
            return repository().save(employee);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save employee.");
        }
    }

    public Employee update(Employee existing, String fullName, String position, String salaryText) {
        if (existing == null || existing.getId() <= 0) {
            throw new IllegalArgumentException("Select a valid employee first.");
        }

        String name = required(fullName, "Name is required.");
        String role = required(position, "Position is required.");
        double salary = parseSalary(salaryText);

        try {
            if (repository().existsByNameAndPosition(name, role, existing.getId())) {
                throw new IllegalArgumentException("Duplicate employee (same name and position) is not allowed.");
            }

            Employee updated = new Employee(existing.getId(), name, role, salary, existing.getCreatedAt(), LocalDateTime.now());
            boolean success = repository().update(updated);
            if (!success) {
                throw new IllegalStateException("Employee record was not updated.");
            }
            return updated;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update employee.");
        }
    }

    public void delete(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid employee id.");
        }
        try {
            repository().deleteById(id);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete employee.");
        }
    }

    public int deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        try {
            return repository().deleteBatch(ids);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete selected employees.");
        }
    }

    private String required(String value, String message) {
        String normalized = normalize(value);
        if (ValidationUtil.isBlank(normalized)) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private double parseSalary(String salaryText) {
        String normalized = normalize(salaryText);
        try {
            double salary = Double.parseDouble(normalized);
            if (salary <= 0) {
                throw new IllegalArgumentException("Salary must be greater than 0.");
            }
            return salary;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Monthly salary must be a valid number.");
        }
    }
}

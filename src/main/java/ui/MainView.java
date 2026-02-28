package ui;

import config.ThemeManager;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import model.AppSettings;
import model.AttendanceRecord;
import model.Employee;
import model.PayrollRecord;
import service.AttendanceService;
import service.AuthService;
import service.DatabaseBackupService;
import service.EmployeeService;
import service.PayrollService;
import service.SettingsService;
import ui.auth.LoginView;
import ui.auth.RegisterView;
import ui.layout.HeaderBar;
import ui.layout.MainWorkspace;
import ui.layout.SideControlPanel;
import util.DialogUtil;

public class MainView {
    private final StackPane root;
    private final AuthService authService;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;
    private final SettingsService settingsService;
    private final DatabaseBackupService backupService;

    private MainWorkspace workspace;
    private SideControlPanel sidePanel;
    private HeaderBar headerBar;
    private Map<Long, String> employeeNameMap = Map.of();
    private final AtomicBoolean operationInProgress = new AtomicBoolean(false);

    public MainView() {
        this.root = new StackPane();
        this.authService = new AuthService();
        this.employeeService = new EmployeeService();
        this.attendanceService = new AttendanceService();
        this.payrollService = new PayrollService();
        this.settingsService = new SettingsService();
        this.backupService = new DatabaseBackupService();
    }

    public Parent createRoot() {
        root.getChildren().setAll(new Label("Loading..."));
        initializeAsync();
        return root;
    }

    private void initializeAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                authService.initialize();
                settingsService.initialize();
                boolean registrationRequired = authService.isRegistrationRequired();
                Platform.runLater(() -> {
                    if (registrationRequired) showRegister(); else showLogin();
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    DialogUtil.showError(window(), "Database Error", "Failed to initialize application database.");
                    showFallback();
                });
            }
        });
    }

    private void showRegister() {
        RegisterView registerView = new RegisterView();
        root.getChildren().setAll(registerView.create(request -> {
            try {
                authService.registerAdmin(request.fullName(), request.username(), request.password(), request.confirmPassword());
                DialogUtil.showSuccess(window(), "Registration Successful", "Admin account created. Please sign in.");
                showLogin();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                DialogUtil.showWarning(window(), "Registration", ex.getMessage());
            } catch (SQLException ex) {
                DialogUtil.showError(window(), "Registration Error", "Unable to store account information.");
            }
        }));
    }

    private void showLogin() {
        LoginView loginView = new LoginView();
        root.getChildren().setAll(loginView.create((username, password) -> {
            try {
                if (authService.login(username, password)) {
                    DialogUtil.showSuccess(window(), "Login Successful", "Welcome to PayrollSystemFX.");
                    showApplicationLayout();
                } else {
                    DialogUtil.showError(window(), "Login Failed", "Invalid username or password.");
                }
            } catch (SQLException ex) {
                DialogUtil.showError(window(), "Login Error", "Unable to validate credentials.");
            }
        }));
    }

    private void showApplicationLayout() {
        headerBar = new HeaderBar(() -> {
            authService.logout();
            DialogUtil.showSuccess(window(), "Logged Out", "Session closed successfully.");
            showLogin();
        });

        sidePanel = new SideControlPanel();
        workspace = new MainWorkspace();
        workspace.setActiveTabListener(this::onTabChanged);

        wireEmployeeModule();
        wireAttendanceModule();
        wirePayrollModule();
        wireSettingsModule();
        wireActionHandlers();

        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("dashboard-container");
        layout.setTop(headerBar);
        layout.setLeft(sidePanel);
        layout.setCenter(workspace);
        root.getChildren().setAll(layout);

        AppSettings settings = settingsService.getSettings();
        headerBar.setCompanyName(settings.getCompanyName());
        ThemeManager.applyTheme(root.getScene(), settings.getTheme());
        refreshDashboardAsync(workspace.getPayrollPanel().selectedMonth());
    }

    private void wireEmployeeModule() {
        CompletableFuture.runAsync(() -> {
            try {
                employeeService.initialize();
                List<Employee> data = employeeService.findAll();
                Map<Long, String> map = toNameMap(data);
                Platform.runLater(() -> {
                    workspace.getEmployeePanel().setData(data);
                    employeeNameMap = map;
                    sidePanel.setAttendanceEmployeeOptions(employeeNameMap);
                    workspace.getAttendancePanel().setEmployeeOptions(employeeNameMap);
                });
            } catch (IllegalStateException ex) {
                Platform.runLater(() -> DialogUtil.showError(window(), "Employee Module", ex.getMessage()));
            }
        });

        workspace.getEmployeePanel().onSelectionChanged(selected -> {
            if (sidePanel.getMode() == SideControlPanel.Mode.EMPLOYEE) {
                sidePanel.setSelectionAvailable(selected != null);
                if (selected != null) sidePanel.fillFrom(selected);
            }
        });

        workspace.setSearchHandler(keyword -> {
            if (sidePanel.getMode() != SideControlPanel.Mode.EMPLOYEE) return;
            CompletableFuture.supplyAsync(() -> employeeService.search(keyword))
                    .thenAccept(results -> Platform.runLater(() -> workspace.getEmployeePanel().setData(results)))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> DialogUtil.showError(window(), "Search", "Unable to perform employee search."));
                        return null;
                    });
        });
    }

    private void wireAttendanceModule() {
        CompletableFuture.runAsync(() -> {
            try {
                attendanceService.initialize();
                List<AttendanceRecord> monthData = attendanceService.findByMonth(YearMonth.now());
                Platform.runLater(() -> workspace.getAttendancePanel().setRows(monthData, employeeNameMap));
            } catch (IllegalStateException ex) {
                Platform.runLater(() -> DialogUtil.showError(window(), "Attendance Module", ex.getMessage()));
            }
        });

        workspace.getAttendancePanel().onSelectionChanged(selected -> {
            if (sidePanel.getMode() == SideControlPanel.Mode.ATTENDANCE) {
                sidePanel.setSelectionAvailable(selected != null);
                if (selected != null) sidePanel.fillAttendance(selected.getEmployeeId(), selected.getDate(), selected.getStatus());
            }
        });

        workspace.getAttendancePanel().onFilterChanged(filter -> refreshAttendance(filter.employeeId(), filter.month()));
    }

    private void wirePayrollModule() {
        CompletableFuture.runAsync(() -> {
            try {
                payrollService.initialize();
                List<PayrollRecord> rows = payrollService.findByMonth(workspace.getPayrollPanel().selectedMonth());
                Platform.runLater(() -> workspace.getPayrollPanel().setRows(rows, employeeNameMap));
            } catch (IllegalStateException ex) {
                Platform.runLater(() -> DialogUtil.showError(window(), "Payroll Module", ex.getMessage()));
            }
        });

        workspace.getPayrollPanel().onFilterChanged(req -> {
            refreshPayroll(req.month());
            refreshDashboardAsync(req.month());
        });

        workspace.getPayrollPanel().onSelectionChanged(count -> {
            if (sidePanel.getMode() == SideControlPanel.Mode.PAYROLL) sidePanel.setSelectionAvailable(count > 0);
        });
    }

    private void wireSettingsModule() {
        var settingsPanel = workspace.getSettingsPanel();
        AppSettings settings = settingsService.getSettings();
        settingsPanel.setValues(settings);
        settingsService.addChangeListener(updated -> Platform.runLater(() -> {
            headerBar.setCompanyName(updated.getCompanyName());
            if (root.getScene() != null) ThemeManager.applyTheme(root.getScene(), updated.getTheme());
        }));

        Parent settingsContent = settingsPanel.createView(request -> {
            try {
                double workDays = Double.parseDouble(request.requiredWorkDays());
                AppSettings updated = settingsService.save(request.companyName(), workDays, request.theme());
                ThemeManager.applyTheme(root.getScene(), updated.getTheme());
                headerBar.setCompanyName(updated.getCompanyName());
                DialogUtil.showSuccess(window(), "Settings", "Settings saved successfully.");
            } catch (NumberFormatException ex) {
                DialogUtil.showWarning(window(), "Settings", "Required work days must be a valid number.");
            } catch (RuntimeException ex) {
                DialogUtil.showError(window(), "Settings", ex.getMessage());
            }
        }, this::runBackup, this::runRestore);
        workspace.setSettingsContent(settingsContent);
    }

    private void runBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Backup Database");
        chooser.setInitialFileName("payroll-backup.db");
        var file = chooser.showSaveDialog(window());
        if (file == null) return;
        CompletableFuture.runAsync(() -> backupService.backupTo(file.toPath()))
                .thenRun(() -> Platform.runLater(() -> DialogUtil.showSuccess(window(), "Backup", "Database backup completed.")))
                .exceptionally(ex -> {
                    Platform.runLater(() -> DialogUtil.showError(window(), "Backup", rootCauseMessage(ex)));
                    return null;
                });
    }

    private void runRestore() {
        boolean confirmed = DialogUtil.showConfirmation(window(), "Restore Database", "Restore will replace current data. Continue?");
        if (!confirmed) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Restore Database");
        var file = chooser.showOpenDialog(window());
        if (file == null) return;

        CompletableFuture.runAsync(() -> backupService.restoreFrom(Path.of(file.toURI())))
                .thenRun(() -> Platform.runLater(() -> {
                    DialogUtil.showSuccess(window(), "Restore", "Database restored. Reinitializing views.");
                    showApplicationLayout();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> DialogUtil.showError(window(), "Restore", rootCauseMessage(ex)));
                    return null;
                });
    }

    private void wireActionHandlers() {
        sidePanel.setActionListener(new SideControlPanel.ActionListener() {
            @Override public void onAddOrSave() {
                if (sidePanel.getMode() == SideControlPanel.Mode.EMPLOYEE) addEmployee();
                else if (sidePanel.getMode() == SideControlPanel.Mode.ATTENDANCE) addAttendance();
                else if (sidePanel.getMode() == SideControlPanel.Mode.PAYROLL) generatePayroll();
            }

            @Override public void onEditOrUpdate(boolean editMode) {
                if (sidePanel.getMode() == SideControlPanel.Mode.EMPLOYEE) editEmployee(editMode);
                else if (sidePanel.getMode() == SideControlPanel.Mode.ATTENDANCE) editAttendance(editMode);
                else if (sidePanel.getMode() == SideControlPanel.Mode.PAYROLL) markPayrollPaid();
            }

            @Override public void onDelete() {
                if (sidePanel.getMode() == SideControlPanel.Mode.EMPLOYEE) deleteEmployees();
                else if (sidePanel.getMode() == SideControlPanel.Mode.ATTENDANCE) deleteAttendance();
                else if (sidePanel.getMode() == SideControlPanel.Mode.PAYROLL) deletePayroll();
            }

            @Override public void onImport() { DialogUtil.showWarning(window(), "Import", "Import feature will be implemented in a later phase."); }
            @Override public void onExport() { DialogUtil.showWarning(window(), "Export", "Export feature will be implemented in a later phase."); }
        });
    }

    private void onTabChanged(String tabKey) {
        if (MainWorkspace.EMPLOYEE.equals(tabKey)) {
            sidePanel.switchMode(SideControlPanel.Mode.EMPLOYEE);
            sidePanel.setSelectionAvailable(workspace.getEmployeePanel().getSelectedEmployee() != null);
        } else if (MainWorkspace.ATTENDANCE.equals(tabKey)) {
            sidePanel.switchMode(SideControlPanel.Mode.ATTENDANCE);
            sidePanel.setSelectionAvailable(workspace.getAttendancePanel().getSelectedRecord() != null);
            refreshAttendance(workspace.getAttendancePanel().selectedFilterEmployeeId(), workspace.getAttendancePanel().selectedMonth());
        } else if (MainWorkspace.PAYROLL.equals(tabKey)) {
            sidePanel.switchMode(SideControlPanel.Mode.PAYROLL);
            sidePanel.setSelectionAvailable(workspace.getPayrollPanel().selectedCount() > 0);
            refreshPayroll(workspace.getPayrollPanel().selectedMonth());
            refreshDashboardAsync(workspace.getPayrollPanel().selectedMonth());
        } else {
            sidePanel.switchMode(SideControlPanel.Mode.SETTINGS);
            sidePanel.setSelectionAvailable(false);
        }
    }

    private void addEmployee() {
        if (!beginUiOperation()) return;
        CompletableFuture.supplyAsync(() -> employeeService.create(sidePanel.getFullNameInput(), sidePanel.getPositionInput(), sidePanel.getMonthlySalaryInput()))
                .thenAccept(saved -> Platform.runLater(() -> {
                    workspace.getEmployeePanel().addEmployee(saved);
                    employeeNameMap = new LinkedHashMap<>(employeeNameMap);
                    employeeNameMap.put(saved.getId(), saved.getFullName());
                    sidePanel.setAttendanceEmployeeOptions(employeeNameMap);
                    workspace.getAttendancePanel().setEmployeeOptions(employeeNameMap);
                    sidePanel.clearForm();
                    refreshDashboardAsync(workspace.getPayrollPanel().selectedMonth());
                    DialogUtil.showSuccess(window(), "Employee", "Employee added successfully.");
                }))
                .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showWarning(window(), "Employee", rootCauseMessage(ex))); return null; })
                .whenComplete((ignore, ex) -> endUiOperation());
    }

    private void editEmployee(boolean editMode) {
        if (!editMode) {
            Employee selected = workspace.getEmployeePanel().getSelectedEmployee();
            CompletableFuture.supplyAsync(() -> employeeService.update(selected, sidePanel.getFullNameInput(), sidePanel.getPositionInput(), sidePanel.getMonthlySalaryInput()))
                    .thenAccept(updated -> Platform.runLater(() -> {
                        workspace.getEmployeePanel().updateEmployee(updated);
                        employeeNameMap = new LinkedHashMap<>(employeeNameMap);
                        employeeNameMap.put(updated.getId(), updated.getFullName());
                        sidePanel.setAttendanceEmployeeOptions(employeeNameMap);
                        workspace.getAttendancePanel().setEmployeeOptions(employeeNameMap);
                        sidePanel.clearForm();
                        DialogUtil.showSuccess(window(), "Employee", "Employee updated successfully.");
                    }))
                    .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showWarning(window(), "Employee", rootCauseMessage(ex))); return null; });
        } else {
            Employee selected = workspace.getEmployeePanel().getSelectedEmployee();
            if (selected == null) {
                DialogUtil.showWarning(window(), "Employee", "Select an employee before editing.");
                return;
            }
            sidePanel.fillFrom(selected);
        }
    }

    private void deleteEmployees() {
        List<Long> ids = workspace.getEmployeePanel().getSelectedEmployees().stream().map(Employee::getId).toList();
        if (ids.isEmpty()) { DialogUtil.showWarning(window(), "Employee", "Select at least one employee to delete."); return; }

        if (!beginUiOperation()) return;
        CompletableFuture.supplyAsync(() -> employeeService.deleteBatch(ids))
                .thenAccept(affected -> Platform.runLater(() -> {
                    workspace.getEmployeePanel().removeEmployees(ids);
                    employeeNameMap = new LinkedHashMap<>(employeeNameMap);
                    ids.forEach(employeeNameMap::remove);
                    sidePanel.setAttendanceEmployeeOptions(employeeNameMap);
                    workspace.getAttendancePanel().setEmployeeOptions(employeeNameMap);
                    sidePanel.clearForm();
                    refreshDashboardAsync(workspace.getPayrollPanel().selectedMonth());
                    DialogUtil.showSuccess(window(), "Employee", "Deleted " + affected + " employee(s).");
                }))
                .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showError(window(), "Employee", rootCauseMessage(ex))); return null; })
                .whenComplete((ignore, ex) -> endUiOperation());
    }

    private void addAttendance() {
        if (!beginUiOperation()) return;
        CompletableFuture.supplyAsync(() -> attendanceService.create(sidePanel.getAttendanceEmployeeIdInput(), sidePanel.getAttendanceDateInput(), sidePanel.getAttendanceStatusInput()))
                .thenAccept(saved -> Platform.runLater(() -> {
                    workspace.getAttendancePanel().addRow(saved, employeeNameMap.getOrDefault(saved.getEmployeeId(), "Unknown"));
                    sidePanel.clearForm();
                    DialogUtil.showSuccess(window(), "Attendance", "Attendance record added.");
                }))
                .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showWarning(window(), "Attendance", rootCauseMessage(ex))); return null; })
                .whenComplete((ignore, ex) -> endUiOperation());
    }

    private void editAttendance(boolean editMode) {
        if (!editMode) {
            AttendanceRecord selected = workspace.getAttendancePanel().getSelectedRecord();
            CompletableFuture.supplyAsync(() -> attendanceService.update(selected, sidePanel.getAttendanceEmployeeIdInput(), sidePanel.getAttendanceDateInput(), sidePanel.getAttendanceStatusInput()))
                    .thenAccept(updated -> Platform.runLater(() -> {
                        workspace.getAttendancePanel().updateRow(updated, employeeNameMap.getOrDefault(updated.getEmployeeId(), "Unknown"));
                        sidePanel.clearForm();
                        DialogUtil.showSuccess(window(), "Attendance", "Attendance record updated.");
                    }))
                    .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showWarning(window(), "Attendance", rootCauseMessage(ex))); return null; });
        } else {
            AttendanceRecord selected = workspace.getAttendancePanel().getSelectedRecord();
            if (selected == null) { DialogUtil.showWarning(window(), "Attendance", "Select a record before editing."); return; }
            sidePanel.fillAttendance(selected.getEmployeeId(), selected.getDate(), selected.getStatus());
        }
    }

    private void deleteAttendance() {
        List<Long> ids = workspace.getAttendancePanel().getSelectedRecords().stream().map(AttendanceRecord::getId).toList();
        if (ids.isEmpty()) { DialogUtil.showWarning(window(), "Attendance", "Select at least one record to delete."); return; }

        if (!beginUiOperation()) return;
        CompletableFuture.supplyAsync(() -> attendanceService.deleteBatch(ids))
                .thenAccept(affected -> Platform.runLater(() -> {
                    workspace.getAttendancePanel().removeRows(ids);
                    sidePanel.clearForm();
                    DialogUtil.showSuccess(window(), "Attendance", "Deleted " + affected + " attendance record(s).");
                }))
                .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showError(window(), "Attendance", rootCauseMessage(ex))); return null; })
                .whenComplete((ignore, ex) -> endUiOperation());
    }

    private void generatePayroll() {
        YearMonth month = sidePanel.getPayrollMonthInput();
        if (month == null) { DialogUtil.showWarning(window(), "Payroll", "Month is required."); return; }

        if (!beginUiOperation()) return;
        CompletableFuture.supplyAsync(() -> payrollService.processAll(month))
                .thenAccept(count -> Platform.runLater(() -> {
                    refreshPayroll(month);
                    refreshDashboardAsync(month);
                    DialogUtil.showSuccess(window(), "Payroll", "Processed payroll for " + count + " employee(s).");
                }))
                .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showWarning(window(), "Payroll", rootCauseMessage(ex))); return null; })
                .whenComplete((ignore, ex) -> endUiOperation());
    }

    private void markPayrollPaid() {
        List<Long> ids = workspace.getPayrollPanel().getSelectedRecords().stream().map(PayrollRecord::getId).toList();
        if (ids.isEmpty()) { DialogUtil.showWarning(window(), "Payroll", "Select payroll records first."); return; }
        if (!beginUiOperation()) return;
        CompletableFuture.runAsync(() -> payrollService.markAsPaid(ids))
                .thenRun(() -> Platform.runLater(() -> {
                    refreshPayroll(workspace.getPayrollPanel().selectedMonth());
                    refreshDashboardAsync(workspace.getPayrollPanel().selectedMonth());
                    DialogUtil.showSuccess(window(), "Payroll", "Selected records marked as PAID.");
                }))
                .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showError(window(), "Payroll", rootCauseMessage(ex))); return null; })
                .whenComplete((ignore, ex) -> endUiOperation());
    }

    private void deletePayroll() {
        List<Long> ids = workspace.getPayrollPanel().getSelectedRecords().stream().map(PayrollRecord::getId).toList();
        if (ids.isEmpty()) { DialogUtil.showWarning(window(), "Payroll", "Select payroll records to delete."); return; }

        if (!beginUiOperation()) return;
        CompletableFuture.supplyAsync(() -> payrollService.deleteBatch(ids))
                .thenAccept(affected -> Platform.runLater(() -> {
                    workspace.getPayrollPanel().removeRows(ids);
                    refreshDashboardAsync(workspace.getPayrollPanel().selectedMonth());
                    DialogUtil.showSuccess(window(), "Payroll", "Deleted " + affected + " payroll record(s).");
                }))
                .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showError(window(), "Payroll", rootCauseMessage(ex))); return null; })
                .whenComplete((ignore, ex) -> endUiOperation());
    }

    private void refreshAttendance(Long employeeId, YearMonth month) {
        CompletableFuture.supplyAsync(() -> employeeId == null
                        ? attendanceService.findByMonth(month)
                        : attendanceService.findByEmployeeAndMonth(employeeId, month))
                .thenAccept(records -> Platform.runLater(() -> workspace.getAttendancePanel().setRows(records, employeeNameMap)))
                .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showError(window(), "Attendance", "Unable to refresh attendance records.")); return null; });
    }

    private void refreshPayroll(YearMonth month) {
        CompletableFuture.supplyAsync(() -> payrollService.findByMonth(month))
                .thenAccept(records -> Platform.runLater(() -> workspace.getPayrollPanel().setRows(records, employeeNameMap)))
                .exceptionally(ex -> { Platform.runLater(() -> DialogUtil.showError(window(), "Payroll", "Unable to refresh payroll records.")); return null; });
    }

    private void refreshDashboardAsync(YearMonth month) {
        CompletableFuture.runAsync(() -> {
            try {
                int employees = employeeService.findAll().size();
                int processed = payrollService.countByMonth(month);
                String recent = payrollService.recentActivity();
                String company = settingsService.getSettings().getCompanyName();
                Platform.runLater(() -> {
                    headerBar.setCompanyName(company);
                    headerBar.setTotalEmployees(employees);
                    headerBar.setProcessedPayrollCount(processed);
                    headerBar.setRecentActivity(recent);
                });
            } catch (Exception ignored) {
            }
        });
    }

    private Map<Long, String> toNameMap(List<Employee> employees) {
        return employees.stream().collect(Collectors.toMap(Employee::getId, Employee::getFullName, (a, b) -> a, LinkedHashMap::new));
    }

    private String rootCauseMessage(Throwable ex) {
        Throwable c = ex;
        while (c.getCause() != null) c = c.getCause();
        return c.getMessage() == null ? "Unexpected error" : c.getMessage();
    }

    private javafx.stage.Window window() { return root.getScene() == null ? null : root.getScene().getWindow(); }
    private void showFallback() { root.getChildren().setAll(new Label("Initialization failed.")); }

    private boolean beginUiOperation() {
        if (!operationInProgress.compareAndSet(false, true)) {
            DialogUtil.showWarning(window(), "Please wait", "Another operation is already in progress.");
            return false;
        }
        if (sidePanel != null) sidePanel.setActionsDisabled(true);
        return true;
    }

    private void endUiOperation() {
        operationInProgress.set(false);
        Platform.runLater(() -> {
            if (sidePanel != null) sidePanel.setActionsDisabled(false);
        });
    }
}

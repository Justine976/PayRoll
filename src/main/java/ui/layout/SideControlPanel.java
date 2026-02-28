package ui.layout;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.AttendanceRecord;
import model.Employee;
import util.DialogUtil;

public class SideControlPanel extends VBox {
    public enum Mode { EMPLOYEE, ATTENDANCE, PAYROLL, SETTINGS }

    public interface ActionListener {
        void onAddOrSave();
        void onEditOrUpdate(boolean editMode);
        void onDelete();
        void onImport();
        void onExport();
    }

    private final BooleanProperty selectionAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty actionsDisabled = new SimpleBooleanProperty(false);

    private final Label title = new Label("Employee Controls");
    private final VBox dynamicFields = new VBox(8);

    private final TextField fullNameField = new TextField();
    private final TextField positionField = new TextField();
    private final TextField monthlySalaryField = new TextField();

    private final ComboBox<EmployeeOption> attendanceEmployee = new ComboBox<>();
    private final DatePicker attendanceDate = new DatePicker();
    private final ComboBox<AttendanceRecord.Status> attendanceStatus = new ComboBox<>();

    private final ComboBox<YearMonth> payrollMonth = new ComboBox<>();

    private final Button addSaveButton = new Button("Add / Save");
    private final Button editUpdateButton = new Button("Edit / Update");
    private final Button deleteButton = new Button("Delete");
    private final Button importButton = new Button("Import");
    private final Button exportButton = new Button("Export");

    private Mode mode = Mode.EMPLOYEE;
    private boolean editMode;
    private Predicate<String> validationHook = value -> true;
    private ActionListener actionListener;

    public SideControlPanel() {
        getStyleClass().add("side-panel");
        setSpacing(10);
        setPadding(new Insets(12));
        setAlignment(Pos.TOP_CENTER);

        title.getStyleClass().add("panel-title");

        fullNameField.setPromptText("Full Name");
        positionField.setPromptText("Position");
        monthlySalaryField.setPromptText("Monthly Salary");

        attendanceEmployee.setPromptText("Select Employee");
        attendanceDate.setPromptText("Date");
        attendanceStatus.setPromptText("Status");
        attendanceStatus.setItems(FXCollections.observableArrayList(AttendanceRecord.Status.values()));

        payrollMonth.setPromptText("Payroll Month");
        payrollMonth.getItems().setAll(YearMonth.now(), YearMonth.now().minusMonths(1), YearMonth.now().minusMonths(2));
        payrollMonth.getSelectionModel().selectFirst();

        addSaveButton.getStyleClass().add("primary-button");
        addSaveButton.setMaxWidth(Double.MAX_VALUE);
        addSaveButton.disableProperty().bind(actionsDisabled);
        addSaveButton.setOnAction(event -> {
            if (!validationHook.test(mode.name())) {
                DialogUtil.showWarning(getWindow(), "Validation", "Input did not pass validation hook.");
                return;
            }
            if (actionListener != null) actionListener.onAddOrSave();
        });

        editUpdateButton.getStyleClass().add("secondary-button");
        editUpdateButton.setMaxWidth(Double.MAX_VALUE);
        editUpdateButton.disableProperty().bind(Bindings.or(actionsDisabled, selectionAvailable.not()));
        editUpdateButton.setOnAction(event -> {
            editMode = !editMode;
            if (actionListener != null) actionListener.onEditOrUpdate(editMode);
        });

        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.disableProperty().bind(Bindings.or(actionsDisabled, selectionAvailable.not()));
        deleteButton.setOnAction(event -> {
            boolean confirmed = DialogUtil.showConfirmation(getWindow(), "Confirm Delete", "Delete selected item(s)?");
            if (confirmed && actionListener != null) actionListener.onDelete();
        });

        importButton.getStyleClass().add("secondary-button");
        importButton.setMaxWidth(Double.MAX_VALUE);
        importButton.disableProperty().bind(actionsDisabled);
        importButton.setOnAction(event -> { if (actionListener != null) actionListener.onImport(); });

        exportButton.getStyleClass().add("secondary-button");
        exportButton.setMaxWidth(Double.MAX_VALUE);
        exportButton.disableProperty().bind(actionsDisabled);
        exportButton.setOnAction(event -> { if (actionListener != null) actionListener.onExport(); });

        VBox.setVgrow(dynamicFields, Priority.NEVER);
        getChildren().addAll(title, dynamicFields, addSaveButton, editUpdateButton, deleteButton, importButton, exportButton);
        switchMode(Mode.EMPLOYEE);
    }

    public void switchMode(Mode mode) {
        this.mode = mode;
        dynamicFields.getChildren().clear();
        clearForm();
        editMode = false;
        if (mode == Mode.EMPLOYEE) {
            title.setText("Employee Controls");
            dynamicFields.getChildren().addAll(fullNameField, positionField, monthlySalaryField);
        } else if (mode == Mode.ATTENDANCE) {
            title.setText("Attendance Controls");
            dynamicFields.getChildren().addAll(attendanceEmployee, attendanceDate, attendanceStatus);
        } else if (mode == Mode.PAYROLL) {
            title.setText("Payroll Controls");
            dynamicFields.getChildren().add(payrollMonth);
        } else {
            title.setText("Settings");
        }
    }

    public void setAttendanceEmployeeOptions(Map<Long, String> employees) {
        var options = FXCollections.<EmployeeOption>observableArrayList();
        employees.forEach((id, name) -> options.add(new EmployeeOption(id, name)));
        attendanceEmployee.setItems(options);
    }

    public void setActionListener(ActionListener actionListener) { this.actionListener = actionListener; }
    public void setValidationHook(Predicate<String> validationHook) { this.validationHook = Objects.requireNonNullElse(validationHook, value -> true); }
    public void setSelectionAvailable(boolean available) { this.selectionAvailable.set(available); }
    public void setActionsDisabled(boolean disabled) { this.actionsDisabled.set(disabled); }

    public String getFullNameInput() { return fullNameField.getText(); }
    public String getPositionInput() { return positionField.getText(); }
    public String getMonthlySalaryInput() { return monthlySalaryField.getText(); }

    public Long getAttendanceEmployeeIdInput() {
        EmployeeOption option = attendanceEmployee.getValue();
        return option == null ? null : option.id();
    }
    public LocalDate getAttendanceDateInput() { return attendanceDate.getValue(); }
    public AttendanceRecord.Status getAttendanceStatusInput() { return attendanceStatus.getValue(); }
    public YearMonth getPayrollMonthInput() { return payrollMonth.getValue(); }

    public void fillFrom(Employee employee) {
        if (employee == null) { clearForm(); return; }
        fullNameField.setText(employee.getFullName());
        positionField.setText(employee.getPosition());
        monthlySalaryField.setText(String.valueOf(employee.getMonthlySalary()));
    }

    public void fillAttendance(Long employeeId, LocalDate date, AttendanceRecord.Status status) {
        if (employeeId != null) {
            attendanceEmployee.getItems().stream().filter(option -> option.id().equals(employeeId)).findFirst().ifPresent(attendanceEmployee::setValue);
        }
        attendanceDate.setValue(date);
        attendanceStatus.setValue(status);
    }

    public void clearForm() {
        fullNameField.clear();
        positionField.clear();
        monthlySalaryField.clear();
        attendanceEmployee.setValue(null);
        attendanceDate.setValue(null);
        attendanceStatus.setValue(null);
    }

    public Mode getMode() { return mode; }
    private javafx.stage.Window getWindow() { return getScene() == null ? null : getScene().getWindow(); }

    private record EmployeeOption(Long id, String name) { @Override public String toString() { return name; } }
}

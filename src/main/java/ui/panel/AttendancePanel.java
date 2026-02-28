package ui.panel;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import util.TableConfigurator;
import model.AttendanceRecord;

public class AttendancePanel {
    public static class AttendanceRow {
        private final ObjectProperty<AttendanceRecord> record = new SimpleObjectProperty<>();
        private final StringProperty employeeName = new SimpleStringProperty();

        public AttendanceRow(AttendanceRecord record, String employeeName) {
            this.record.set(record);
            this.employeeName.set(employeeName);
        }

        public AttendanceRecord getRecord() {
            return record.get();
        }

        public ObjectProperty<AttendanceRecord> recordProperty() {
            return record;
        }

        public StringProperty employeeNameProperty() {
            return employeeName;
        }

        public StringProperty dateProperty() {
            return new SimpleStringProperty(getRecord().getDate().format(DateTimeFormatter.ISO_DATE));
        }

        public StringProperty statusProperty() {
            return new SimpleStringProperty(getRecord().getStatus().name());
        }
    }

    public static class FilterRequest {
        private final Long employeeId;
        private final YearMonth month;

        public FilterRequest(Long employeeId, YearMonth month) {
            this.employeeId = employeeId;
            this.month = month;
        }

        public Long employeeId() {
            return employeeId;
        }

        public YearMonth month() {
            return month;
        }
    }

    private final ObservableList<AttendanceRow> rows = FXCollections.observableArrayList();
    private final TableView<AttendanceRow> tableView = new TableView<>(rows);
    private final ComboBox<EmployeeOption> employeeFilter = new ComboBox<>();
    private final ComboBox<YearMonth> monthFilter = new ComboBox<>();

    public Parent createView() {
        Label title = new Label("Attendance Panel");
        title.getStyleClass().add("panel-title");

        setupFilters();

        TableColumn<AttendanceRow, String> employeeCol = new TableColumn<>("Employee Name");
        employeeCol.setCellValueFactory(cell -> cell.getValue().employeeNameProperty());

        TableColumn<AttendanceRow, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell -> cell.getValue().dateProperty());

        TableColumn<AttendanceRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> cell.getValue().statusProperty());

        tableView.getColumns().setAll(employeeCol, dateCol, statusCol);
        tableView.setPlaceholder(new Label("No attendance data yet."));
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getStyleClass().add("data-table");
        TableConfigurator.apply(tableView, "attendance_table");

        HBox filters = new HBox(8, employeeFilter, monthFilter);
        HBox.setHgrow(employeeFilter, Priority.ALWAYS);
        HBox.setHgrow(monthFilter, Priority.NEVER);

        VBox root = new VBox(10, title, filters, tableView);
        root.getStyleClass().add("content-panel");
        VBox.setVgrow(tableView, Priority.ALWAYS);
        return root;
    }

    private void setupFilters() {
        employeeFilter.setPromptText("All Employees");
        monthFilter.setPromptText("Month");
        monthFilter.getItems().setAll(YearMonth.now(), YearMonth.now().minusMonths(1), YearMonth.now().minusMonths(2));
        monthFilter.getSelectionModel().selectFirst();
    }

    public void setEmployeeOptions(Map<Long, String> employees) {
        List<EmployeeOption> options = new ArrayList<>();
        options.add(new EmployeeOption(null, "All Employees"));
        employees.forEach((id, name) -> options.add(new EmployeeOption(id, name)));
        employeeFilter.getItems().setAll(options);
        employeeFilter.getSelectionModel().selectFirst();
    }

    public void setRows(List<AttendanceRecord> records, Map<Long, String> employeeNameMap) {
        List<AttendanceRow> mapped = records.stream()
                .map(record -> new AttendanceRow(record, employeeNameMap.getOrDefault(record.getEmployeeId(), "Unknown")))
                .toList();
        rows.setAll(mapped);
    }

    public void addRow(AttendanceRecord record, String employeeName) {
        rows.add(0, new AttendanceRow(record, employeeName));
    }

    public void updateRow(AttendanceRecord record, String employeeName) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getRecord().getId() == record.getId()) {
                rows.set(i, new AttendanceRow(record, employeeName));
                return;
            }
        }
    }

    public void removeRows(List<Long> ids) {
        LinkedHashSet<Long> set = new LinkedHashSet<>(ids);
        rows.removeIf(row -> set.contains(row.getRecord().getId()));
    }

    public AttendanceRecord getSelectedRecord() {
        AttendanceRow row = tableView.getSelectionModel().getSelectedItem();
        return row == null ? null : row.getRecord();
    }

    public List<AttendanceRecord> getSelectedRecords() {
        return tableView.getSelectionModel().getSelectedItems().stream().map(AttendanceRow::getRecord).toList();
    }

    public void onSelectionChanged(Consumer<AttendanceRecord> listener) {
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> listener.accept(newValue == null ? null : newValue.getRecord()));
    }

    public void onFilterChanged(Consumer<FilterRequest> listener) {
        Runnable emit = () -> {
            EmployeeOption selectedEmployee = employeeFilter.getValue();
            YearMonth month = monthFilter.getValue() == null ? YearMonth.now() : monthFilter.getValue();
            listener.accept(new FilterRequest(selectedEmployee == null ? null : selectedEmployee.id(), month));
        };
        employeeFilter.valueProperty().addListener((obs, oldValue, newValue) -> emit.run());
        monthFilter.valueProperty().addListener((obs, oldValue, newValue) -> emit.run());
    }

    public int selectedCount() {
        return tableView.getSelectionModel().getSelectedItems().size();
    }

    public Long selectedFilterEmployeeId() {
        EmployeeOption option = employeeFilter.getValue();
        return option == null ? null : option.id();
    }

    public YearMonth selectedMonth() {
        return Objects.requireNonNullElse(monthFilter.getValue(), YearMonth.now());
    }

    private record EmployeeOption(Long id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}

package ui.panel;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.property.SimpleStringProperty;
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
import model.PayrollRecord;

public class PayrollPanel {
    public record MonthRequest(YearMonth month) {}

    public static class PayrollRow {
        private final PayrollRecord record;
        private final String employeeName;

        public PayrollRow(PayrollRecord record, String employeeName) {
            this.record = record;
            this.employeeName = employeeName;
        }

        public PayrollRecord record() { return record; }
        public SimpleStringProperty employeeNameProperty() { return new SimpleStringProperty(employeeName); }
        public SimpleStringProperty monthProperty() { return new SimpleStringProperty(record.getMonth().toString()); }
        public SimpleStringProperty baseSalaryProperty() { return currency(record.getBaseSalary()); }
        public SimpleStringProperty effectiveDaysProperty() { return new SimpleStringProperty(String.valueOf(record.getEffectiveWorkDays())); }
        public SimpleStringProperty computedSalaryProperty() { return currency(record.getComputedSalary()); }
        public SimpleStringProperty statusProperty() { return new SimpleStringProperty(record.getStatus().name()); }

        private SimpleStringProperty currency(double value) {
            return new SimpleStringProperty(NumberFormat.getCurrencyInstance(Locale.US).format(value));
        }
    }

    private final ObservableList<PayrollRow> rows = FXCollections.observableArrayList();
    private final TableView<PayrollRow> tableView = new TableView<>(rows);
    private final ComboBox<YearMonth> monthSelector = new ComboBox<>();

    public Parent createView() {
        Label title = new Label("Payroll Panel");
        title.getStyleClass().add("panel-title");

        monthSelector.getItems().setAll(YearMonth.now(), YearMonth.now().minusMonths(1), YearMonth.now().minusMonths(2));
        monthSelector.getSelectionModel().selectFirst();

        TableColumn<PayrollRow, String> employeeCol = new TableColumn<>("Employee Name");
        employeeCol.setCellValueFactory(cell -> cell.getValue().employeeNameProperty());

        TableColumn<PayrollRow, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cell -> cell.getValue().monthProperty());

        TableColumn<PayrollRow, String> baseCol = new TableColumn<>("Base Salary");
        baseCol.setCellValueFactory(cell -> cell.getValue().baseSalaryProperty());

        TableColumn<PayrollRow, String> effectiveCol = new TableColumn<>("Effective Work Days");
        effectiveCol.setCellValueFactory(cell -> cell.getValue().effectiveDaysProperty());

        TableColumn<PayrollRow, String> computedCol = new TableColumn<>("Computed Salary");
        computedCol.setCellValueFactory(cell -> cell.getValue().computedSalaryProperty());

        TableColumn<PayrollRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> cell.getValue().statusProperty());

        tableView.getColumns().setAll(employeeCol, monthCol, baseCol, effectiveCol, computedCol, statusCol);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.setPlaceholder(new Label("No payroll data yet."));
        tableView.getStyleClass().add("data-table");
        TableConfigurator.apply(tableView, "payroll_table");

        HBox top = new HBox(8, new Label("Month:"), monthSelector);
        HBox.setHgrow(monthSelector, Priority.NEVER);

        VBox root = new VBox(10, title, top, tableView);
        root.getStyleClass().add("content-panel");
        VBox.setVgrow(tableView, Priority.ALWAYS);
        return root;
    }

    public void setRows(List<PayrollRecord> records, Map<Long, String> names) {
        rows.setAll(records.stream().map(r -> new PayrollRow(r, names.getOrDefault(r.getEmployeeId(), "Unknown"))).toList());
    }

    public void addRow(PayrollRecord record, String name) { rows.add(0, new PayrollRow(record, name)); }
    public void removeRows(List<Long> ids) { rows.removeIf(r -> ids.contains(r.record().getId())); }

    public List<PayrollRecord> getSelectedRecords() { return tableView.getSelectionModel().getSelectedItems().stream().map(PayrollRow::record).toList(); }
    public int selectedCount() { return tableView.getSelectionModel().getSelectedItems().size(); }

    public void onSelectionChanged(Consumer<Integer> listener) {
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> listener.accept(selectedCount()));
    }

    public void onFilterChanged(Consumer<MonthRequest> listener) {
        monthSelector.valueProperty().addListener((obs, oldV, newV) -> listener.accept(new MonthRequest(selectedMonth())));
    }

    public YearMonth selectedMonth() { return Objects.requireNonNullElse(monthSelector.getValue(), YearMonth.now()); }
}

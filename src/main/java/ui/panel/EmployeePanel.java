package ui.panel;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import util.TableConfigurator;
import model.Employee;

public class EmployeePanel {
    private final ObservableList<Employee> employees = FXCollections.observableArrayList();
    private final TableView<Employee> tableView = new TableView<>(employees);

    public Parent createView() {
        Label title = new Label("Employee Panel");
        title.getStyleClass().add("panel-title");

        TableColumn<Employee, String> fullNameCol = new TableColumn<>("Full Name");
        fullNameCol.setCellValueFactory(cell -> cell.getValue().fullNameProperty());

        TableColumn<Employee, String> positionCol = new TableColumn<>("Position");
        positionCol.setCellValueFactory(cell -> cell.getValue().positionProperty());

        TableColumn<Employee, Number> salaryCol = new TableColumn<>("Monthly Salary");
        salaryCol.setCellValueFactory(cell -> cell.getValue().monthlySalaryProperty());
        salaryCol.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(NumberFormat.getCurrencyInstance(Locale.US).format(item.doubleValue()));
                }
            }
        });

        tableView.getColumns().setAll(fullNameCol, positionCol, salaryCol);
        tableView.setPlaceholder(new Label("No employee data yet."));
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getStyleClass().add("data-table");
        TableConfigurator.apply(tableView, "employee_table");

        VBox root = new VBox(10, title, tableView);
        root.getStyleClass().add("content-panel");
        VBox.setVgrow(tableView, Priority.ALWAYS);
        return root;
    }

    public void setData(List<Employee> data) {
        employees.setAll(data);
    }

    public void addEmployee(Employee employee) {
        employees.add(0, employee);
    }

    public void updateEmployee(Employee employee) {
        for (int i = 0; i < employees.size(); i++) {
            if (employees.get(i).getId() == employee.getId()) {
                employees.set(i, employee);
                tableView.getSelectionModel().select(employee);
                return;
            }
        }
    }

    public void removeEmployeeById(long id) {
        employees.removeIf(emp -> emp.getId() == id);
    }

    public void removeEmployees(Collection<Long> ids) {
        employees.removeIf(emp -> ids.contains(emp.getId()));
    }

    public Employee getSelectedEmployee() {
        return tableView.getSelectionModel().getSelectedItem();
    }

    public List<Employee> getSelectedEmployees() {
        return List.copyOf(tableView.getSelectionModel().getSelectedItems());
    }

    public void onSelectionChanged(Consumer<Employee> listener) {
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> listener.accept(newValue));
    }

    public int selectedCount() {
        return tableView.getSelectionModel().getSelectedItems().size();
    }
}

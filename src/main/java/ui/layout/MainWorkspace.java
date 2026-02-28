package ui.layout;

import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.collections.FXCollections;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ui.navigation.ViewManager;
import ui.panel.AttendancePanel;
import ui.panel.EmployeePanel;
import ui.panel.PayrollPanel;
import ui.panel.SettingsPanel;

public class MainWorkspace extends VBox {
    public static final String EMPLOYEE = "employee";
    public static final String ATTENDANCE = "attendance";
    public static final String PAYROLL = "payroll";
    public static final String SETTINGS = "settings";

    private final ViewManager viewManager = new ViewManager();
    private final EmployeePanel employeePanel = new EmployeePanel();
    private final AttendancePanel attendancePanel = new AttendancePanel();
    private final PayrollPanel payrollPanel = new PayrollPanel();
    private final SettingsPanel settingsPanel = new SettingsPanel();
    private final TextField searchField = new TextField();
    private final ComboBox<String> filterSelector = new ComboBox<>();
    private final ComboBox<String> sortSelector = new ComboBox<>();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(280));
    private Consumer<String> activeTabListener;
    private Consumer<EmployeeQuery> employeeQueryHandler;

    public record EmployeeQuery(String keyword, String positionFilter, String sortKey) {
    }

    public MainWorkspace() {
        getStyleClass().add("workspace-root");
        setSpacing(10);
        setPadding(new Insets(12));

        registerViews();
        HBox toolbar = buildToolbar();
        TabPane tabs = buildTabs();
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().addAll(toolbar, tabs);
    }

    public EmployeePanel getEmployeePanel() { return employeePanel; }
    public AttendancePanel getAttendancePanel() { return attendancePanel; }
    public PayrollPanel getPayrollPanel() { return payrollPanel; }
    public SettingsPanel getSettingsPanel() { return settingsPanel; }

    public void setSearchHandler(Consumer<String> handler) {
        debounce.setOnFinished(e -> handler.accept(searchField.getText()));
        searchField.textProperty().addListener((obs, oldText, newText) -> debounce.playFromStart());
    }

    public void setEmployeeQueryHandler(Consumer<EmployeeQuery> handler) {
        this.employeeQueryHandler = handler;
    }

    public void setEmployeeFilterOptions(java.util.Collection<String> positions) {
        var items = FXCollections.<String>observableArrayList();
        items.add("ALL");
        if (positions != null) {
            items.addAll(positions);
        }
        filterSelector.setItems(items);
        if (filterSelector.getValue() == null || !items.contains(filterSelector.getValue())) {
            filterSelector.getSelectionModel().selectFirst();
        }
    }

    public void setActiveTabListener(Consumer<String> listener) { this.activeTabListener = listener; }

    private void registerViews() {
        viewManager.register(EMPLOYEE, employeePanel::createView);
        viewManager.register(ATTENDANCE, attendancePanel::createView);
        viewManager.register(PAYROLL, payrollPanel::createView);
        viewManager.register(SETTINGS, () -> new Label("Settings loading..."));
    }

    private HBox buildToolbar() {
        searchField.setPromptText("Search by name or position...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        filterSelector.setPromptText("Filter");
        filterSelector.getStyleClass().add("toolbar-chip");
        filterSelector.getItems().setAll("ALL");
        filterSelector.getSelectionModel().selectFirst();

        sortSelector.setPromptText("Sort");
        sortSelector.getStyleClass().add("toolbar-chip");
        sortSelector.getItems().setAll("DEFAULT", "NAME_ASC", "NAME_DESC", "SALARY_ASC", "SALARY_DESC");
        sortSelector.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((obs, oldText, newText) -> debounce.playFromStart());
        filterSelector.valueProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());
        sortSelector.valueProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());
        debounce.setOnFinished(event -> publishEmployeeQuery());

        HBox toolbar = new HBox(8, searchField, filterSelector, sortSelector);
        toolbar.getStyleClass().add("workspace-toolbar");
        toolbar.setPadding(new Insets(8));
        return toolbar;
    }

    private void publishEmployeeQuery() {
        if (employeeQueryHandler == null) {
            return;
        }
        employeeQueryHandler.accept(new EmployeeQuery(
                searchField.getText(),
                filterSelector.getValue(),
                sortSelector.getValue()));
    }

    private TabPane buildTabs() {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("workspace-tabs");
        tabPane.getTabs().add(createTab("Employee Panel", EMPLOYEE));
        tabPane.getTabs().add(createTab("Attendance Panel", ATTENDANCE));
        tabPane.getTabs().add(createTab("Payroll Panel", PAYROLL));
        tabPane.getTabs().add(createTab("Settings Panel", SETTINGS));
        if (!tabPane.getTabs().isEmpty()) {
            Tab first = tabPane.getTabs().get(0);
            first.setContent(viewManager.getView(EMPLOYEE));
            notifyActiveTab(EMPLOYEE);
        }
        return tabPane;
    }

    private Tab createTab(String label, String key) {
        Tab tab = new Tab(label);
        tab.setClosable(false);
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) {
                if (tab.getContent() == null) {
                    Parent view = viewManager.getView(key);
                    tab.setContent(view);
                }
                notifyActiveTab(key);
            }
        });
        return tab;
    }

    private void notifyActiveTab(String key) {
        if (activeTabListener != null) activeTabListener.accept(key);
    }

    public void setSettingsContent(Parent settingsRoot) {
        viewManager.register(SETTINGS, () -> settingsRoot);
    }
}

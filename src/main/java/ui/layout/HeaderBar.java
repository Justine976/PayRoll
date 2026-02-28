package ui.layout;

import config.AppConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class HeaderBar extends HBox {
    private final Label companyValue = new Label("My Company");
    private final Label employeesValue = new Label("0");
    private final Label payrollValue = new Label("0");
    private final Label recentValue = new Label("Idle");

    public HeaderBar(Runnable onLogout) {
        getStyleClass().add("header-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(10));

        Label title = new Label(AppConfig.appName() + " v" + AppConfig.appVersion());
        title.getStyleClass().add("header-title");

        HBox summaryPanel = new HBox(
                summaryCard("Company", companyValue),
                summaryCard("Employees", employeesValue),
                summaryCard("Processed", payrollValue),
                summaryCard("Activity", recentValue));
        summaryPanel.setSpacing(8);
        summaryPanel.getStyleClass().add("summary-panel");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setOnAction(event -> onLogout.run());

        getChildren().addAll(title, summaryPanel, spacer, logoutButton);
    }

    public void setCompanyName(String name) { companyValue.setText(name == null || name.isBlank() ? "My Company" : name); }
    public void setTotalEmployees(int count) { employeesValue.setText(String.valueOf(count)); }
    public void setProcessedPayrollCount(int count) { payrollValue.setText(String.valueOf(count)); }
    public void setRecentActivity(String text) { recentValue.setText(text == null || text.isBlank() ? "Idle" : text); }

    private HBox summaryCard(String label, Label valueNode) {
        Label key = new Label(label + ":");
        key.getStyleClass().add("summary-key");
        valueNode.getStyleClass().add("summary-value");
        HBox box = new HBox(4, key, valueNode);
        box.getStyleClass().add("summary-card");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 8, 4, 8));
        return box;
    }
}

package ui.panel;

import config.ThemeManager;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import model.AppSettings;

public class SettingsPanel {
    public record SettingsRequest(String companyName, String requiredWorkDays, ThemeManager.Theme theme) {}

    private final TextField companyNameField = new TextField();
    private final TextField requiredWorkDaysField = new TextField();
    private final ComboBox<ThemeManager.Theme> themeSelector = new ComboBox<>();
    private final Button saveButton = new Button("Save Settings");
    private final Button backupButton = new Button("Backup Database");
    private final Button restoreButton = new Button("Restore Database");

    public Parent createView(Consumer<SettingsRequest> onSave, Runnable onBackup, Runnable onRestore) {
        Label title = new Label("Settings Panel");
        title.getStyleClass().add("panel-title");

        companyNameField.setPromptText("Company Name");
        requiredWorkDaysField.setPromptText("Required Work Days");
        themeSelector.getItems().setAll(ThemeManager.Theme.LIGHT, ThemeManager.Theme.DARK);

        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(e -> onSave.accept(new SettingsRequest(
                companyNameField.getText(),
                requiredWorkDaysField.getText(),
                themeSelector.getValue())));

        backupButton.getStyleClass().add("secondary-button");
        backupButton.setOnAction(e -> onBackup.run());

        restoreButton.getStyleClass().add("danger-button");
        restoreButton.setOnAction(e -> onRestore.run());

        VBox root = new VBox(10, title, companyNameField, requiredWorkDaysField, themeSelector, saveButton, backupButton, restoreButton);
        root.getStyleClass().add("content-panel");
        root.setPadding(new Insets(12));
        return root;
    }

    public void setValues(AppSettings settings) {
        companyNameField.setText(settings.getCompanyName());
        requiredWorkDaysField.setText(String.valueOf(settings.getRequiredWorkDays()));
        themeSelector.setValue(settings.getTheme());
    }
}

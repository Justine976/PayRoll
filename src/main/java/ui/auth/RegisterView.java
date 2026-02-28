package ui.auth;

import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegisterView {
    public record RegisterRequest(String fullName, String username, char[] password, char[] confirmPassword) {
    }

    public Parent create(Consumer<RegisterRequest> onRegister) {
        Label title = new Label("First-Time Admin Registration");
        title.getStyleClass().add("auth-title");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");

        Button registerButton = new Button("Create Admin Account");
        registerButton.setDefaultButton(true);
        registerButton.setOnAction(event -> onRegister.accept(new RegisterRequest(
                fullNameField.getText(),
                usernameField.getText(),
                passwordField.getText().toCharArray(),
                confirmPasswordField.getText().toCharArray())));

        VBox form = new VBox(10,
                title,
                fullNameField,
                usernameField,
                passwordField,
                confirmPasswordField,
                registerButton);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(24));
        form.setMaxWidth(360);
        form.getStyleClass().add("auth-form");

        VBox wrapper = new VBox(form);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }
}

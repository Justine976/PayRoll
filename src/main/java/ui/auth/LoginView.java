package ui.auth;

import java.util.function.BiConsumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginView {
    public Parent create(BiConsumer<String, char[]> onLogin) {
        Label title = new Label("Login");
        title.getStyleClass().add("auth-title");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Sign In");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> onLogin.accept(usernameField.getText(), passwordField.getText().toCharArray()));

        VBox form = new VBox(10, title, usernameField, passwordField, loginButton);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(24));
        form.setMaxWidth(320);
        form.getStyleClass().add("auth-form");

        VBox wrapper = new VBox(form);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }
}

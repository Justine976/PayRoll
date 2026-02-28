package util;

import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class DialogUtil {
    private DialogUtil() {
    }

    public static void showWarning(Window owner, String title, String message) {
        show(owner, AlertType.WARNING, title, message);
    }

    public static void showError(Window owner, String title, String message) {
        show(owner, AlertType.ERROR, title, message);
    }

    public static void showSuccess(Window owner, String title, String message) {
        show(owner, AlertType.INFORMATION, title, message);
    }

    public static boolean showConfirmation(Window owner, String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (owner != null) {
            alert.initOwner(owner);
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void show(Window owner, AlertType type, String title, String message) {
        Runnable task = () -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
}

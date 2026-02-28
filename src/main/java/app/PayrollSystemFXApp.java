package app;

import config.AppConfig;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.ApplicationLifecycleService;
import ui.MainView;

public class PayrollSystemFXApp extends Application {

    @Override
    public void start(Stage stage) {
        Parent root = new MainView().createRoot();
        Scene scene = new Scene(root, AppConfig.windowWidth(), AppConfig.windowHeight());

        String css = Objects.requireNonNull(getClass().getResource("/styles/global.css")).toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle(AppConfig.appName() + " v" + AppConfig.appVersion());
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() {
        ApplicationLifecycleService.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

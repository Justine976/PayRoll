package config;

import javafx.scene.Scene;

public final class ThemeManager {
    public enum Theme {
        LIGHT("theme-light"),
        DARK("theme-dark");

        private final String styleClass;

        Theme(String styleClass) {
            this.styleClass = styleClass;
        }

        public String styleClass() {
            return styleClass;
        }
    }

    private static Theme activeTheme = Theme.LIGHT;

    private ThemeManager() {
    }

    public static Theme activeTheme() {
        return activeTheme;
    }

    public static void applyTheme(Scene scene, Theme theme) {
        if (scene == null || scene.getRoot() == null || theme == null) {
            return;
        }
        var root = scene.getRoot();
        root.getStyleClass().remove(Theme.LIGHT.styleClass());
        root.getStyleClass().remove(Theme.DARK.styleClass());
        root.getStyleClass().add(theme.styleClass());
        activeTheme = theme;
    }

    public static void toggleTheme(Scene scene) {
        applyTheme(scene, activeTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT);
    }
}

package config;

import util.AppConstants;

public final class AppConfig {
    private AppConfig() {
    }

    public static String appName() { return AppConstants.APP_NAME; }
    public static String appVersion() { return AppConstants.APP_VERSION; }
    public static String appVendor() { return AppConstants.APP_VENDOR; }
    public static String appDescription() { return AppConstants.APP_DESCRIPTION; }
    public static int windowWidth() { return AppConstants.DEFAULT_WINDOW_WIDTH; }
    public static int windowHeight() { return AppConstants.DEFAULT_WINDOW_HEIGHT; }
}

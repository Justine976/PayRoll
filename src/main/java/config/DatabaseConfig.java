package config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import util.AppConstants;

public final class DatabaseConfig {
    private static final String DATA_DIR = "data";
    private static final String FALLBACK_DIR = ".payrollsystemfx";
    private static volatile Path resolvedDatabasePath;

    private DatabaseConfig() {
    }

    public static Path appRootPath() {
        return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
    }

    public static Path dataPath() {
        return appRootPath().resolve(DATA_DIR);
    }

    public static Path databasePath() {
        Path cached = resolvedDatabasePath;
        if (cached != null) {
            return cached;
        }

        synchronized (DatabaseConfig.class) {
            if (resolvedDatabasePath != null) {
                return resolvedDatabasePath;
            }

            Path portable = dataPath().resolve(AppConstants.DB_FILE_NAME);
            if (canUse(portable)) {
                resolvedDatabasePath = portable;
                return resolvedDatabasePath;
            }

            String home = System.getProperty("user.home", ".");
            Path fallback = Paths.get(home, FALLBACK_DIR, DATA_DIR, AppConstants.DB_FILE_NAME).toAbsolutePath();
            resolvedDatabasePath = fallback;
            return resolvedDatabasePath;
        }
    }

    public static String jdbcUrl() {
        return "jdbc:sqlite:" + databasePath();
    }

    private static boolean canUse(Path filePath) {
        try {
            Path parent = filePath.getParent();
            if (parent == null) {
                return false;
            }
            Files.createDirectories(parent);
            return Files.isWritable(parent);
        } catch (Exception ex) {
            return false;
        }
    }
}

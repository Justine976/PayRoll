package config;

import java.nio.file.Path;
import java.nio.file.Paths;
import util.AppConstants;

public final class DatabaseConfig {
    private static final String DATA_DIR = "data";

    private DatabaseConfig() {
    }

    public static Path appRootPath() {
        return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
    }

    public static Path dataPath() {
        return appRootPath().resolve(DATA_DIR);
    }

    public static Path databasePath() {
        return dataPath().resolve(AppConstants.DB_FILE_NAME);
    }

    public static String jdbcUrl() {
        return "jdbc:sqlite:" + databasePath();
    }
}

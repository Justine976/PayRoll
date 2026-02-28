package service;

import config.SessionManager;
import database.SQLiteConnectionManager;

public final class ApplicationLifecycleService {
    private ApplicationLifecycleService() {
    }

    public static void shutdown() {
        SessionManager.clear();
        SQLiteConnectionManager.getInstance().close();
    }
}

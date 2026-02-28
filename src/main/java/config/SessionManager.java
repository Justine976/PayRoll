package config;

import model.User;

public final class SessionManager {
    private static User currentUser;

    private SessionManager() {
    }

    public static synchronized void setCurrentUser(User user) {
        currentUser = user;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized boolean isAuthenticated() {
        return currentUser != null;
    }

    public static synchronized void clear() {
        currentUser = null;
    }
}

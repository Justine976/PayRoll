package util;

public final class ValidationUtil {
    private ValidationUtil() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean maxLength(String value, int max) {
        return value == null || value.length() <= max;
    }

    public static boolean isPositive(long value) {
        return value > 0;
    }

    public static boolean usernameFormat(String username) {
        return username != null && username.matches("[A-Za-z0-9_.-]{3,40}");
    }
}

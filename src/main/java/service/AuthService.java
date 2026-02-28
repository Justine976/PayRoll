package service;

import config.SessionManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import model.User;
import repository.UserRepository;
import util.PasswordUtil;
import util.ValidationUtil;

public class AuthService {
    private UserRepository userRepository;

    private UserRepository repository() {
        if (userRepository == null) {
            userRepository = new UserRepository();
        }
        return userRepository;
    }

    public void initialize() throws SQLException {
        repository().ensureSchema();
    }

    public boolean isRegistrationRequired() throws SQLException {
        return !repository().hasAnyUser();
    }

    public void registerAdmin(String fullName, String username, char[] password, char[] confirmPassword) throws SQLException {
        validateRegistrationInput(fullName, username, password, confirmPassword);
        if (!isRegistrationRequired()) {
            throw new IllegalStateException("Registration is disabled after first account creation.");
        }

        if (!ValidationUtil.usernameFormat(username.trim())) {
            throw new IllegalArgumentException("Username must be 3-40 chars (letters, numbers, _, ., -).");
        }

        if (repository().existsByUsername(username.trim())) {
            throw new IllegalArgumentException("Username already exists.");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setUsername(username.trim());
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        repository().save(user);
    }

    public boolean login(String username, char[] password) throws SQLException {
        if (ValidationUtil.isBlank(username) || password == null || password.length == 0) {
            return false;
        }

        Optional<User> user = repository().findByUsername(username.trim());
        if (user.isEmpty()) {
            PasswordUtil.verifyPassword(password, "65536:c2FsdFNhbXBsZTEyMzQ1Ng==:l2wH0E0aJlvn9GQW6nmkepXf2V3aGCUelxQVAkbQ+Eg=");
            return false;
        }

        boolean matched = PasswordUtil.verifyPassword(password, user.get().getPasswordHash());
        if (matched) {
            SessionManager.setCurrentUser(user.get());
            return true;
        }

        return false;
    }

    public void logout() {
        SessionManager.clear();
    }

    private void validateRegistrationInput(String fullName, String username, char[] password, char[] confirmPassword) {
        if (ValidationUtil.isBlank(fullName) || ValidationUtil.isBlank(username)
                || password == null || confirmPassword == null
                || password.length == 0 || confirmPassword.length == 0) {
            throw new IllegalArgumentException("All fields are required.");
        }

        if (!PasswordUtil.constantTimeEquals(password, confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
    }
}

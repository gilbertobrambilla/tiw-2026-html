package it.polimi.tiw.dao;

import it.polimi.tiw.beans.User;
import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.beans.UserRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object used for user-related database operations.
 */
public class UserDAO {
    private final Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    public UserLogin getUserByCredentials(String username, String password) throws SQLException {
        String query = "SELECT id, username, first_name, last_name, role, photo FROM users WHERE username = ? AND password = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    return new UserLogin(
                            result.getInt("id"),
                            result.getString("username"),
                            result.getString("first_name"),
                            result.getString("last_name"),
                            UserRole.valueOf(result.getString("role"))
                    );
                }
            }
        }
        return null;
    }

    public List<User> getUsersByRole(String role) throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT id, username, first_name, last_name FROM users WHERE role = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, role);

            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    User user = new User(
                            result.getInt("id"),
                            result.getString("username"),
                            result.getString("first_name"),
                            result.getString("last_name")
                    );
                    users.add(user);
                }
            }
        }
        return users;
    }

    public UserRole getRoleByUser(int userId) throws SQLException {
        String query = "SELECT role FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);

            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    // user role is extracted
                    return UserRole.valueOf(result.getString("role"));
                }
            }
        }
        return null;
    }
}

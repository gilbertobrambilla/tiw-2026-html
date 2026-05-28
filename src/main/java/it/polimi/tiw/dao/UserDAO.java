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

    public boolean isValidTechnicalCollaborator(int userId, int managerId) throws SQLException {
        String query = "SELECT COUNT(*) AS valid_count " +
                "FROM users " +
                "WHERE id = ? AND role = 'technical' AND id <> ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, managerId);

            try (ResultSet result = stmt.executeQuery()) {
                return result.next() && result.getInt("valid_count") == 1;
            }
        }
    }

    public List<User> getAvailableCollaboratorsByManager(int managerId) throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT id, username, first_name, last_name FROM users WHERE role = 'technical' AND id != ? ORDER BY last_name ASC, first_name ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, managerId);

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

    public List<User> getCollaboratorsByManager(int managerId) throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT DISTINCT u.id, u.username, u.first_name, u.last_name FROM users u " +
                "JOIN task_workers tw ON u.id = tw.user_id " +
                "JOIN tasks t ON tw.task_id = t.id " +
                "JOIN wp w ON t.wp_id = w.id " +
                "JOIN projects p ON w.project_id = p.id " +
                "WHERE p.managed_by = ? " +
                "ORDER BY u.last_name ASC, u.first_name ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, managerId);

            // distinct collaborators assigned to the manager projects are retrieved
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

    public List<User> getCollaboratorsByTask(int taskId) throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT u.id, u.username, u.first_name, u.last_name FROM users u " +
                "JOIN task_workers tw ON u.id = tw.user_id " +
                "WHERE tw.task_id = ? " +
                "ORDER BY u.last_name ASC, u.first_name ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);

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

    public User getUserById(int userId) throws SQLException {
        String query = "SELECT id, username, first_name, last_name FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("first_name"),
                            rs.getString("last_name")
                    );
                }
            }
        }
        return null;
    }

    public List<Integer> getCollaboratorIdsByTask(int taskId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String query = "SELECT user_id FROM task_workers WHERE task_id = ?";

        // basic numerical ids are extracted for view checkbox population
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("user_id"));
                }
            }
        }
        return ids;
    }
}

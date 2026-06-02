package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TaskHoursDAO {
    private final Connection connection;

    public TaskHoursDAO(Connection connection) {
        this.connection = connection;
    }

    public int getTotalExpectedHoursByTask(int taskId) throws SQLException {
        String query = "SELECT COALESCE(SUM(hours), 0) AS total_expected FROM task_expected_hours WHERE task_id = ?";
        int totalExpected = 0;

        // expected hours sum is retrieved from database
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalExpected = rs.getInt("total_expected");
                }
            }
        }
        return totalExpected;
    }

    public int getTotalWorkedHoursByTaskAndUser(int taskId, int userId) throws SQLException {
        String query = "SELECT COALESCE(SUM(hours), 0) AS total_worked FROM task_worked_hours WHERE task_id = ? AND user_id = ?";
        int totalWorked = 0;

        // personal worked hours sum is retrieved from database
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalWorked = rs.getInt("total_worked");
                }
            }
        }
        return totalWorked;
    }

    public void saveWorkedHours(int taskId, int userId, int monthIndex, int hours) throws SQLException {
        String selectQuery = "SELECT * FROM task_worked_hours WHERE task_id = ? AND user_id = ? AND month_index = ?";
        boolean existingWorkedHours;

        // existing worked-hours row is checked
        try (PreparedStatement stmt = connection.prepareStatement(selectQuery)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            stmt.setInt(3, monthIndex);

            try (ResultSet result = stmt.executeQuery()) {
                existingWorkedHours = result.next();
            }
        }

        if (existingWorkedHours) {
            String updateQuery = "UPDATE task_worked_hours SET hours = ? WHERE task_id = ? AND user_id = ? AND month_index = ?";

            try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
                stmt.setInt(1, hours);
                stmt.setInt(2, taskId);
                stmt.setInt(3, userId);
                stmt.setInt(4, monthIndex);
                stmt.executeUpdate();
            }
        } else {
            String insertQuery = "INSERT INTO task_worked_hours (task_id, user_id, month_index, hours) VALUES (?, ?, ?, ?)";

            try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
                stmt.setInt(1, taskId);
                stmt.setInt(2, userId);
                stmt.setInt(3, monthIndex);
                stmt.setInt(4, hours);
                stmt.executeUpdate();
            }
        }
    }

    public void saveExpectedHours(int taskId, int monthIndex, int hours) throws SQLException {
        String selectQuery = "SELECT * FROM task_expected_hours WHERE task_id = ? AND month_index = ?";
        boolean existingExpectedHours;

        // existing expected-hours row is checked
        try (PreparedStatement stmt = connection.prepareStatement(selectQuery)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, monthIndex);

            try (ResultSet result = stmt.executeQuery()) {
                existingExpectedHours = result.next();
            }
        }

        if (existingExpectedHours) {
            String updateQuery = "UPDATE task_expected_hours SET hours = ? WHERE task_id = ? AND month_index = ?";

            try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
                stmt.setInt(1, hours);
                stmt.setInt(2, taskId);
                stmt.setInt(3, monthIndex);
                stmt.executeUpdate();
            }
        } else {
            String insertQuery = "INSERT INTO task_expected_hours (task_id, month_index, hours) VALUES (?, ?, ?)";

            try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
                stmt.setInt(1, taskId);
                stmt.setInt(2, monthIndex);
                stmt.setInt(3, hours);
                stmt.executeUpdate();
            }
        }
    }

    public Map<Integer, Integer> getExpectedHoursByTask(int taskId) throws SQLException {
        Map<Integer, Integer> expectedHours = new HashMap<>();
        String query = "SELECT month_index, hours FROM task_expected_hours WHERE task_id = ? ORDER BY month_index ASC";

        // monthly expected hours are extracted and mapped
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expectedHours.put(rs.getInt("month_index"), rs.getInt("hours"));
                }
            }
        }
        return expectedHours;
    }

    public Map<Integer, Integer> getExpectedHoursByTask(int taskId, int startMonth, int endMonth) throws SQLException {
        Map<Integer, Integer> expectedHours = createEmptyExpectedHoursMap(startMonth, endMonth);
        String query = "SELECT month_index, hours FROM task_expected_hours WHERE task_id = ? AND month_index BETWEEN ? AND ? ORDER BY month_index ASC";

        // monthly expected hours are extracted and mapped without replacing missing rows with zero
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, startMonth);
            stmt.setInt(3, endMonth);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expectedHours.put(rs.getInt("month_index"), rs.getInt("hours"));
                }
            }
        }
        return expectedHours;
    }

    public Map<Integer, Integer> getWorkedHoursByTaskAndUser(int taskId, int userId) throws SQLException {
        Map<Integer, Integer> workedHours = new HashMap<>();
        String query = "SELECT month_index, hours FROM task_worked_hours WHERE task_id = ? AND user_id = ? ORDER BY month_index ASC";

        // monthly worked hours are extracted and mapped
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    workedHours.put(rs.getInt("month_index"), rs.getInt("hours"));
                }
            }
        }
        return workedHours;
    }

    private Map<Integer, Integer> createEmptyExpectedHoursMap(int startMonth, int endMonth) {
        Map<Integer, Integer> expectedHours = new LinkedHashMap<>();
        for (int month = startMonth; month <= endMonth; month++) {
            expectedHours.put(month, null);
        }
        return expectedHours;
    }
}

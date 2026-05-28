package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
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

    public int getWorkedHoursByTaskAndUserAndMonth(int taskId, int userId, int monthIndex) throws SQLException {
        String query = "SELECT hours FROM task_worked_hours WHERE task_id = ? AND user_id = ? AND month_index = ?";
        int hours = 0;

        // specific month worked hours are fetched
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            stmt.setInt(3, monthIndex);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    hours = rs.getInt("hours");
                }
            }
        }
        return hours;
    }

    public void saveWorkedHours(int taskId, int userId, int monthIndex, int hours) throws SQLException {
        String query = "INSERT INTO task_worked_hours (task_id, user_id, month_index, hours) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE hours = ?";

        // worked hours are inserted or updated if already existing
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            stmt.setInt(3, monthIndex);
            stmt.setInt(4, hours);
            stmt.setInt(5, hours);
            stmt.executeUpdate();
        }
    }

    public void saveExpectedHours(int taskId, int monthIndex, int hours) throws SQLException {
        String query = "INSERT INTO task_expected_hours (task_id, month_index, hours) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE hours = ?";

        // expected hours are inserted or updated on duplicate key
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, monthIndex);
            stmt.setInt(3, hours);
            stmt.setInt(4, hours);
            stmt.executeUpdate();
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
}

package it.polimi.tiw.dao;

import it.polimi.tiw.beans.Task;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object used for task-related database operations.
 */
public class TaskDAO {
    private final Connection connection;

    public TaskDAO(Connection connection) {
        this.connection = connection;
    }

    public void createTask(int wpId, String title, String description, int startMonth, int endMonth) throws SQLException {
        String seqQuery = "SELECT COALESCE(MAX(sequence_number), 0) + 1 AS next_seq FROM tasks WHERE wp_id = ?";
        String insertQuery = "INSERT INTO tasks (wp_id, sequence_number, title, description, start_month, end_month) VALUES (?, ?, ?, ?, ?, ?)";

        int nextSeq = 1;

        // sequence number assigned dynamically based on the parent wp
        try (PreparedStatement seqStmt = connection.prepareStatement(seqQuery)) {
            seqStmt.setInt(1, wpId);
            try (ResultSet rs = seqStmt.executeQuery()) {
                if (rs.next()) {
                    nextSeq = rs.getInt("next_seq");
                }
            }
        }

        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            insertStmt.setInt(1, wpId);
            insertStmt.setInt(2, nextSeq);
            insertStmt.setString(3, title);
            insertStmt.setString(4, description);
            insertStmt.setInt(5, startMonth);
            insertStmt.setInt(6, endMonth);
            insertStmt.executeUpdate();
        }
    }

    public List<Task> getTasksByWpAndUser(int wpId, int userId) throws SQLException {
        List<Task> taskList = new ArrayList<>();
        // double join required to link tasks to wp and finally to projects for creator validation
        String query = "SELECT t.* FROM tasks t " +
                       "JOIN wp ON t.wp_id = wp.id " +
                       "JOIN projects p ON wp.project_id = p.id " +
                       "WHERE t.wp_id = ? AND p.created_by = ? " +
                       "ORDER BY t.sequence_number ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, wpId);
            stmt.setInt(2, userId);

            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    Task task = new Task(
                            result.getInt("id"),
                            result.getInt("wp_id"),
                            result.getInt("sequence_number"),
                            result.getString("title"),
                            result.getString("description"),
                            result.getInt("start_month"),
                            result.getInt("end_month")
                    );
                    taskList.add(task);
                }
            }
        }
        return taskList;
    }

    public List<Task> getTasksByProjectAndUser(int projectId, int creatorId) throws SQLException {
        List<Task> taskList = new ArrayList<>();
        // double join required to link tasks to wp and finally to projects for creator validation
        String query = "SELECT t.* FROM tasks t " +
                       "JOIN wp ON t.wp_id = wp.id " +
                       "JOIN projects p ON wp.project_id = p.id " +
                       "WHERE p.id = ? AND p.created_by = ? " +
                       "ORDER BY wp.sequence_number ASC, t.sequence_number ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, projectId);
            stmt.setInt(2, creatorId);

            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    Task task = new Task(
                            result.getInt("id"),
                            result.getInt("wp_id"),
                            result.getInt("sequence_number"),
                            result.getString("title"),
                            result.getString("description"),
                            result.getInt("start_month"),
                            result.getInt("end_month")
                    );
                    taskList.add(task);
                }
            }
        }
        return taskList;
    }
}

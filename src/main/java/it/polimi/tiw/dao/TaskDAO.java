package it.polimi.tiw.dao;

import it.polimi.tiw.beans.ProjectStatus;
import it.polimi.tiw.beans.Task;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    public List<Task> getTasksByWpAndWorker(int wpId, int workerId) throws SQLException {
        List<Task> taskList = new ArrayList<>();
        String query = "SELECT t.* FROM tasks t JOIN task_workers tw ON t.id = tw.task_id WHERE t.wp_id = ? AND tw.user_id = ? ORDER BY t.sequence_number ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, wpId);
            stmt.setInt(2, workerId);

            // tasks specifically assigned to the worker are retrieved
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

    public Task getTaskByIdAndWorker(int taskId, int workerId) throws SQLException {
        String query = "SELECT t.* FROM tasks t JOIN task_workers tw ON t.id = tw.task_id WHERE t.id = ? AND tw.user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, workerId);

            // worker assignment to the specific task is strictly validated
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    return new Task(
                            result.getInt("id"),
                            result.getInt("wp_id"),
                            result.getInt("sequence_number"),
                            result.getString("title"),
                            result.getString("description"),
                            result.getInt("start_month"),
                            result.getInt("end_month")
                    );
                }
            }
        }
        return null;
    }

    public void deleteWorkersByTask(int taskId) throws SQLException {
        String query = "DELETE FROM task_workers WHERE task_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.executeUpdate();
        }
    }

    public void addWorkerToTask(int taskId, int userId) throws SQLException {
        String query = "INSERT INTO task_workers (task_id, user_id) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public Task getTaskByIdAndManager(int taskId, int managerId) throws SQLException {
        // double join prevents unauthorized access to tasks belonging to other managers
        String query = "SELECT t.* FROM tasks t " +
                "JOIN wp w ON t.wp_id = w.id " +
                "JOIN projects p ON w.project_id = p.id " +
                "WHERE t.id = ? AND p.managed_by = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, managerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Task(
                            rs.getInt("id"),
                            rs.getInt("wp_id"),
                            rs.getInt("sequence_number"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getInt("start_month"),
                            rs.getInt("end_month")
                    );
                }
            }
        }
        return null;
    }

    public Task getTaskByIdAndManagerAndProjectStatus(int taskId, int managerId, ProjectStatus projectStatus) throws SQLException {
        String query = "SELECT t.* FROM tasks t " +
                "JOIN wp w ON t.wp_id = w.id " +
                "JOIN projects p ON w.project_id = p.id " +
                "WHERE t.id = ? AND p.managed_by = ? AND p.status = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, managerId);
            stmt.setString(3, projectStatus.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Task(
                            rs.getInt("id"),
                            rs.getInt("wp_id"),
                            rs.getInt("sequence_number"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getInt("start_month"),
                            rs.getInt("end_month")
                    );
                }
            }
        }
        return null;
    }

    public int getProjectIdByTask(int taskId) throws SQLException {
        // project id is extracted traversing upward the wp relationship
        String query = "SELECT w.project_id FROM wp w JOIN tasks t ON w.id = t.wp_id WHERE t.id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("project_id");
                }
            }
        }
        return -1;
    }
}

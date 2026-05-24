package it.polimi.tiw.dao;

import it.polimi.tiw.beans.Project;
import it.polimi.tiw.beans.ProjectStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object used for project-related database operations.
 */
public class ProjectDAO {
    private final Connection connection;

    public ProjectDAO(Connection connection) {
        this.connection = connection;
    }

    public void createProject(String title, int durationMonths, int createdBy, int managedBy) throws SQLException {
        String query = "INSERT INTO projects (title, duration_months, created_by, managed_by) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.setInt(2, durationMonths);
            stmt.setInt(3, createdBy);
            stmt.setInt(4, managedBy);
            stmt.executeUpdate();
        }
    }

    public List<Project> getProjectsByCreator(int creatorId) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String query = "SELECT * FROM projects WHERE created_by = ? ORDER BY id DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, creatorId);

            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    // inner user objects are set to null as they require separate queries or joins if needed later
                    Project project = new Project(
                            result.getInt("id"),
                            result.getString("title"),
                            result.getInt("duration_months"),
                            ProjectStatus.valueOf(result.getString("status")),
                            null,
                            null
                    );
                    projects.add(project);
                }
            }
        }
        return projects;
    }

    public Project getProjectByIdAndCreator(int projectId, int creatorId) throws SQLException {
        String query = "SELECT * FROM projects WHERE id = ? AND created_by = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, projectId);
            stmt.setInt(2, creatorId);

            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    return new Project(
                            result.getInt("id"),
                            result.getString("title"),
                            result.getInt("duration_months"),
                            ProjectStatus.valueOf(result.getString("status")),
                            null,
                            null
                    );
                }
            }
        }
        return null;
    }

    public List<Project> getProjectsByCreatorAndStatus(int creatorId, ProjectStatus status) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String query = "SELECT * FROM projects WHERE created_by = ? AND status = ? ORDER BY id DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, creatorId);
            stmt.setString(2, status.name().toLowerCase());

            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    Project project = new Project(
                            result.getInt("id"),
                            result.getString("title"),
                            result.getInt("duration_months"),
                            status,
                            null,
                            null
                    );
                    projects.add(project);
                }
            }
        }
        return projects;
    }

    public boolean isProjectClosable(int projectId) throws SQLException {
        String query = "SELECT COUNT(*) AS uncompleted_tasks_count FROM project_wp_tasks_summary_view WHERE project_id = ? AND total_worked_hours < total_expected_hours";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, projectId);

            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    // true is returned if no uncompleted tasks are found
                    return result.getInt("uncompleted_tasks_count") == 0;
                }
            }
        }
        return false;
    }

    public List<Project> getProjectsByWorker(int workerId) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String query = "SELECT DISTINCT p.* FROM projects p JOIN wp w ON p.id = w.project_id JOIN tasks t ON w.id = t.wp_id JOIN task_workers tw ON t.id = tw.task_id WHERE tw.user_id = ? ORDER BY p.id DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, workerId);

            // assigned projects are fetched avoiding duplicates via distinct keyword
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    Project project = new Project(
                            result.getInt("id"),
                            result.getString("title"),
                            result.getInt("duration_months"),
                            ProjectStatus.valueOf(result.getString("status")),
                            null,
                            null
                    );
                    projects.add(project);
                }
            }
        }
        return projects;
    }

    public Project getProjectByIdAndWorker(int projectId, int workerId) throws SQLException {
        String query = "SELECT DISTINCT p.* FROM projects p JOIN wp w ON p.id = w.project_id JOIN tasks t ON w.id = t.wp_id JOIN task_workers tw ON t.id = tw.task_id WHERE p.id = ? AND tw.user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, projectId);
            stmt.setInt(2, workerId);

            // worker assignment to the specific project is verified
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    return new Project(
                            result.getInt("id"),
                            result.getString("title"),
                            result.getInt("duration_months"),
                            ProjectStatus.valueOf(result.getString("status")),
                            null,
                            null
                    );
                }
            }
        }
        return null;
    }
}

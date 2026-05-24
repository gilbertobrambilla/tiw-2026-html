package it.polimi.tiw.dao;

import it.polimi.tiw.beans.Project;
import it.polimi.tiw.beans.ProjectStatus;
import it.polimi.tiw.beans.WP;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access object used for work-package-related database operations.
 */
public class WPDAO {
    private final Connection connection;

    public WPDAO(Connection connection) {
        this.connection = connection;
    }

    public void createWP(int projectId, String title, int startMonth, int endMonth) throws SQLException {
        String sequenceQuery = "SELECT COALESCE(MAX(sequence_number), 0) + 1 AS next_seq FROM wp WHERE project_id = ?";
        String insertQuery = "INSERT INTO wp (project_id, sequence_number, title, start_month, end_month) VALUES (?, ?, ?, ?, ?)";

        int nextSeq = 1;

        // sequence number calculation executed within the same connection scope
        try (PreparedStatement seqStmt = connection.prepareStatement(sequenceQuery)) {
            seqStmt.setInt(1, projectId);
            try (ResultSet rs = seqStmt.executeQuery()) {
                if (rs.next()) {
                    nextSeq = rs.getInt("next_seq");
                }
            }
        }

        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            insertStmt.setInt(1, projectId);
            insertStmt.setInt(2, nextSeq);
            insertStmt.setString(3, title);
            insertStmt.setInt(4, startMonth);
            insertStmt.setInt(5, endMonth);
            insertStmt.executeUpdate();
        }
    }

    public List<WP> getWPsByProjectAndCreator(int projectId, int creatorId) throws SQLException {
        List<WP> wpList = new ArrayList<>();
        // join used to enforce authorization control on project creator
        String query = "SELECT wp.* FROM wp JOIN projects p ON wp.project_id = p.id WHERE wp.project_id = ? AND p.created_by = ? ORDER BY wp.sequence_number ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, projectId);
            stmt.setInt(2, creatorId);

            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    WP wp = new WP(
                            result.getInt("id"),
                            result.getInt("project_id"),
                            result.getInt("sequence_number"),
                            result.getString("title"),
                            result.getInt("start_month"),
                            result.getInt("end_month")
                    );
                    wpList.add(wp);
                }
            }
        }
        return wpList;
    }

    public WP getWPByIdAndCreator(int wpId, int creatorId) throws SQLException {
        // join used to verify that the retrieved wp belongs to a project owned by the user
        String query = "SELECT wp.* FROM wp JOIN projects p ON wp.project_id = p.id WHERE wp.id = ? AND p.created_by = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, wpId);
            stmt.setInt(2, creatorId);

            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    return new WP(
                            result.getInt("id"),
                            result.getInt("project_id"),
                            result.getInt("sequence_number"),
                            result.getString("title"),
                            result.getInt("start_month"),
                            result.getInt("end_month")
                    );
                }
            }
        }
        return null;
    }

    public Map<Project, List<WP>> getWPGroupedByProject(int creatorId) throws SQLException {
        // LinkedHashMap is crucial here to preserve the SQL ORDER BY arrangement
        Map<Project, List<WP>> groupedWPs = new LinkedHashMap<>();

        // Joining projects and wp, filtering by creator and 'created' status
        String query = "SELECT p.id AS p_id, p.title AS p_title, p.duration_months, p.status, " +
                "wp.id AS wp_id, wp.sequence_number, wp.title AS wp_title, wp.start_month, wp.end_month " +
                "FROM projects p " +
                "JOIN wp ON p.id = wp.project_id " +
                "WHERE p.created_by = ? AND p.status = 'created' " +
                "ORDER BY p.id DESC, wp.sequence_number ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, creatorId);

            try (ResultSet result = stmt.executeQuery()) {
                Project currentProject = null;
                List<WP> currentWpList = null;

                while (result.next()) {
                    int projectId = result.getInt("p_id");

                    // If it's a new project, create the Project bean and add it to the map
                    if (currentProject == null || currentProject.getId() != projectId) {
                        currentProject = new Project(
                                projectId,
                                result.getString("p_title"),
                                result.getInt("duration_months"),
                                ProjectStatus.valueOf(result.getString("status")),
                                null,
                                null
                        );
                        currentWpList = new ArrayList<>();
                        groupedWPs.put(currentProject, currentWpList);
                    }

                    // Create the WP bean and add it to the current project's list
                    WP wp = new WP(
                            result.getInt("wp_id"),
                            projectId,
                            result.getInt("sequence_number"),
                            result.getString("wp_title"),
                            result.getInt("start_month"),
                            result.getInt("end_month")
                    );
                    currentWpList.add(wp);
                }
            }
        }
        return groupedWPs;
    }

    public List<WP> getWPsByProjectAndWorker(int projectId, int workerId) throws SQLException {
        List<WP> wpList = new ArrayList<>();
        String query = "SELECT DISTINCT w.* FROM wp w JOIN tasks t ON w.id = t.wp_id JOIN task_workers tw ON t.id = tw.task_id WHERE w.project_id = ? AND tw.user_id = ? ORDER BY w.sequence_number ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, projectId);
            stmt.setInt(2, workerId);

            // wps containing tasks assigned to the worker are extracted
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    WP wp = new WP(
                            result.getInt("id"),
                            result.getInt("project_id"),
                            result.getInt("sequence_number"),
                            result.getString("title"),
                            result.getInt("start_month"),
                            result.getInt("end_month")
                    );
                    wpList.add(wp);
                }
            }
        }
        return wpList;
    }
}

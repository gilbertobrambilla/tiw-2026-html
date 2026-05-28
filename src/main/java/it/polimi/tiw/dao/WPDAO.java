package it.polimi.tiw.dao;

import it.polimi.tiw.beans.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        // linked hash map is used to preserve the sql order
        Map<Project, List<WP>> groupedWPs = new LinkedHashMap<>();

        // project columns are added to selection
        String query = "SELECT p.id AS p_id, p.title AS p_title, p.duration_months, p.status, p.created_by, p.managed_by, " +
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

                    // project is instantiated when id changes
                    if (currentProject == null || currentProject.getId() != projectId) {
                        currentProject = new Project(
                                projectId,
                                result.getString("p_title"),
                                result.getInt("duration_months"),
                                ProjectStatus.valueOf(result.getString("status")),
                                result.getInt("created_by"),
                                result.getInt("managed_by")
                        );
                        currentWpList = new ArrayList<>();
                        groupedWPs.put(currentProject, currentWpList);
                    }

                    // wp bean is instantiated and added to the list
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

    public List<ProjectStructure> getProjectStructureByManager(int managerId) throws SQLException {
        List<ProjectStructure> structure = new ArrayList<>();
        String query = "SELECT p.id AS p_id, p.title AS p_title, p.duration_months, p.status, p.created_by, p.managed_by, " +
                "w.id AS w_id, w.sequence_number AS w_seq, w.title AS w_title, w.start_month AS w_start, w.end_month AS w_end, " +
                "t.id AS t_id, t.sequence_number AS t_seq, t.title AS t_title, t.description, t.start_month AS t_start, t.end_month AS t_end " +
                "FROM projects p " +
                "JOIN wp w ON p.id = w.project_id " +
                "JOIN tasks t ON w.id = t.wp_id " +
                "WHERE p.managed_by = ? AND p.status = 'created' " +
                "ORDER BY p.id DESC, w.sequence_number ASC, t.sequence_number ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, managerId);

            try (ResultSet rs = stmt.executeQuery()) {
                Project currentProject = null;
                List<WPWithTasks> currentWpList = null;
                WP currentWp = null;
                List<Task> currentTaskList = null;

                while (rs.next()) {
                    int projectId = rs.getInt("p_id");
                    int wpId = rs.getInt("w_id");

                    // project is instantiated when id changes
                    if (currentProject == null || currentProject.getId() != projectId) {
                        currentProject = new Project(
                                projectId,
                                rs.getString("p_title"),
                                rs.getInt("duration_months"),
                                ProjectStatus.valueOf(rs.getString("status")),
                                rs.getInt("created_by"),
                                rs.getInt("managed_by")
                        );
                        currentWpList = new ArrayList<>();
                        structure.add(new ProjectStructure(currentProject, currentWpList));
                    }

                    // wp data is instantiated when id changes
                    if (currentWp == null || currentWp.getId() != wpId) {
                        currentWp = new WP(
                                wpId,
                                projectId,
                                rs.getInt("w_seq"),
                                rs.getString("w_title"),
                                rs.getInt("w_start"),
                                rs.getInt("w_end")
                        );
                        currentTaskList = new ArrayList<>();
                        if (currentWpList != null) {
                            currentWpList.add(new WPWithTasks(currentWp, currentTaskList));
                        }
                    }

                    // task is added to the list
                    Task task = new Task(
                            rs.getInt("t_id"),
                            wpId,
                            rs.getInt("t_seq"),
                            rs.getString("t_title"),
                            rs.getString("description"),
                            rs.getInt("t_start"),
                            rs.getInt("t_end")
                    );
                    if (currentTaskList != null) {
                        currentTaskList.add(task);
                    }
                }
            }
        }
        return structure;
    }
}

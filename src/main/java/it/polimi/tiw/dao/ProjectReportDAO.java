package it.polimi.tiw.dao;

import it.polimi.tiw.beans.Task;
import it.polimi.tiw.beans.TaskWithHours;
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
 * Data access object used for project reporting and validation operations.
 */
public class ProjectReportDAO {
    private final Connection connection;

    public ProjectReportDAO(Connection connection) {
        this.connection = connection;
    }

    public Map<WP, List<TaskWithHours>> getProjectReport(int projectId) throws SQLException {
        // linked hash map is used to preserve the sql order
        Map<WP, List<TaskWithHours>> report = new LinkedHashMap<>();
        String query = "SELECT * FROM project_wp_tasks_summary_view WHERE project_id = ? ORDER BY wp_sequence ASC, task_sequence ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, projectId);

            try (ResultSet result = stmt.executeQuery()) {
                WP currentWp = null;
                List<TaskWithHours> currentTaskList = null;

                while (result.next()) {
                    int wpId = result.getInt("wp_id");

                    // new wp instance is created when the id changes
                    if (currentWp == null || currentWp.getId() != wpId) {
                        currentWp = new WP(
                                wpId,
                                projectId,
                                result.getInt("wp_sequence"),
                                result.getString("wp_title"),
                                result.getInt("wp_start_month"),
                                result.getInt("wp_end_month")
                        );
                        currentTaskList = new ArrayList<>();
                        report.put(currentWp, currentTaskList);
                    }

                    // base task is instantiated with full descriptive fields from the view
                    Task task = new Task(
                            result.getInt("task_id"),
                            wpId,
                            result.getInt("task_sequence"),
                            result.getString("task_title"),
                            result.getString("task_description"),
                            result.getInt("start_month"),
                            result.getInt("end_month")
                    );

                    // aggregate columns are retrieved from the view
                    int plannedHours = result.getInt("total_expected_hours");
                    int workedHours = result.getInt("total_worked_hours");

                    // task transfer object is instantiated and added to the list
                    TaskWithHours taskWithHours = new TaskWithHours(task, plannedHours, workedHours);
                    if (currentTaskList != null) {
                        currentTaskList.add(taskWithHours);
                    }
                }
            }
        }
        return report;
    }
}
